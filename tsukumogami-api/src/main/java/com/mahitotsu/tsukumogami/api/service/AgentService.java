package com.mahitotsu.tsukumogami.api.service;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
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
public class AgentService {

    @Autowired
    private BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient;

    @Autowired
    private BeanFactory beanFactory;

    @Value("${tsukumogami.agentservice.foundationmodel}")
    private String foundationModel;

    @Value("${tsukumogami.agentservice.instruction}")
    private String instruction;

    public String getChatResponse(final String userInput) {

        final ValueHolder<String> inputText = new ValueHolder<>(userInput);
        final ValueHolder<String> sessionId = new ValueHolder<>(UUID.randomUUID().toString());
        final ValueHolder<InlineAgentReturnControlPayload> returnControlPayload = new ValueHolder<>(null);
        final ValueHolder<InlineSessionState> inlineSessionState = new ValueHolder<>(null);
        final StringBuilder answer = new StringBuilder();

        do {
            final InvokeInlineAgentRequest request = InvokeInlineAgentRequest.builder()
                    .foundationModel(this.foundationModel)
                    .instruction(this.instruction)
                    .sessionId(sessionId.get())
                    .inputText(inlineSessionState.get() == null ? inputText.get() : null)
                    .inlineSessionState(inlineSessionState.get())
                    .build();

            this.bedrockAgentRuntimeAsyncClient.invokeInlineAgent(request,
                    InvokeInlineAgentResponseHandler.builder()
                            .onResponse(response -> sessionId.set(response.sessionId()))
                            .onEventStream(publisher -> publisher.subscribe(event -> event.accept(Visitor.builder()
                                    .onChunk(c -> answer.append(c.bytes().asString(Charset.defaultCharset())))
                                    .onReturnControl(rc -> returnControlPayload.set(rc))
                                    .build())))
                            .build())
                    .join();

            if (returnControlPayload.get() != null) {
                final InlineAgentReturnControlPayload rc = returnControlPayload.get();
                final Collection<InvocationResultMember> results = new ArrayList<>();
                for (final InvocationInputMember i : rc.invocationInputs()) {
                    final FunctionInvocationInput input = i.functionInvocationInput();
                    final String actionGroup = input.actionGroup();
                    final String function = input.function();
                    final List<FunctionParameter> parameters = input.parameters();
                    final String body = this.beanFactory
                            .getBean(actionGroup + "." + function, ActionGroupFunction.class).apply(parameters);
                    final InvocationResultMember result = InvocationResultMember.builder()
                            .functionResult(FunctionResult.builder()
                                    .actionGroup(actionGroup)
                                    .function(function)
                                    .responseBody(Collections.singletonMap("TEXT",
                                            ContentBody.builder().body(body).build()))
                                    .build())
                            .build();
                    results.add(result);
                }
                inlineSessionState.set(InlineSessionState.builder()
                        .invocationId(rc.invocationId())
                        .returnControlInvocationResults(results)
                        .build());
            }

        } while (returnControlPayload.get() != null);

        return answer.toString();
    }
}
