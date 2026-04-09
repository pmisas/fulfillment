output "notification_events_queue_url" {
  value = aws_sqs_queue.notification_events_queue.url
}

output "order_events_queue_url" {
  value = aws_sqs_queue.order_events_queue.url
}