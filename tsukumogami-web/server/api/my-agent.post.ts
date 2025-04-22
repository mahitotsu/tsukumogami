
import { BedrockAgentRuntimeClient, InvokeInlineAgentCommand } from '@aws-sdk/client-bedrock-agent-runtime';
import { initClientBundle } from '@nuxt/icon/runtime/components/shared.js';
import { isTemplateSpan } from 'typescript';
import { stringifyQuery } from 'vue-router';

const bedrockAgentClient = new BedrockAgentRuntimeClient();

export default defineEventHandler(async (event) => {

    const runtimeConfig = useRuntimeConfig();
    const body = await readBody(event);

    const foundationModel = runtimeConfig.foundationModel;
    const sessionId = Date.now().toString();

    let prompt = body.prompt || 'What should I talk about?';
    let inlineSessionState: any = undefined;
    let returnControls = [];
    let answer = '';
    let trace = '';

    do {
        returnControls = [];
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
            if (item.returnControl) {
                returnControls.push(item.returnControl);
            }
        }

        if (returnControls.length > 0) {
            inlineSessionState = await processReturnControls(returnControls);
            // prompt = '';
        } else {
            inlineSessionState = undefined;
        }
        console.log(JSON.stringify({
            returnControls, inlineSessionState,
        }))
    } while (returnControls.length > 0);

    setResponseStatus(event, 200);
    setHeader(event, 'content-type', 'application/json')
    return { answer, trace };
})

const processReturnControls = async (returnControls: any[]) => {
    if (returnControls.length === 0) return undefined;

    const results = [];
    for (const control of returnControls) {
        const { invocationId, invocationInputs } = control;
        for (const input of invocationInputs) {
            const { actionGroup, function: funcName, parameters } = input.functionInvocationInput;
            let result;
            try {
                result = (localTools as Record<string, any>)[actionGroup][funcName](parameters);
            } catch (e) {
                result = { error: (e as Error).message };
            }
            results.push({
                invocationId,
                functionResult: {
                    actionGroup,
                    function: funcName,
                    responseBody: { TEXT: { body: JSON.stringify(result) } },
                }
            });
        }
    }
    return { returnControlInvocationResults: results };
}

const localTools = {
    DatabaseAccessTools: {
        query: (params: any) => {
            return params;
        }
    }
}