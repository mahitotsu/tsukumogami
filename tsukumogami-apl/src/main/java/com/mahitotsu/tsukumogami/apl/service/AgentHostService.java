package com.mahitotsu.tsukumogami.apl.service;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.SequencedCollection;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mahitotsu.tsukumogami.apl.tools.provisioner.ActionGroupProvisionerBean;

import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.AgentActionGroup;
import software.amazon.awssdk.services.bedrockagentruntime.model.InlineAgentReturnControlPayload;
import software.amazon.awssdk.services.bedrockagentruntime.model.InlineSessionState;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvocationResultMember;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeInlineAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeInlineAgentResponseHandler;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeInlineAgentResponseHandler.Visitor;

@Service
public class AgentHostService {

    @Autowired
    private BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient;

    @Autowired
    private BeanFactory beanFactory;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public String execute(final String workOrder) {

        final String sessionId = UUID.randomUUID().toString();
        final StringBuilder resultReport = new StringBuilder();

        final Deque<InlineSessionState> sessionState = new LinkedList<>();
        final Deque<InlineAgentReturnControlPayload> returnControls = new LinkedList<>();
        final Collection<InvocationResultMember> results = new ArrayList<>();

        final ActionGroupProvisionerBean provisioner = this.beanFactory.getBean(ActionGroupProvisionerBean.class);

        do {
            final Collection<AgentActionGroup> actionGroups = new ArrayList<>();
            actionGroups.addAll(provisioner.getActiveActionGroups());

            returnControls.clear();
            this.bedrockAgentRuntimeAsyncClient.invokeInlineAgent(
                    this.buildInvokeInlineAgentRequest(sessionId, workOrder,
                            sessionState.size() > 0 ? sessionState.pop() : null,
                            actionGroups),
                    this.buildInvokeInlineAgentResponseHandler(resultReport, returnControls))
                    .join();

            results.clear();
            for (final InlineAgentReturnControlPayload payload : returnControls) {
                this.logger.info("process the payload: " + payload);
                results.addAll(provisioner.buildInvocationResultMembers(payload));
            }
            if (results.size() > 0) {
                sessionState.push(InlineSessionState.builder()
                        .invocationId(returnControls.peek().invocationId())
                        .returnControlInvocationResults(results)
                        .build());
                this.logger.info("the next session state: " + sessionState.peek());
            }

        } while (returnControls.size() > 0);

        this.logger.info("Result: " + resultReport.toString());
        return resultReport.toString();
    }

    private InvokeInlineAgentRequest buildInvokeInlineAgentRequest(final String sessionId, final String inputText,
            final InlineSessionState sessionState, final Collection<AgentActionGroup> actionGroups) {

        return InvokeInlineAgentRequest.builder()
                .foundationModel("apac.anthropic.claude-3-5-sonnet-20241022-v2:0")
                .instruction("""
                        あなたは以下の手順にしたがってユーザーの入力に対する応答を作成します。
                        * ユーザーが要求する内容を理解します。
                        * 要求された内容を達成するための作業項目を定義し、作業の実行計画を作成します。
                        * 作業項目ごとに必要となるツールを選定します。
                        * 選定したツールを利用しながら作業を実行し、最終的な応答を作成します。

                        注意事項
                        * 作業指示の完遂のために必要なツールはProvisionerを通じて発見し、有効化して、利用してください。
                        * 今日、明日、昨日、など現在日時を起点にした日付はツールを利用して取得してください。
                        """)
                .sessionId(sessionId)
                .inputText(sessionState == null ? inputText : null)
                .inlineSessionState(sessionState)
                .actionGroups(actionGroups)
                .enableTrace(true)
                .build();
    }

    private InvokeInlineAgentResponseHandler buildInvokeInlineAgentResponseHandler(final StringBuilder outputText,
            final SequencedCollection<InlineAgentReturnControlPayload> returnControls) {

        return InvokeInlineAgentResponseHandler.builder()
                .onEventStream(publisher -> publisher.subscribe(event -> event.accept(Visitor.builder()
                        .onChunk(c -> outputText
                                .append(c.bytes().asString(Charset.defaultCharset())))
                        // .onTrace(t -> this.logger.info(t.toString()))
                        .onReturnControl(rc -> returnControls.add(rc))
                        .build())))
                .build();
    }
}