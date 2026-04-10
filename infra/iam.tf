resource "aws_iam_role" "events_publisher_role" {
  name = "EventsPublisher-role-uobdqweh"
  path = "/service-role/"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "events_publisher_custom_policy" {
  role       = aws_iam_role.events_publisher_role.name
  policy_arn = "arn:aws:iam::029643846829:policy/EventsPublisherLambdaPolicy"
}

resource "aws_iam_role_policy_attachment" "events_publisher_basic_execution" {
  role       = aws_iam_role.events_publisher_role.name
  policy_arn = "arn:aws:iam::029643846829:policy/service-role/AWSLambdaBasicExecutionRole-7b4286f2-91ac-4d64-8f92-7c595dfc0b75"
}

resource "aws_iam_role_policy" "events_publisher_publish_topic" {
  name = "AllowPublishToFulfillmentDomainEventsTopic"
  role = aws_iam_role.events_publisher_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "AllowPublishToFulfillmentDomainEventsTopic"
        Effect   = "Allow"
        Action   = "sns:Publish"
        Resource = "arn:aws:sns:us-east-1:029643846829:FulfillmentDomainEventsTopic"
      }
    ]
  })
}