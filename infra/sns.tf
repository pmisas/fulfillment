resource "aws_sns_topic" "domain_events" {
  name = "FulfillmentDomainEventsTopic"

  tags = local.common_tags
}

resource "aws_sns_topic_subscription" "order_events_queue" {
  topic_arn            = aws_sns_topic.domain_events.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.order_events_queue.arn
  raw_message_delivery = false

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
      "ShipmentShipped",
      "ShipmentDelivered"
    ]
  })

  depends_on = [aws_sqs_queue_policy.notification_events_queue]
}
