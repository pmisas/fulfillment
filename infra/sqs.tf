resource "aws_sqs_queue" "notification_events_dlq" {
  name = "NotificationEventsDLQ"

  lifecycle {
    ignore_changes = [max_message_size]
  }
}

resource "aws_sqs_queue" "notification_events_queue" {
  name = "NotificationEventsQueue"

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.notification_events_dlq.arn
    maxReceiveCount     = 5
  })

  lifecycle {
    ignore_changes = [max_message_size]
  }
}

resource "aws_sqs_queue" "order_events_dlq" {
  name = "OrderEventsDLQ"

  lifecycle {
    ignore_changes = [max_message_size]
  }
}

resource "aws_sqs_queue" "order_events_queue" {
  name = "OrderEventsQueue"

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.order_events_dlq.arn
    maxReceiveCount     = 5
  })

  lifecycle {
    ignore_changes = [max_message_size]
  }
}

data "aws_iam_policy_document" "order_events_queue_policy" {
  statement {
    sid    = "__owner_statement"
    effect = "Allow"

    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::029643846829:root"]
    }

    actions   = ["SQS:*"]
    resources = [aws_sqs_queue.order_events_queue.arn]
  }

  statement {
    sid    = "topic-subscription-arn:aws:sns:us-east-1:029643846829:FulfillmentDomainEventsTopic"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }

    actions   = ["sqs:SendMessage"]
    resources = [aws_sqs_queue.order_events_queue.arn]

    condition {
      test     = "ArnLike"
      variable = "aws:SourceArn"
      values   = [aws_sns_topic.domain_events.arn]
    }
  }
}

resource "aws_sqs_queue_policy" "order_events_queue" {
  queue_url = aws_sqs_queue.order_events_queue.id
  policy    = data.aws_iam_policy_document.order_events_queue_policy.json

  lifecycle {
    ignore_changes = [policy]
  }
}

data "aws_iam_policy_document" "notification_events_queue_policy" {
  statement {
    sid    = "__owner_statement"
    effect = "Allow"

    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::029643846829:root"]
    }

    actions   = ["SQS:*"]
    resources = [aws_sqs_queue.notification_events_queue.arn]
  }

  statement {
    sid    = "topic-subscription-arn:aws:sns:us-east-1:029643846829:FulfillmentDomainEventsTopic"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }

    actions   = ["sqs:SendMessage"]
    resources = [aws_sqs_queue.notification_events_queue.arn]

    condition {
      test     = "ArnLike"
      variable = "aws:SourceArn"
      values   = [aws_sns_topic.domain_events.arn]
    }
  }
}

resource "aws_sqs_queue_policy" "notification_events_queue" {
  queue_url = aws_sqs_queue.notification_events_queue.id
  policy    = data.aws_iam_policy_document.notification_events_queue_policy.json

  lifecycle {
    ignore_changes = [policy]
  }
}