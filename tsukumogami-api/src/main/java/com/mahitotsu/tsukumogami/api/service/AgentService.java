package com.mahitotsu.tsukumogami.api.service;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
@ConfigurationProperties(prefix = "tsukumogami.agentservice")
public class AgentService extends InlineAgentServiceProperties {

    @Autowired
    private BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient;

    @Autowired
    private BeanFactory beanFactory;

    public String getChatResponse(final String userInput) {

        final String inputText = userInput;
        final String sessionId = UUID.randomUUID().toString();

        final ValueHolder<InlineAgentReturnControlPayload> returnControlPayload = new ValueHolder<>(null);
        final ValueHolder<InlineSessionState> inlineSessionState = new ValueHolder<>(null);
        final StringBuilder answer = new StringBuilder();

        do {
            final InvokeInlineAgentRequest request = this.initInvokeInlineAgentRequestBuilder()
                    .sessionId(sessionId)
                    .inputText(inlineSessionState.get() == null ? inputText : null)
                    .inlineSessionState(inlineSessionState.get())
                    .build();

            returnControlPayload.set(null);
            final InvokeInlineAgentResponseHandler handler = this.initInvokeInlineAgentResponseHandler(answer,
                    returnControlPayload);

            this.bedrockAgentRuntimeAsyncClient.invokeInlineAgent(request, handler).join();

            if (returnControlPayload.get() != null) {
                final InlineAgentReturnControlPayload rc = returnControlPayload.get();
                final Collection<InvocationResultMember> results = this.processReturnControlPayload(rc);
                inlineSessionState.set(InlineSessionState.builder()
                        .invocationId(rc.invocationId())
                        .returnControlInvocationResults(results)
                        .build());
            }

        } while (returnControlPayload.get() != null);

        return answer.toString();
    }

    private InvokeInlineAgentResponseHandler initInvokeInlineAgentResponseHandler(
            final StringBuilder answer, final ValueHolder<InlineAgentReturnControlPayload> returnControlPayload) {

        return InvokeInlineAgentResponseHandler.builder()
                .onEventStream(publisher -> publisher.subscribe(event -> event.accept(Visitor.builder()
                        .onChunk(c -> answer.append(c.bytes().asString(Charset.defaultCharset())))
                        .onReturnControl(rc -> returnControlPayload.set(rc))
                        .build())))
                .build();
    }

    private Collection<InvocationResultMember> processReturnControlPayload(final InlineAgentReturnControlPayload rc) {

        final Collection<InvocationResultMember> results = new ArrayList<>();

        for (final InvocationInputMember i : rc.invocationInputs()) {
            final FunctionInvocationInput input = i.functionInvocationInput();
            final String actionGroup = input.actionGroup();
            final String function = input.function();
            final List<FunctionParameter> parameters = input.parameters();
            String body;
            try {
                body = BeanFactoryAnnotationUtils.qualifiedBeanOfType(this.beanFactory,
                        ActionGroupFunction.class, actionGroup + "." + function)
                        .invoke(parameters);
            } catch (final Exception e) {
                body = e.toString();
            }
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

        return results;
    }
}
