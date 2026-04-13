variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "project_name" {
  type    = string
  default = "fulfillment"
}

variable "lambda_artifacts_bucket" {
  type    = string
  default = "fulfillment-lambda-artifacts"
}

variable "events_publisher_s3_key" {
  type    = string
  default = "lambdas/events-publisher/events-publisher-lambda.jar"
}

variable "notification_lambda_s3_key" {
  type    = string
  default = "lambdas/notification/notification-lambda.jar"
}

variable "shipping_guides_bucket_name" {
  type    = string
  default = "fulfillment-shipping-guides"
}

variable "outbox_publisher_schedule_expression" {
  type    = string
  default = "rate(1 minute)"
}

variable "cognito_user_pool_id" {
  type    = string
  default = ""
}

variable "cognito_user_pool_arn" {
  type    = string
  default = "*"
}

variable "ses_from_email" {
  type    = string
  default = "no-reply@example.com"
}

variable "vpc_id" {
  type    = string
  default = ""
}

variable "public_http_cidr_blocks" {
  type    = list(string)
  default = ["0.0.0.0/0"]
}

variable "ssh_cidr_blocks" {
  type    = list(string)
  default = []
}
