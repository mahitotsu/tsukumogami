import { CfnOutput, Duration, RemovalPolicy, Stack, StackProps } from "aws-cdk-lib";
import { FoundationModel, FoundationModelIdentifier } from "aws-cdk-lib/aws-bedrock";
import { AllowedMethods, BehaviorOptions, CachePolicy, Distribution } from "aws-cdk-lib/aws-cloudfront";
import { FunctionUrlOrigin, S3BucketOrigin } from "aws-cdk-lib/aws-cloudfront-origins";
import { Effect, PolicyStatement } from "aws-cdk-lib/aws-iam";
import { FunctionUrlAuthType, InvokeMode } from "aws-cdk-lib/aws-lambda";
import { NodejsFunction } from "aws-cdk-lib/aws-lambda-nodejs";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";
import { Bucket } from "aws-cdk-lib/aws-s3";
import { BucketDeployment, Source } from "aws-cdk-lib/aws-s3-deployment";
import { Construct } from "constructs";

export class TsukumogamiAwsStack extends Stack {
    constructor(scope: Construct, id: string, props: StackProps) {
        super(scope, id, props);

        const foundationModelId = FoundationModelIdentifier.AMAZON_NOVA_MICRO_V1_0;
        const foundationModel = FoundationModel.fromFoundationModelId(this, 'FoundationModel', foundationModelId);

        // webapp static contents
        const contents = new Bucket(this, 'WebappContents', {
            removalPolicy: RemovalPolicy.DESTROY,
            autoDeleteObjects: true,
        });
        new BucketDeployment(this, 'WebappContentsDeployment', {
            destinationBucket: contents,
            destinationKeyPrefix: '',
            sources: [Source.asset(`${__dirname}/../../tsukumogami-web/.output/public`)],
            memoryLimit: 4096,
            retainOnDelete: false,
        });

        // webapp hanlder
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

        // webapp endponts
        const webappBehavior = {
            origin: FunctionUrlOrigin.withOriginAccessControl(webapp.addFunctionUrl({
                authType: FunctionUrlAuthType.AWS_IAM,
                invokeMode: InvokeMode.BUFFERED,
            })),
            allowedMethods: AllowedMethods.ALLOW_ALL,
            cachePolicy: CachePolicy.CACHING_DISABLED,
        } as BehaviorOptions;
        const s3BucketBehavior = {
            origin: S3BucketOrigin.withOriginAccessControl(contents, { originPath: '', },),
            allowedMethods: AllowedMethods.ALLOW_GET_HEAD,
            cachePolicy: CachePolicy.CACHING_DISABLED,
        } as BehaviorOptions;

        const distributinon = new Distribution(this, 'WebappDistribution', {
            defaultBehavior: webappBehavior,
            additionalBehaviors: {
                '/_nuxt/**': s3BucketBehavior,
                '/robot.txt': s3BucketBehavior,
                '/favicon.ico': s3BucketBehavior,
                
            }
        });

        // print access url
        new CfnOutput(this, 'ApiEndpoint', { value: `https://${distributinon.domainName}`, });
    }
}
