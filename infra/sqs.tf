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
    sid     = "AllowDomainEventsTopicToSend"
    effect  = "Allow"
    actions = ["sqs:SendMessage"]

    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }

    resources = [aws_sqs_queue.order_events_queue.arn]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_sns_topic.domain_events.arn]
    }
  }

  statement {
    sid     = "AllowEventBridgeToSend"
    effect  = "Allow"
    actions = ["sqs:SendMessage"]

    principals {
      type        = "Service"
      identifiers = ["events.amazonaws.com"]
    }

    resources = [aws_sqs_queue.order_events_queue.arn]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_cloudwatch_event_rule.order_events_to_sqs.arn]
    }
  }
}

resource "aws_sqs_queue_policy" "order_events_queue" {
  queue_url = aws_sqs_queue.order_events_queue.id
  policy    = data.aws_iam_policy_document.order_events_queue_policy.json
}

data "aws_iam_policy_document" "notification_events_queue_policy" {
  statement {
    sid     = "AllowDomainEventsTopicToSend"
    effect  = "Allow"
    actions = ["sqs:SendMessage"]

    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }

    resources = [aws_sqs_queue.notification_events_queue.arn]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_sns_topic.domain_events.arn]
    }
  }

  statement {
    sid     = "AllowEventBridgeToSend"
    effect  = "Allow"
    actions = ["sqs:SendMessage"]

    principals {
      type        = "Service"
      identifiers = ["events.amazonaws.com"]
    }

    resources = [aws_sqs_queue.notification_events_queue.arn]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_cloudwatch_event_rule.notification_events_to_sqs.arn]
    }
  }
}

resource "aws_sqs_queue_policy" "notification_events_queue" {
  queue_url = aws_sqs_queue.notification_events_queue.id
  policy    = data.aws_iam_policy_document.notification_events_queue_policy.json
}
