output "notification_events_queue_url" {
  value = aws_sqs_queue.notification_events_queue.url
}

output "notification_events_queue_arn" {
  value = aws_sqs_queue.notification_events_queue.arn
}

output "order_events_queue_url" {
  value = aws_sqs_queue.order_events_queue.url
}

output "order_events_queue_arn" {
  value = aws_sqs_queue.order_events_queue.arn
}

output "domain_events_topic_arn" {
  value = aws_sns_topic.domain_events.arn
}

output "events_publisher_lambda_name" {
  value = aws_lambda_function.events_publisher.function_name
}

output "notification_lambda_name" {
  value = aws_lambda_function.notification.function_name
}

output "shipping_guides_bucket_name" {
  value = aws_s3_bucket.shipping_guides.bucket
}

output "order_state_worker_security_group_id" {
  value = try(aws_security_group.order_state_worker[0].id, null)
}

output "inventory_warehouse_security_group_id" {
  value = try(aws_security_group.inventory_warehouse[0].id, null)
}

output "shipping_security_group_id" {
  value = try(aws_security_group.shipping[0].id, null)
}