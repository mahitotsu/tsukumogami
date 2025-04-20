FROM public.ecr.aws/lambda/nodejs:22
COPY ./.output/server ${LAMBDA_TASK_ROOT}
CMD [ "index.handler" ]