import { BedrockAgentRuntimeClient, FunctionParameter, InlineSessionState, InvocationResultMember, InvokeInlineAgentCommand, ReturnControlPayload } from "@aws-sdk/client-bedrock-agent-runtime";
import { APIGatewayProxyHandlerV2 } from "aws-lambda";
import { v4 as uuid } from "uuid";

const agentClient = new BedrockAgentRuntimeClient();
const foundationModel = process.env.FOUNDATION_MODEL;

export const handler: APIGatewayProxyHandlerV2 = async (event, context) => {

    const payload = event.body as any;
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

    const _accountNumber = '1234567';
    const _sessionId = uuid();
    const _applicationTools = applicationTools(_accountNumber);

    do {
        const command = new InvokeInlineAgentCommand({
            foundationModel,
            sessionId: _sessionId,
            instruction: `
            You are the client's personal agent. You will assist them in managing their application. 

            Your client profile is as follows:
            - account number: ${_accountNumber}

            The application stores data in tables defined by the following DDL:
            -----
            CREATE TABLE account_transactions ( -- A table that stores the transaction history for all accounts
                transaction_number UUID NOT NULL, -- Transaction number (UUID), for system purposes
                account_number CHAR(7) NOT NULL, -- Target account number (7-digit numeric)
                recorded_date TIMESTAMP NOT NULL, -- Transaction record timestamp, in ISO 8601 format
                amount NUMERIC(13,0) NOT NULL, -- Transaction amount: deposits are positive, withdrawals are negative, currency is JPY
                balance NUMERIC(13,0) NOT NULL, -- Account balance after this transaction, currency is JPY
                memo TEXT, -- Optional memo for the transaction
                CHECK (account_number ~ '^[0-9]{7}$'), -- Only 7-digit numbers allowed for account number
                PRIMARY KEY (transaction_number)
            );
            -----

            The operation of the application follows the following rules:
            - Search results are limited to a maximum of 10 results.
            - Search results are listed in order of most recent data, unless otherwise specified.
            - System-purpose data is not included in responses unless explicitly requested.
            `,
            inputText: _inputText,
            streamingConfigurations: { streamFinalResponse: false },
            enableTrace: true,
            actionGroups: [{
                actionGroupName: 'RdbOperations',
                description: `This action group provides tools for RDB operations. `,
                actionGroupExecutor: { customControl: 'RETURN_CONTROL' },
                functionSchema: {
                    functions: [{
                        name: 'executeQuery',
                        description: `
                        Executes the specified SQL statement and returns the retrieved records.
                        The results are returned as an array with elements of type Record<string, any>.
                        If no corresponding records exist, an empty array is returned.
                        `,
                        parameters: {
                            statement: {
                                type: 'string', required: true,
                                description: `SQL statement to be executed, ANSI SQL (SQL-92) compliant, structured as simply as possible`
                            }
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
                    console.log(JSON.stringify(item.trace.trace, null, 4));
                }
                _returnControl = item.returnControl;
            }
        }
        if (_returnControl) {
            _inlineSessionState = await processResultControl(_returnControl, _applicationTools);
            _inputText = undefined;
        } else {
            _inlineSessionState = undefined;
        }
    } while (_returnControl)

    return {
        statusCode: 200,
        headers: {
            'Content-Type': 'text/plain'
        },
        body: _answer
    };
}

const processResultControl = async (ReturnControl: ReturnControlPayload,
    tools: { [key: string]: { [key: string]: (params: Record<string, any>) => any } }): Promise<InlineSessionState> => {

    const { invocationId, invocationInputs } = ReturnControl;
    const _results: InvocationResultMember[] = [];

    if (invocationInputs) {
        for (const input of invocationInputs) {
            if (input.functionInvocationInput) {
                const { actionGroup, function: funcName, parameters } = input.functionInvocationInput;
                let _result;
                try {
                    if (actionGroup && funcName) {
                        _result = tools[actionGroup][funcName](parametersToMap(parameters))
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

const applicationTools = (accountNumber: string) => {

    return {
        RdbOperations: {
            executeQuery: (args: Record<string, any>): Record<string, any>[] => {

                const _statement = args.statement;
                if (_statement === undefined) {
                    return [];
                }

                const recordSet = [{
                    transaction_number: '1234567-1234-5678-9012-345678901234',
                    account_number: '1234567',
                    recorded_date: '2024-11-22T13:24:56',
                    amount: 5000,
                    memo: '',
                }, {
                    transaction_number: '1234567-1234-5678-9012-345678901235',
                    account_number: '1234567',
                    recorded_date: '2024-12-20T12:29:45',
                    amount: -6000,
                    memo: '',
                }, {
                    transaction_number: '1234567-1234-5678-9012-345678901236',
                    account_number: '1234567',
                    recorded_date: '2025-01-18T10:27:42',
                    amount: 13000,
                    memo: '',
                }];
                return recordSet
            }
        }
    }
}