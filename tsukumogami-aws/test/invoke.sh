#!/bin/bash
echo '{
    "body": {
        "prompt": "Looking for all deposit transactions in my account."
    }
}' | sam local invoke MyAgent \
    --region ap-northeast-1 \
    -e - \
    -t ./cdk.out/Tsukumogami-HND-Stack.template.json