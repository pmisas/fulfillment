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

resource "aws_lambda_function" "notification_lambda" {
  function_name = "notificationLambda"
  role          = "arn:aws:iam::029643846829:role/lambdaFunction-role-cpl9krj0"
  handler       = "com.fulfillment.notificationlambda.NotificationHandler::handleRequest"
  runtime       = "java17"

  s3_bucket = "fulfillment-lambda-artifacts"
  s3_key    = "lambdas/notification-lambda/notification-lambda.jar"

  memory_size = 512
  timeout     = 15

  architectures = ["x86_64"]

  ephemeral_storage {
    size = 512
  }

  environment {
    variables = {
      COGNITO_USER_POOL_ID = "us-east-1_uNLRWeBsi"
      SES_FROM_EMAIL       = "fulfillmentAWS@gmail.com"
    }
  }
}
