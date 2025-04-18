import { CfnOutput, Duration, RemovalPolicy, Stack, StackProps } from "aws-cdk-lib";
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

        const webapp = new NodejsFunction(this, 'Webapp', {
            entry: `${__dirname}/../../tsukumogami-web/.output/server/index.mjs`,
            timeout: Duration.seconds(60),
            environment: {
                FOUNDATION_MODEL: foundationModel.modelId,
            },
            logGroup: new LogGroup(this, 'WebappLogGroup', {
                retention: RetentionDays.ONE_DAY,
                removalPolicy: RemovalPolicy.DESTROY,
            }),
        });
        webapp.addToRolePolicy(new PolicyStatement({
            effect: Effect.ALLOW,
            actions: ['bedrock:InvokeInlineAgent'],
            resources: ['*'],
        }));
        webapp.addToRolePolicy(new PolicyStatement({
            effect: Effect.ALLOW,
            actions: ['bedrock:InvokeModel*'],
            resources: [foundationModel.modelArn],
        }));

        const webappEndpoint = webapp.addFunctionUrl({
            authType: FunctionUrlAuthType.NONE,
            invokeMode: InvokeMode.BUFFERED,
        });

        new CfnOutput(this, 'ApiEndpoint', { value: webappEndpoint.url, });
    }
}
