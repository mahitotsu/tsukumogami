
import { BedrockAgentRuntimeClient, InvokeInlineAgentCommand } from '@aws-sdk/client-bedrock-agent-runtime';

const bedrockAgentClient = new BedrockAgentRuntimeClient();

export default defineEventHandler(async (event) => {

    const runtimeConfig = useRuntimeConfig();
    const body = await readBody(event);

    const foundationModel = runtimeConfig.foundationModel;
    const prompt = body.prompt || 'What should I talk about?';
    const sessionId = Date.now().toString();

    const command = new InvokeInlineAgentCommand({
        sessionId, foundationModel,
        instruction: 'You are a dedicated agent assigned to each customer.',
        inputText: prompt,
        streamingConfigurations: { streamFinalResponse: false },
    });
    const result = await bedrockAgentClient.send(command);

    const response = event.node.res;
    response.statusCode = 200;
    response.setHeader('Content-Type', 'text/plain');
    for await (const item of result.completion!) {
        if (item.chunk?.bytes) {
            response.write(Buffer.from(item.chunk.bytes).toString('utf-8'));
        }
    }
    response.end();
})