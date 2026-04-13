resource "aws_scheduler_schedule" "events_publisher" {
  name                         = "EventsScheduler"
  group_name                   = "default"
  schedule_expression          = "cron(*/59 * * * ? *)"
  schedule_expression_timezone = "America/Bogota"
  state                        = "ENABLED"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = aws_lambda_function.events_publisher.arn
    role_arn = var.scheduler_role_arn

    retry_policy {
      maximum_retry_attempts = 0
    }
  }
}