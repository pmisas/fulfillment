data "aws_iam_policy_document" "lambda_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "ec2_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "events_publisher_lambda" {
  name               = "EventsPublisherRole"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume_role.json

  tags = local.common_tags
}

resource "aws_iam_role_policy_attachment" "events_publisher_basic_execution" {
  role       = aws_iam_role.events_publisher_lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

data "aws_iam_policy_document" "events_publisher_policy" {
  statement {
    sid = "OutboxTableAccess"
    actions = [
      "dynamodb:Query",
      "dynamodb:GetItem",
      "dynamodb:UpdateItem"
    ]
    resources = [
      aws_dynamodb_table.outbox_events.arn,
      "${aws_dynamodb_table.outbox_events.arn}/index/ByPublishStatus"
    ]
  }

  statement {
    sid       = "PublishDomainEvents"
    actions   = ["sns:Publish"]
    resources = [aws_sns_topic.domain_events.arn]
  }
}

resource "aws_iam_policy" "events_publisher" {
  name   = "EventsPublisherPolicy"
  policy = data.aws_iam_policy_document.events_publisher_policy.json

  tags = local.common_tags
}

resource "aws_iam_role_policy_attachment" "events_publisher" {
  role       = aws_iam_role.events_publisher_lambda.name
  policy_arn = aws_iam_policy.events_publisher.arn
}

resource "aws_iam_role" "notification_lambda" {
  name               = "NotificationLambdaRole"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume_role.json

  tags = local.common_tags
}

resource "aws_iam_role_policy_attachment" "notification_basic_execution" {
  role       = aws_iam_role.notification_lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

data "aws_iam_policy_document" "notification_lambda_policy" {
  statement {
    sid = "ReadOrder"
    actions = [
      "dynamodb:GetItem"
    ]
    resources = [aws_dynamodb_table.orders.arn]
  }

  statement {
    sid = "ConsumeNotificationEvents"
    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:GetQueueAttributes",
      "sqs:ChangeMessageVisibility"
    ]
    resources = [aws_sqs_queue.notification_events_queue.arn]
  }

  statement {
    sid       = "SendEmail"
    actions   = ["ses:SendEmail"]
    resources = ["*"]
  }

  statement {
    sid       = "LookupOperatorEmail"
    actions   = ["cognito-idp:AdminGetUser"]
    resources = [var.cognito_user_pool_arn]
  }
}

resource "aws_iam_policy" "notification_lambda" {
  name   = "NotificationLambdaPolicy"
  policy = data.aws_iam_policy_document.notification_lambda_policy.json

  tags = local.common_tags
}

resource "aws_iam_role_policy_attachment" "notification_lambda" {
  role       = aws_iam_role.notification_lambda.name
  policy_arn = aws_iam_policy.notification_lambda.arn
}

resource "aws_iam_role" "api_services_ec2" {
  name               = "FulfillmentApiServicesEc2Role"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume_role.json

  tags = local.common_tags
}

resource "aws_iam_instance_profile" "api_services" {
  name = "FulfillmentApiServicesInstanceProfile"
  role = aws_iam_role.api_services_ec2.name
}

data "aws_iam_policy_document" "api_services_ec2_policy" {
  statement {
    sid = "DynamoDbApplicationTables"
    actions = [
      "dynamodb:BatchGetItem",
      "dynamodb:BatchWriteItem",
      "dynamodb:ConditionCheckItem",
      "dynamodb:DeleteItem",
      "dynamodb:DescribeTable",
      "dynamodb:GetItem",
      "dynamodb:PutItem",
      "dynamodb:Query",
      "dynamodb:Scan",
      "dynamodb:TransactGetItems",
      "dynamodb:TransactWriteItems",
      "dynamodb:UpdateItem"
    ]
    resources = [
      aws_dynamodb_table.orders.arn,
      "${aws_dynamodb_table.orders.arn}/index/*",
      aws_dynamodb_table.order_state_history.arn,
      "${aws_dynamodb_table.order_state_history.arn}/index/*",
      aws_dynamodb_table.reservations.arn,
      "${aws_dynamodb_table.reservations.arn}/index/*",
      aws_dynamodb_table.shipments.arn,
      "${aws_dynamodb_table.shipments.arn}/index/*",
      aws_dynamodb_table.warehouses.arn,
      "${aws_dynamodb_table.warehouses.arn}/index/*",
      aws_dynamodb_table.inventory_items.arn,
      "${aws_dynamodb_table.inventory_items.arn}/index/*",
      aws_dynamodb_table.outbox_events.arn,
      "${aws_dynamodb_table.outbox_events.arn}/index/*",
      aws_dynamodb_table.warehouse_access.arn,
      "${aws_dynamodb_table.warehouse_access.arn}/index/*"
    ]
  }

  statement {
    sid = "ShippingGuidesBucket"
    actions = [
      "s3:GetObject",
      "s3:PutObject"
    ]
    resources = ["${aws_s3_bucket.shipping_guides.arn}/*"]
  }

  statement {
    sid = "WarehouseUserDirectory"
    actions = [
      "cognito-idp:AdminListGroupsForUser",
      "cognito-idp:ListUsers"
    ]
    resources = [var.cognito_user_pool_arn]
  }
}

resource "aws_iam_policy" "api_services_ec2" {
  name   = "FulfillmentApiServicesEc2Policy"
  policy = data.aws_iam_policy_document.api_services_ec2_policy.json

  tags = local.common_tags
}

resource "aws_iam_role_policy_attachment" "api_services_ec2" {
  role       = aws_iam_role.api_services_ec2.name
  policy_arn = aws_iam_policy.api_services_ec2.arn
}

resource "aws_iam_role" "worker_ec2" {
  name               = "FulfillmentWorkerEc2Role"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume_role.json

  tags = local.common_tags
}

resource "aws_iam_instance_profile" "worker" {
  name = "FulfillmentWorkerInstanceProfile"
  role = aws_iam_role.worker_ec2.name
}

data "aws_iam_policy_document" "worker_ec2_policy" {
  statement {
    sid = "ConsumeOrderEvents"
    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:GetQueueAttributes",
      "sqs:ChangeMessageVisibility"
    ]
    resources = [aws_sqs_queue.order_events_queue.arn]
  }

  statement {
    sid = "UpdateOrderState"
    actions = [
      "dynamodb:DescribeTable",
      "dynamodb:GetItem",
      "dynamodb:PutItem",
      "dynamodb:Query",
      "dynamodb:TransactWriteItems",
      "dynamodb:UpdateItem"
    ]
    resources = [
      aws_dynamodb_table.orders.arn,
      "${aws_dynamodb_table.orders.arn}/index/*",
      aws_dynamodb_table.order_state_history.arn,
      "${aws_dynamodb_table.order_state_history.arn}/index/*"
    ]
  }
}

resource "aws_iam_policy" "worker_ec2" {
  name   = "FulfillmentWorkerEc2Policy"
  policy = data.aws_iam_policy_document.worker_ec2_policy.json

  tags = local.common_tags
}

resource "aws_iam_role_policy_attachment" "worker_ec2" {
  role       = aws_iam_role.worker_ec2.name
  policy_arn = aws_iam_policy.worker_ec2.arn
}
