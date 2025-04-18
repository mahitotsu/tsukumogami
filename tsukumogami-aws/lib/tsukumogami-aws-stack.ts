import { CfnOutput, Duration, RemovalPolicy, ScopedAws, Stack, StackProps } from "aws-cdk-lib";
import { FoundationModel, FoundationModelIdentifier } from "aws-cdk-lib/aws-bedrock";
import { Effect, PolicyStatement } from "aws-cdk-lib/aws-iam";
import { FunctionUrlAuthType, InvokeMode } from "aws-cdk-lib/aws-lambda";
import { NodejsFunction } from "aws-cdk-lib/aws-lambda-nodejs";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";
import { Construct } from "constructs";

export class TsukumogamiAwsStack extends Stack {
    constructor(scope: Construct, id: string, props: StackProps) {
        super(scope, id, props);

        // const { region, accountId } = new ScopedAws(this);
        const foundationModelId = FoundationModelIdentifier.AMAZON_NOVA_MICRO_V1_0;
        const foundationModel = FoundationModel.fromFoundationModelId(this, 'FoundationModel', foundationModelId);

        const aiQueryHandler = new NodejsFunction(this, 'AiQueryHandler', {
            entry: `${__dirname}/functions/aiQueryHandler.ts`,
            timeout: Duration.seconds(60),
            environment: {
                FOUNDATION_MODEL: foundationModel.modelId,
            },
            logGroup: new LogGroup(this, 'AiQueryHandlerLogGroup', {
                retention: RetentionDays.ONE_DAY,
                removalPolicy: RemovalPolicy.DESTROY,
            }),
        });
        aiQueryHandler.addToRolePolicy(new PolicyStatement({
            effect: Effect.ALLOW,
            actions: ['bedrock:InvokeInlineAgent'],
            resources: ['*'],
        }));
        aiQueryHandler.addToRolePolicy(new PolicyStatement({
            effect: Effect.ALLOW,
            actions: ['bedrock:InvokeModel*'],
            resources: [foundationModel.modelArn],
        }));

        const aiQueryEndpoint = aiQueryHandler.addFunctionUrl({
            authType: FunctionUrlAuthType.NONE,
            invokeMode: InvokeMode.RESPONSE_STREAM,
        });

        new CfnOutput(this, 'ApiEndpoint', { value: aiQueryEndpoint.url, });
    }
}
