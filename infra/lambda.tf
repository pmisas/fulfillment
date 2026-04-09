resource "aws_lambda_function" "events_publisher" {
  function_name = "EventsPublisher"
  role          = "arn:aws:iam::029643846829:role/service-role/EventsPublisher-role-uobdqweh"
  handler       = "com.fulfillment.outboxpublisher.OutboxPublisherHandler::handleRequest"
  runtime       = "java17"

  s3_bucket = "fulfillment-lambda-artifacts"
  s3_key    = "lambdas/events-publisher/events-publisher-lambda.jar"

  memory_size = 512
  timeout     = 15

  architectures = ["x86_64"]

  ephemeral_storage {
    size = 512
  }

  environment {
    variables = {
      MAX_BATCH     = "25"
      OUTBOX_GSI    = "ByPublishStatus"
      OUTBOX_TABLE  = "OutboxEvents"
      SNS_TOPIC_ARN = "arn:aws:sns:us-east-1:029643846829:FulfillmentDomainEventsTopic"
    }
  }
}