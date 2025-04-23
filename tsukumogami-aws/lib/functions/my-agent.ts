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
    const _accountNumber = '1234567';
    const _applicationTools = applicationTools(_accountNumber);

    do {
        const command = new InvokeInlineAgentCommand({
            foundationModel,
            sessionId: _sessionId,
            instruction: 'You are a dedicated agent assigned to each customer.',
            inputText: _inputText,
            streamingConfigurations: { streamFinalResponse: false },
            enableTrace: true,
            actionGroups: [{
                actionGroupName: 'TxActions',
                description: `
                This action group provides tools for transactions. Transactions are stored in the table created with the following DDL:
                -----
                CREATE TABLE account_transactions (
                    account_number CHAR(7) NOT NULL, -- Target account number (7-digit numeric)
                    recorded_date TIMESTAMP NOT NULL, -- Transaction record timestamp
                    amount NUMERIC(13,0) NOT NULL, -- Transaction amount: deposits are positive, withdrawals are negative
                    balance NUMERIC(13,0) NOT NULL, -- Account balance after this transaction
                    memo TEXT, -- Optional memo for the transaction
                    CHECK (account_number ~ '^[0-9]{7}$'), -- Only 7-digit numbers allowed for account number
                    CHECK (amount > 0 AND amount < 1000000000000), -- Amount must be a positive integer less than 1 trillion
                    PRIMARY KEY (account_number, recorded_date) -- Composite primary key: account number and record timestamp
                );
                -----
                The following rules are followed when selecting records from the table.
                - The search result records must include the account_number column.
                - The search criteria must include the condition "account_number column is equal to ${_accountNumber}."
                `,
                actionGroupExecutor: { customControl: 'RETURN_CONTROL' },
                functionSchema: {
                    functions: [{
                        name: 'listTransactions',
                        description: `
                        Executes a SQL statement to extract records from a specified table and returns the retrieved records.
                        The result is returned as an array whose elements are Record<string, any> type. 
                        If no corresponding record exists, an empty array is returned.
                        `,
                        parameters: {
                            statement: {
                                type: 'string', required: true,
                                description: 'SQL statement to be executed',
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
                    _trace += JSON.stringify(item.trace.trace);
                    _trace += '\n';
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
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ ansser: _answer, trace: _trace })
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
        TxTools: {
            listTransactions: (args: Record<string, any>): Record<string, any>[] => {
                const recordSet = [{
                    account_number: '1234567',
                    recorded_date: '2024-11-22T13:24:56',
                    amount: 5000,
                    memo: '',
                }, {
                    account_number: '1234567',
                    recorded_date: '2024-12-20T12:29:45',
                    amount: -6000,
                    memo: '',
                }, {
                    account_number: '1234567',
                    recorded_date: '2025-01-18T10:27:42',
                    amount: 13000,
                    memo: '',
                }];
                return recordSet.filter(record => accountNumber == record.account_number);
            }
        }
    }
}