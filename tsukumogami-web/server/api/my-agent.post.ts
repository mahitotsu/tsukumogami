
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
        enableTrace: true,
        actionGroups: [{
            actionGroupName: 'UserInput',
            parentActionGroupSignature: 'AMAZON.UserInput'
        }, {
            actionGroupName: 'CodeInterpreter',
            parentActionGroupSignature: 'AMAZON.CodeInterpreter'
        },],
    });
    const result = await bedrockAgentClient.send(command);

    let answer = '';
    let trace = '';
    for await (const item of result.completion!) {
        if (item.chunk?.bytes) {
            answer += Buffer.from(item.chunk.bytes).toString('utf-8');
        }
        if (item.trace?.trace) {
            trace += JSON.stringify(item.trace.trace);
            trace += '\n\n';
        }
    }

    setResponseStatus(event, 200);
    setHeader(event, 'content-type', 'application/json')
    return { answer, trace };
})