#!/usr/bin/env node

import { App } from "aws-cdk-lib";
import { TsukumogamiAwsStack } from "../lib/tsukumogami-aws-stack";

const app = new App();
const account = process.env.CDK_DEFAULT_ACCOUNT;

new TsukumogamiAwsStack(app, 'Tsukumogami-IAD-Stack', {
  env: { account, region: 'us-east-1' },
});