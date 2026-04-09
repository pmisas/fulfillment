resource "aws_sqs_queue" "notification_events_dlq" {
  name = "NotificationEventsDLQ"

  lifecycle {
    ignore_changes = [max_message_size]
  }
}

resource "aws_sqs_queue" "notification_events_queue" {
  name = "NotificationEventsQueue"

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

  lifecycle {
    ignore_changes = [max_message_size]
  }
}