package com.mahitotsu.tsukumogami.apl.service;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahitotsu.tsukumogami.apl.tools.ActionGroupProperties;
import com.mahitotsu.tsukumogami.apl.tools.provisioner.ActionGroupProvisionerBean;

import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.AgentActionGroup;
import software.amazon.awssdk.services.bedrockagentruntime.model.ContentBody;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionInvocationInput;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionParameter;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionResult;
import software.amazon.awssdk.services.bedrockagentruntime.model.InlineAgentReturnControlPayload;
import software.amazon.awssdk.services.bedrockagentruntime.model.InlineSessionState;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvocationInputMember;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvocationResultMember;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeInlineAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeInlineAgentResponseHandler;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeInlineAgentResponseHandler.Visitor;

@Service
public class Proteus {

    @Autowired
    private BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConversionService converter;

    @Autowired(required = false)
    private Collection<ActionGroupProperties> actionGroupBeans;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public String execute(final String workOrder) {

        final String sessionId = UUID.randomUUID().toString();
        final StringBuilder resultReport = new StringBuilder();

        final Deque<InlineSessionState> sessionState = new LinkedList<>();
        final Deque<InlineAgentReturnControlPayload> returnControls = new LinkedList<>();
        final Collection<InvocationResultMember> results = new ArrayList<>();

        final ActionGroupProvisionerBean provisioner = new ActionGroupProvisionerBean(this.actionGroupBeans);

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
                results.addAll(this.processResultControl(payload, provisioner));
            }
            if (results.size() > 0) {
                sessionState.push(InlineSessionState.builder()
                        .invocationId(returnControls.peek().invocationId())
                        .returnControlInvocationResults(results)
                        .build());
                this.logger.info("the next session state: " + sessionState.peek());
            }

        } while (returnControls.size() > 0);

        return resultReport.toString();
    }

    private InvokeInlineAgentRequest buildInvokeInlineAgentRequest(final String sessionId, final String inputText,
            final InlineSessionState sessionState, final Collection<AgentActionGroup> actionGroups) {

        return InvokeInlineAgentRequest.builder()
                .foundationModel("apac.anthropic.claude-3-5-sonnet-20241022-v2:0")
                .instruction("""
                        あなたは作業指示の内容を慎重に理解して完遂することが出来るエージェントです。
                        作業の遂行のために必要なツールはProvisionerを通じて検索して有効化することができます。

                        注意事項
                        * 日付を扱う場合、必ず計算の起点となる現在日時はツールを利用して最新の値を取得してください。
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

    private Collection<InvocationResultMember> processResultControl(final InlineAgentReturnControlPayload payload,
            final ActionGroupProvisionerBean provisoner) {

        final Collection<InvocationResultMember> results = new ArrayList<>();
        for (final InvocationInputMember i : payload.invocationInputs()) {
            FunctionResult result;
            try {
                result = this.processReturnCotrol(i.functionInvocationInput(), provisoner);
            } catch (Exception e) {
                result = this.reportException(i.functionInvocationInput(), e);
                e.printStackTrace();
            }
            results.add(InvocationResultMember.builder().functionResult(result).build());
        }

        return results;
    }

    private FunctionResult reportException(final FunctionInvocationInput input, final Exception e) {

        return FunctionResult.builder()
                .actionGroup(input.actionGroup())
                .function(input.function())
                .responseBody(Collections.singletonMap("TEXT",
                        ContentBody.builder().body(e.getMessage()).build()))
                .build();
    }

    private FunctionResult processReturnCotrol(final FunctionInvocationInput input,
            final ActionGroupProvisionerBean provisioner) throws Exception {

        final String actionGroup = input.actionGroup();
        final String function = input.function();
        final ActionGroupProperties tool = Optional.ofNullable(provisioner.getActionGroupBean(actionGroup))
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "The specified actionGroup is not found. actionGroup="
                                        + actionGroup));

        final int numOfArgs = input.parameters().size();
        final Method method = Arrays.stream(tool.getToolApiType().getMethods())
                .filter(m -> m.getName().equals(function) && m.getParameterCount() == numOfArgs)
                .findFirst().orElseThrow(() -> new NoSuchElementException(
                        "The spcified function is not found. actionGroup=" + actionGroup
                                + ", function=" + function));

        final Class<?>[] parameterTypes = method.getParameterTypes();
        final Object[] args = new Object[numOfArgs];
        final FunctionParameter[] params = new FunctionParameter[numOfArgs];
        input.parameters().stream().forEach(p -> params[tool.getParameterIndex(function, p.name())] = p);

        for (int i = 0; i < args.length; i++) {
            final FunctionParameter param = params[i];
            final String pv = param.value();
            Object value;
            switch (param.type()) {
                case "string":
                case "integer":
                case "boolean":
                    value = pv;
                    break;
                case "array":
                    value = Arrays.stream(pv.substring(1, pv.length() - 1).split(","))
                            .map(s -> s.trim())
                            .toArray(j -> new String[j]);
                    break;
                default:
                    throw new IllegalStateException("Unkonwn type is specified: " + param.type());
            }
            args[i] = this.converter.convert(value, parameterTypes[i]);
        }

        final Object result = method.invoke(tool, args);

        return FunctionResult.builder()
                .actionGroup(actionGroup)
                .function(function)
                .responseBody(Collections.singletonMap("TEXT",
                        ContentBody.builder().body(this.objectMapper.writeValueAsString(result))
                                .build()))
                .build();
    }
}
