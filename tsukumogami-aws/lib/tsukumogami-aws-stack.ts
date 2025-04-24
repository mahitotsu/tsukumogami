import { Duration, RemovalPolicy, Stack, StackProps } from "aws-cdk-lib";
import { FoundationModel, FoundationModelIdentifier } from "aws-cdk-lib/aws-bedrock";
import { Runtime } from "aws-cdk-lib/aws-lambda";
import { NodejsFunction } from "aws-cdk-lib/aws-lambda-nodejs";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";
import { Construct } from "constructs";

export class TsukumogamiAwsStack extends Stack {
    constructor(scope: Construct, id: string, props: StackProps) {
        super(scope, id, props);

        const foundationModelId = FoundationModelIdentifier.ANTHROPIC_CLAUDE_3_5_SONNET_20241022_V2_0;
        const foundationModel = FoundationModel.fromFoundationModelId(this, 'FoundationModel', foundationModelId);

        const myAgent = new NodejsFunction(this, 'MyAgent', {
            runtime: Runtime.NODEJS_22_X,
            entry: `${__dirname}/functions/my-agent.ts`,
            environment: {
                FOUNDATION_MODEL: 'apac.' + foundationModel.modelId,
            },
            timeout: Duration.seconds(60),
            logGroup: new LogGroup(this, 'MyAgentLogGroup', {
                retention: RetentionDays.ONE_DAY,
                removalPolicy: RemovalPolicy.DESTROY,
            })
        });
    }
}
