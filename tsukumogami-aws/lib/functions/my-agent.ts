import { BedrockAgentRuntimeClient, FunctionParameter, InlineSessionState, InvocationResultMember, InvokeInlineAgentCommand, ReturnControlPayload } from "@aws-sdk/client-bedrock-agent-runtime";
import { APIGatewayProxyHandlerV2 } from "aws-lambda";

const agentClient = new BedrockAgentRuntimeClient();
const foundationModel = process.env.FOUNDATION_MODEL;

export const handler: APIGatewayProxyHandlerV2 = async (event, context) => {

    const payload = event.body ? JSON.parse(event.body) : undefined;
    if (!(payload && payload.prompt)) {
        return {
            statusCode: 400,
            body: JSON.stringify({ message: 'The prompt must not be empty.' })
        };
    }

    let _inputText = payload.prompt;
    let _inlineSessionState: InlineSessionState | undefined;
    let _returnControl: ReturnControlPayload | undefined;
    let _answer = '';
    let _trace = '';
    const _sessionId = Date.now().toString();

    do {
        const command = new InvokeInlineAgentCommand({
            foundationModel,
            sessionId: _sessionId,
            instruction: 'You are a dedicated agent assigned to each customer.',
            inputText: _inputText,
            streamingConfigurations: { streamFinalResponse: false },
            enableTrace: true,
            actionGroups: [{
                actionGroupName: 'ApplicationTools',
                description: `A set of tools for using the various functions provided by the application.`,
                actionGroupExecutor: { customControl: 'RETURN_CONTROL' },
                functionSchema: {
                    functions: [{
                        name: 'listTransactions',
                        description: `
                        Returns the transaction history for the specified account within the specified time period. 
                        Results are ordered by the date the transaction occurred, in ascending order.
                        `,
                        parameters: {
                            accountNumber: {
                                type: 'string', required: true,
                                description: `
                                The account number to search for.
                                `
                            },
                            startDate: {
                                type: 'string', required: true,
                                description: `
                                The start date of the period. 
                                It will be included in the search results.
                                `
                            },
                            endDate: {
                                type: 'string', required: true,
                                description: `
                                The end date of the target period. 
                                It will not be included in the search results.
                                `
                            },
                        }
                    }]
                }
            }],
            inlineSessionState: _inlineSessionState,
        });

        const result = await agentClient.send(command);
        if (result.completion) {
            for await (const item of result.completion) {
                if (item.chunk?.bytes) {
                    _answer += Buffer.from(item.chunk.bytes).toString('utf-8');
                }
                if (item.trace?.trace) {
                    _trace += JSON.stringify(item.trace.trace);
                    _trace += '\n';
                }
                _returnControl = item.returnControl;
            }
        }
        if (_returnControl) {
            _inlineSessionState = await processResultControl(_returnControl);
            _inputText = undefined;
        } else {
            _inlineSessionState = undefined;
        }
    } while (_returnControl)

    return {
        statusCode: 200,
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ ansser: _answer, trace: _trace })
    };
}

const processResultControl = async (ReturnControl: ReturnControlPayload): Promise<InlineSessionState> => {

    const { invocationId, invocationInputs } = ReturnControl;
    const _results: InvocationResultMember[] = [];

    if (invocationInputs) {
        for (const input of invocationInputs) {
            if (input.functionInvocationInput) {
                const { actionGroup, function: funcName, parameters } = input.functionInvocationInput;
                let _result;
                try {
                    if (actionGroup && funcName) {
                        _result = (localTools as Record<string, any>)[actionGroup][funcName](parametersToMap(parameters))
                    } else {
                        _result = { error: 'The actionGroup and function must be not null.' };
                    }
                } catch (e) {
                    _result = { error: (e as Error).message };
                }
                _results.push({
                    functionResult: {
                        actionGroup, function: funcName,
                        responseBody: { TEXT: { body: JSON.stringify(_result) } }
                    }
                });
            }
        }
    }

    return {
        invocationId,
        returnControlInvocationResults: _results,
    };
}

const parametersToMap = (parameters: FunctionParameter[] | undefined) => {

    const map: Record<string, any> = {};

    if (parameters) {
        for (const p of parameters) {
            map[p.name!] = p.value;
        }
    }
    return map;
}

const localTools = {
    ApplicationTools: {
    }
}