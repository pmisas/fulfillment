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

output "domain_events_bus_name" {
  value = aws_cloudwatch_event_bus.domain_events.name
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

output "api_services_instance_profile_name" {
  value = aws_iam_instance_profile.api_services.name
}

output "worker_instance_profile_name" {
  value = aws_iam_instance_profile.worker.name
}

output "api_services_security_group_id" {
  value = try(aws_security_group.api_services[0].id, null)
}

output "worker_security_group_id" {
  value = try(aws_security_group.worker[0].id, null)
}

output "redis_security_group_id" {
  value = try(aws_security_group.redis[0].id, null)
}
