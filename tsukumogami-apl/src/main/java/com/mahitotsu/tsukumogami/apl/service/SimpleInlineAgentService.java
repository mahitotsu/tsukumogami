package com.mahitotsu.tsukumogami.apl.service;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.UUID;

import org.graalvm.polyglot.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.ActionGroupExecutor;
import software.amazon.awssdk.services.bedrockagentruntime.model.AgentActionGroup;
import software.amazon.awssdk.services.bedrockagentruntime.model.ContentBody;
import software.amazon.awssdk.services.bedrockagentruntime.model.CustomControlMethod;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionDefinition;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionInvocationInput;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionResult;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionSchema;
import software.amazon.awssdk.services.bedrockagentruntime.model.InlineAgentReturnControlPayload;
import software.amazon.awssdk.services.bedrockagentruntime.model.InlineSessionState;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvocationInputMember;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvocationResultMember;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeInlineAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeInlineAgentResponseHandler;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeInlineAgentResponseHandler.Visitor;
import software.amazon.awssdk.services.bedrockagentruntime.model.ParameterDetail;
import software.amazon.awssdk.services.bedrockagentruntime.model.ParameterType;

/**
 * Amazon BedrockのInlineAgentを用いてユーザーからの要求を処理して応答を返すサービス。
 */
@Service
public class SimpleInlineAgentService {

    @Autowired
    private BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * ユーザーからの入力をエージェントに引き渡し、最終的に返された応答を取得する。
     * 
     * @param prompt ユーザーからの入力
     * @return エージェントからの最終的な応答
     */
    public String execute(final String prompt) {

        final String sessionId = UUID.randomUUID().toString();
        final StringBuilder output = new StringBuilder();

        final Deque<InlineSessionState> sessionState = new LinkedList<>();
        final Deque<InlineAgentReturnControlPayload> returnControls = new LinkedList<>();
        final Collection<InvocationResultMember> results = new ArrayList<>();

        // エージェントからのツール実行要求が無くなるまでエージェントとのコミュニケーションを継続する
        // ツール実行要求が無くなった時点で最終的な応答が返されるので、その値を取得する
        do {
            final Collection<AgentActionGroup> actionGroups = new ArrayList<>();
            this.registerAgentActionGroups(actionGroups); // エージェントが利用可能なツール類を登録

            // 前回の実行要求をクリアしてからエージェントの実行要求を送信
            // 応答を受信する過程で、エージェントからのツール実行要求や最終的な応答の取得を行う
            returnControls.clear();
            this.bedrockAgentRuntimeAsyncClient.invokeInlineAgent(
                    this.buildInvokeInlineAgentRequest(sessionId, prompt,
                            sessionState.size() > 0 ? sessionState.pop() : null, // セッション属性がある場合のみセッションを指定
                            actionGroups),
                    this.buildInvokeInlineAgentResponseHandler(output, returnControls))
                    .join();

            // 前回の実行結果をクリアしてから受信したツール実行要求を処理
            results.clear();
            for (final InlineAgentReturnControlPayload payload : returnControls) {
                this.logger.info("process the payload: " + payload);
                // ツール実行要求を処理した結果を登録する
                results.addAll(this.buildInvocationResultMembers(payload));
            }

            // ツール実行結果が存在する場合、セッションの属性として登録する
            // セッションの属性は、次回のエージェント実行要求に含めてエージェントに送信される
            if (results.size() > 0) {
                sessionState.push(InlineSessionState.builder()
                        .invocationId(returnControls.peek().invocationId())
                        .returnControlInvocationResults(results)
                        .build());
                this.logger.info("the next session state: " + sessionState.peek());
            }

        } while (returnControls.size() > 0);

        // 最終的な応答を返却する
        this.logger.info("Result: " + output.toString());
        return output.toString();
    }

    /**
     * インラインエージェントの実行要求を構築して返す。
     * 
     * @param sessionId    一連のリクエストを識別するためのセッションID
     * @param inputText    エージェントに引き渡すユーザーからの入力値
     * @param sessionState 一連のリクエストで共有されるセッションの属性
     * @param actionGroups エージェントが利用可能なツールの定義
     * @return インラインエージェントの実行要求
     */
    private InvokeInlineAgentRequest buildInvokeInlineAgentRequest(final String sessionId, final String inputText,
            final InlineSessionState sessionState, final Collection<AgentActionGroup> actionGroups) {

        // 認証済みユーザーの権限に応じて利用する基盤モデルを切り替える
        final Collection<String> roles = new ArrayList<>();
        Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .ifPresent(a -> a.getAuthorities().stream().forEach(b -> roles.add(b.getAuthority().toUpperCase())));
        final String foundationModel = roles.contains("ROLE_PREMIUM") ? "apac.anthropic.claude-3-5-sonnet-20241022-v2:0"
                : "apac.amazon.nova-micro-v1:0";
        this.logger.info("Selected foundation model: " + foundationModel);

        // 実行要求を組み立てる
        return InvokeInlineAgentRequest.builder()
                .foundationModel(foundationModel)
                .instruction("""
                        あなたは以下の手順にしたがってユーザーの入力に対する応答を作成します。
                        * ユーザーが要求する内容を理解します。
                        * 要求された内容を達成するための作業項目を定義し、作業の実行計画を作成します。
                        * 作業項目ごとに必要となるツールを選定します。
                        * 選定したツールを利用しながら作業を実行し、最終的な応答を作成します。
                        """)
                .sessionId(sessionId)
                .inputText(sessionState == null ? inputText : null) // セッションに属性がある場合はプロンプトは無視されるので渡さない
                .inlineSessionState(sessionState)
                .actionGroups(actionGroups)
                .enableTrace(true)
                .build();
    }

    /**
     * エージェントの応答を処理するハンドラを構築して返す。
     * 
     * @param outputText     エージェントからの応答を格納するオブジェクト
     * @param returnControls エージェントからの実行要求を格納するオブジェクト
     * @return 応答を処理するハンドラ
     */
    private InvokeInlineAgentResponseHandler buildInvokeInlineAgentResponseHandler(final StringBuilder outputText,
            final SequencedCollection<InlineAgentReturnControlPayload> returnControls) {

        // onChunk -> 最終的な応答の断片を受け取ったら追記していく
        // onTrace -> Java SDKの場合、trace情報はマスクされてしまって何も情報が得られないので無視する
        // onReturnControl -> ツール実行要求を受け取った場合は処理対象として保持する
        return InvokeInlineAgentResponseHandler.builder()
                .onEventStream(publisher -> publisher.subscribe(event -> event.accept(Visitor.builder()
                        .onChunk(c -> outputText.append(c.bytes().asString(Charset.defaultCharset())))
                        // .onTrace(t -> this.logger.info(t.toString()))
                        .onReturnControl(rc -> returnControls.add(rc))
                        .build())))
                .build();
    }

    /**
     * 既定のアクショングループを登録する。
     * 
     * @param actionGroups アクショングループの登録先
     */
    private void registerAgentActionGroups(final Collection<AgentActionGroup> actionGroups) {

        // 認証済みではない場合、エージェントに対してツールを提供しない
        if (!Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication()).map(a -> a.isAuthenticated())
                .orElse(false)) {
            return;
        }

        // ActionGroup定義を作成する
        // ActionGroup -> FunctionSchema -> ParameterDetail の構造になっている
        // description属性をLLMが解釈して、エージェントがツール実行要求を構築するので明確な内容を記載しておく
        final AgentActionGroup jsexecutor = AgentActionGroup.builder()
                .actionGroupName("CodeInterpreterAction")
                .actionGroupExecutor(ActionGroupExecutor.fromCustomControl(CustomControlMethod.RETURN_CONTROL))
                .description("""
                        javascriptのコードを実行することが出来るツールです。
                        """)
                .functionSchema(FunctionSchema.builder().functions(
                        FunctionDefinition.builder()
                                .name("eval")
                                .description("""
                                        指定されたjavascriptのコードを実行して結果を返します。
                                        最後に評価された式の結果がコード全体の実行結果となります。
                                        返される結果は文字列型に変換されます。
                                        """)
                                .parameters(Map.of("code", ParameterDetail.builder()
                                        .description("""
                                                実行したいjavascriptのコードを指定します。
                                                 """)
                                        .type(ParameterType.STRING)
                                        .required(true)
                                        .build()))
                                .build())
                        .build())
                .build();

        // 作成したActionGroup定義を登録する
        actionGroups.add(jsexecutor);
    }

    /**
     * ツール実行要求に対して結果を構築し返す。
     * 
     * @param payload ツール実行要求
     * @return ツール実行結果
     */
    private Collection<InvocationResultMember> buildInvocationResultMembers(
            final InlineAgentReturnControlPayload payload) {

        final Collection<InvocationResultMember> results = new ArrayList<>();

        for (final InvocationInputMember member : payload.invocationInputs()) {
            final FunctionInvocationInput input = member.functionInvocationInput();
            String output;

            try {
                // ツール実行要求の中身を取得して提供しているツールに対する実行要求かどうか確認する
                final String actionGroupName = input.actionGroup();
                final String functionName = input.function();
                if (!"CodeInterpreterAction".equals(actionGroupName) || !"eval".equals(functionName)) {
                    throw new NoSuchElementException("The specified actionGroup and function is not supported.");
                }
                final String code = input.parameters().stream().filter(p -> "code".equals(p.name())).map(p -> p.value())
                        .findFirst().orElse(null);
                if (code == null) {
                    throw new IllegalArgumentException("The code parameter is required.");
                }
                // 想定された内容であれば該当する処理を実行して結果を返す
                output = this.eval(code);
            } catch (Exception e) {
                // エラーが発生した場合はエラーの内容を実行結果として返す
                output = e.getMessage();
                this.logger.error("An error occurred while running the tool.", e);
            }
            results.add(InvocationResultMember.builder().functionResult(
                    FunctionResult.builder()
                            .actionGroup(input.actionGroup())
                            .function(input.function())
                            .responseBody(Collections.singletonMap("TEXT",
                                    ContentBody.builder().body(output).build()))
                            .build())
                    .build());
        }

        // ツール実行要求に対する全ての実行結果を返す
        return results;
    }

    /**
     * JavaScriptコードを実行して結果を返す。エージェントに提供しているツールの実装。
     *
     * @param code JavaScriptコード
     * @return 実行結果の文字列
     */
    private String eval(final String code) {
        return Context.create().eval("js", code).asString();
    }
}
