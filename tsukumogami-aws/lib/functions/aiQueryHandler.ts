import { BedrockAgentRuntimeClient, InvokeInlineAgentCommand } from "@aws-sdk/client-bedrock-agent-runtime";
import { APIGatewayProxyEventV2 } from "aws-lambda";
import { v4 as uuid } from "uuid";

const agentClient = new BedrockAgentRuntimeClient();
const foundationModel = process.env.FOUNDATION_MODEL;

export const handler = awslambda.streamifyResponse(
    async (event: APIGatewayProxyEventV2, responseStream, context) => {

        responseStream.setContentType('text/plain');

        await invokeAgent(
            event.body || 'No input text is specified.',
            uuid(),
            responseStream);
        responseStream.write('\n');

        responseStream.end();
    }
);

const invokeAgent = async (inputText: string, sessionId: string, responseStream: awslambda.HttpResponseStream) => {

    const instruction = `
        Respond to user questions and requests accurately and clearly, 
        using the Code Interpreter and Knowledge Base as necessary, 
        while clearly indicating reasons and procedures.
    `;

    const command = new InvokeInlineAgentCommand({
        sessionId,
        foundationModel,
        instruction,
        inputText,
        streamingConfigurations: { streamFinalResponse: true },
    });

    const response = await agentClient.send(command);
    for await (const e of response.completion || []) {
        e.chunk?.bytes && responseStream.write(new TextDecoder().decode(e.chunk.bytes));
    }
}