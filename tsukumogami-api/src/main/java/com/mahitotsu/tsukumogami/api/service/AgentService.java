package com.mahitotsu.tsukumogami.api.service;

import java.nio.charset.Charset;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeInlineAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeInlineAgentResponseHandler;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeInlineAgentResponseHandler.Visitor;

@Service
public class AgentService {

    @Autowired
    private BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient;

    public String getChatResponse(final String userInput) {

        final String initSessionId = UUID.randomUUID().toString();
        final String[] sessionId = new String[] { initSessionId };
        final StringBuilder answer = new StringBuilder();

        final InvokeInlineAgentRequest request = this.createBedrockAgentRuntimeRequest(sessionId[0], userInput);
        this.bedrockAgentRuntimeAsyncClient.invokeInlineAgent(request,
                InvokeInlineAgentResponseHandler.builder()
                        .onResponse(response -> {
                            sessionId[0] = response.sessionId();
                        })
                        .onEventStream(publisher -> publisher.subscribe(event -> event.accept(Visitor.builder()
                                .onChunk(c -> {
                                    answer.append(c.bytes().asString(Charset.defaultCharset()));
                                })
                                .build())))
                        .build())
                .join();

        return answer.toString();
    }

    private InvokeInlineAgentRequest createBedrockAgentRuntimeRequest(final String sessionId, final String userInput) {

        return InvokeInlineAgentRequest.builder()
                .foundationModel("apac.amazon.nova-micro-v1:0")
                .instruction("""
                        You are a friendly and knowledgeable AI assistant,
                        providing concise and easy-to-understand responses to all user questions.
                         """)
                .sessionId(sessionId)
                .inputText(userInput)
                .build();
    }
}
