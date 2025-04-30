package com.mahitotsu.tsukumogami.apl.service;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.ContentBody;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionInvocationInput;
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
    @Qualifier("ActionGroup")
    private Map<String, Object> actionGroups;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public String execute(final String workOrder) {

        final String sessionId = UUID.randomUUID().toString();
        final StringBuilder resultReport = new StringBuilder();
        final Deque<InlineSessionState> sessionState = new LinkedList<>();

        do {
            final Deque<InlineAgentReturnControlPayload> returnControls = new LinkedList<>();
            this.bedrockAgentRuntimeAsyncClient
                    .invokeInlineAgent(
                            this.buildInvokeInlineAgentRequest(sessionId, workOrder,
                                    sessionState.size() > 0 ? sessionState.pop() : null),
                            this.buildInvokeInlineAgentResponseHandler(resultReport, returnControls))
                    .join();

            if (returnControls.isEmpty()) {
                break;
            }

            final Collection<InvocationResultMember> results = new ArrayList<>();
            for (final InlineAgentReturnControlPayload payload : returnControls) {
                results.addAll(this.processResultControl(payload));
            }
            sessionState.push(InlineSessionState.builder()
                    .invocationId(returnControls.peek().invocationId())
                    .returnControlInvocationResults(results)
                    .build());

        } while (sessionState.size() == 1 && sessionState.peek().hasReturnControlInvocationResults());

        return resultReport.toString();
    }

    private InvokeInlineAgentRequest buildInvokeInlineAgentRequest(final String sessionId, final String inputText,
            final InlineSessionState sessionState) {

        return InvokeInlineAgentRequest.builder()
                .foundationModel("apac.anthropic.claude-3-5-sonnet-20241022-v2:0")
                .instruction("""
                        あなたは作業指示の内容を慎重に理解して完遂することが出来るエージェントです。
                        作業結果は省略することなく、順番を入れ替えることなく、詳細を具体的に報告します。
                        """)
                .sessionId(sessionId)
                .inputText(sessionState != null && sessionState.hasReturnControlInvocationResults() ? null : inputText)
                .enableTrace(true)
                .build();
    }

    private InvokeInlineAgentResponseHandler buildInvokeInlineAgentResponseHandler(final StringBuilder outputText,
            final SequencedCollection<InlineAgentReturnControlPayload> returnControls) {

        return InvokeInlineAgentResponseHandler.builder()
                .onEventStream(publisher -> publisher.subscribe(event -> event.accept(Visitor.builder()
                        .onChunk(c -> outputText.append(c.bytes().asString(Charset.defaultCharset())))
                        .onTrace(t -> this.logger.info(t.toString()))
                        .onReturnControl(rc -> returnControls.add(rc))
                        .build())))
                .build();
    }

    private Collection<InvocationResultMember> processResultControl(final InlineAgentReturnControlPayload payload) {

        final Collection<InvocationResultMember> results = new ArrayList<>();
        for (final InvocationInputMember i : payload.invocationInputs()) {
            FunctionResult result;
            try {
                result = this.processReturnCotrol(i.functionInvocationInput());
            } catch (Exception e) {
                result = this.reportException(i.functionInvocationInput(), e);
            }
            InvocationResultMember.builder().functionResult(result).build();
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

    private FunctionResult processReturnCotrol(final FunctionInvocationInput input) throws Exception {

        final String actionGroup = input.actionGroup();
        final String function = input.function();
        final Object tool = Optional.ofNullable(this.actionGroups.get(actionGroup)).orElseThrow(
                () -> new NoSuchElementException("The specified actionGroup is not found. actionGroup=" + actionGroup));

        final int numOfArgs = input.parameters().size();
        final Method method = Arrays.stream(tool.getClass().getMethods())
                .filter(m -> m.getName().equals(function) && m.getParameterCount() == numOfArgs)
                .findFirst().orElseThrow(() -> new NoSuchElementException(
                        "The spcified function is not found. actionGroup=" + actionGroup + ", function=" + function));

        final Class<?>[] parameterTypes = method.getParameterTypes();
        final Object[] args = new Object[numOfArgs];
        for (int i = 0; i < args.length; i++) {
            args[i] = this.converter.convert(input.parameters().get(i).value(), parameterTypes[i]);
        }

        final Object result = method.invoke(tool, args);

        return FunctionResult.builder()
                .actionGroup(actionGroup)
                .function(function)
                .responseBody(Collections.singletonMap("TEXT",
                        ContentBody.builder().body(this.objectMapper.writeValueAsString(result)).build()))
                .build();
    }
}
