resource "aws_sns_topic" "domain_events" {
  name = "FulfillmentDomainEventsTopic"

  lifecycle {
    ignore_changes = [tags, tags_all]
  }
}

resource "aws_sns_topic_subscription" "order_events_queue" {
  topic_arn            = aws_sns_topic.domain_events.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.order_events_queue.arn
  raw_message_delivery = false

  filter_policy = jsonencode({
    eventType = [
      "OrderReceived",
      "OrderCancelled",
      "PickingCompleted",
      "PackingCompleted",
      "ShipmentShipped"
    ]
  })

  lifecycle {
    ignore_changes = [
      confirmation_timeout_in_minutes,
      endpoint_auto_confirms
    ]
  }

  depends_on = [aws_sqs_queue_policy.order_events_queue]
}

resource "aws_sns_topic_subscription" "notification_events_queue" {
  topic_arn            = aws_sns_topic.domain_events.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.notification_events_queue.arn
  raw_message_delivery = false

  filter_policy = jsonencode({
    eventType = [
      "OrderReceived",
      "ShipmentDelivered",
      "ShipmentShipped"
    ]
  })

  lifecycle {
    ignore_changes = [
      confirmation_timeout_in_minutes,
      endpoint_auto_confirms
    ]
  }

  depends_on = [aws_sqs_queue_policy.notification_events_queue]
}