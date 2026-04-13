resource "aws_cloudwatch_event_bus" "domain_events" {
  name = "FulfillmentDomainEventsBus"

  tags = local.common_tags
}

resource "aws_cloudwatch_event_rule" "outbox_publisher_schedule" {
  name                = "EventsPublisherSchedule"
  description         = "Runs the outbox publisher Lambda to publish pending domain events."
  schedule_expression = var.outbox_publisher_schedule_expression

  tags = local.common_tags
}

resource "aws_cloudwatch_event_target" "outbox_publisher_lambda" {
  rule      = aws_cloudwatch_event_rule.outbox_publisher_schedule.name
  target_id = "EventsPublisher"
  arn       = aws_lambda_function.events_publisher.arn
}

resource "aws_lambda_permission" "allow_eventbridge_to_invoke_events_publisher" {
  statement_id  = "AllowEventBridgeInvokeEventsPublisher"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.events_publisher.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.outbox_publisher_schedule.arn
}

resource "aws_cloudwatch_event_rule" "order_events_to_sqs" {
  name           = "OrderEventsToSqs"
  description    = "Routes order lifecycle events from the custom EventBridge bus to the order processor queue."
  event_bus_name = aws_cloudwatch_event_bus.domain_events.name

  event_pattern = jsonencode({
    source = ["fulfillment.domain"]
    "detail-type" = [
      "OrderReceived",
      "OrderCancellationRequested",
      "PickingCompleted",
      "PackingCompleted",
      "ShipmentShipped",
      "ShipmentDelivered"
    ]
  })

  tags = local.common_tags
}

resource "aws_cloudwatch_event_target" "order_events_queue" {
  rule           = aws_cloudwatch_event_rule.order_events_to_sqs.name
  event_bus_name = aws_cloudwatch_event_bus.domain_events.name
  target_id      = "OrderEventsQueue"
  arn            = aws_sqs_queue.order_events_queue.arn

  input_transformer {
    input_paths = {
      event_id   = "$.id"
      event_type = "$.detail-type"
      payload    = "$.detail"
    }

    input_template = "{\"eventId\":\"<event_id>\",\"eventType\":\"<event_type>\",\"payload\":<payload>}"
  }
}

resource "aws_cloudwatch_event_rule" "notification_events_to_sqs" {
  name           = "NotificationEventsToSqs"
  description    = "Routes notification-worthy domain events from the custom EventBridge bus to the notification queue."
  event_bus_name = aws_cloudwatch_event_bus.domain_events.name

  event_pattern = jsonencode({
    source = ["fulfillment.domain"]
    "detail-type" = [
      "OrderReceived",
      "ShipmentShipped",
      "ShipmentDelivered"
    ]
  })

  tags = local.common_tags
}

resource "aws_cloudwatch_event_target" "notification_events_queue" {
  rule           = aws_cloudwatch_event_rule.notification_events_to_sqs.name
  event_bus_name = aws_cloudwatch_event_bus.domain_events.name
  target_id      = "NotificationEventsQueue"
  arn            = aws_sqs_queue.notification_events_queue.arn

  input_transformer {
    input_paths = {
      event_id   = "$.id"
      event_type = "$.detail-type"
      payload    = "$.detail"
    }

    input_template = "{\"eventId\":\"<event_id>\",\"eventType\":\"<event_type>\",\"payload\":<payload>}"
  }
}
