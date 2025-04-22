
import { BedrockAgentRuntimeClient, InvokeInlineAgentCommand } from '@aws-sdk/client-bedrock-agent-runtime';

const bedrockAgentClient = new BedrockAgentRuntimeClient();

export default defineEventHandler(async (event) => {

    const runtimeConfig = useRuntimeConfig();
    const body = await readBody(event);

    const foundationModel = runtimeConfig.foundationModel;
    const sessionId = Date.now().toString();

    let prompt = body.prompt || 'What can I say?';
    let inlineSessionState: any = undefined;
    let returnControl = undefined;
    let answer = '';
    let trace = '';

    do {
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
            }, {
                actionGroupName: 'DatabaseAccessTools',
                actionGroupExecutor: { customControl: 'RETURN_CONTROL' },
                functionSchema: {
                    functions: [{
                        name: 'query',
                        description: 'Executes a search query and returns results.',
                        parameters: {
                            statement: {
                                type: 'string',
                                description: 'SQL statement to execute',
                                required: true,
                            }
                        }
                    }]
                }
            }],
            inlineSessionState,
        });
        const result = await bedrockAgentClient.send(command);

        for await (const item of result.completion!) {
            if (item.chunk?.bytes) {
                answer += Buffer.from(item.chunk.bytes).toString('utf-8');
            }
            if (item.trace?.trace) {
                trace += JSON.stringify(item.trace.trace);
                trace += '\n\n';
            }
            returnControl = item.returnControl;
        }

        if (returnControl) {
            inlineSessionState = await processReturnControls(returnControl);
            prompt = 'See the inlineSessionState.';
        } else {
            inlineSessionState = undefined;
        }
    } while (returnControl);

    setResponseStatus(event, 200);
    setHeader(event, 'content-type', 'application/json')
    return { answer, trace };
})

const processReturnControls = async (returnControl: any) => {

    const { invocationId, invocationInputs } = returnControl;

    const results = [];
    for (const input of invocationInputs) {
        const { actionGroup, function: funcName, parameters } = input.functionInvocationInput;
        let result;
        try {
            result = (localTools as Record<string, any>)[actionGroup][funcName](parameters);
        } catch (e) {
            result = { error: (e as Error).message };
        }
        results.push({
            functionResult: {
                actionGroup,
                function: funcName,
                responseBody: { TEXT: { body: JSON.stringify(result) } },
            }
        });
    }
    return {
        invocationId,
        returnControlInvocationResults: results
    };
}

const localTools = {
    DatabaseAccessTools: {
        query: (params: any) => {
            return [{ idx: 1, val: 'v1' }, { idx: 2, val: 'v2' }, { idx: 3, val: 'v3' }];
        }
    }
}