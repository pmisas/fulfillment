resource "aws_lambda_function" "events_publisher" {
  function_name = "EventsPublisher"
  role          = aws_iam_role.events_publisher_lambda.arn
  handler       = "com.fulfillment.outboxpublisher.OutboxPublisherHandler::handleRequest"
  runtime       = "java17"

  s3_bucket = var.lambda_artifacts_bucket
  s3_key    = var.events_publisher_s3_key

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
      SNS_TOPIC_ARN = aws_sns_topic.domain_events.arn
    }
  }

  tags = local.common_tags

  lifecycle {
    ignore_changes = [
      tags,
      tags_all,
      publish,
      s3_bucket,
      s3_key
    ]
  }
}

resource "aws_lambda_function" "notification" {
  function_name = "notificationLambda"
  role          = aws_iam_role.notification_lambda.arn
  handler       = "com.fulfillment.notificationlambda.NotificationHandler::handleRequest"
  runtime       = "java17"

  s3_bucket = var.lambda_artifacts_bucket
  s3_key    = var.notification_lambda_s3_key

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

  tags = local.common_tags

  lifecycle {
    ignore_changes = [
      tags,
      tags_all,
      publish,
      s3_bucket,
      s3_key
    ]
  }
}

resource "aws_lambda_event_source_mapping" "notification_events_queue" {
  event_source_arn = aws_sqs_queue.notification_events_queue.arn
  function_name    = aws_lambda_function.notification.arn
  batch_size       = 5

  metrics_config {
    metrics = ["EventCount"]
  }
}