# notification-lambda

AWS Lambda that sends SES email notifications to operators when key order lifecycle events occur.

## Overview

This Lambda is triggered by an SQS queue (`NotificationQueue`) that subscribes to the central `OrderEventsTopic` SNS topic. For each event it looks up the order in DynamoDB, resolves the operator's email from Cognito, and sends a formatted HTML email via SES.

```
events-publisher-lambda
        │
        ▼
 OrderEventsTopic (SNS)
   ├──► OrderEventsQueue  ──► order-state-processor
   └──► NotificationQueue ──► notification-lambda ──► SES email → operator
```

## Handled Events

| Event type            | Email content                                                  |
|-----------------------|----------------------------------------------------------------|
| `OrderReceived`       | New order confirmation with item list                          |
| `ShipmentShipped`     | Shipment dispatched; includes carrier and estimated delivery   |
| `ShipmentDelivered`   | Delivery confirmation                                          |

`OrderCancellationRequested` is not handled yet.

## Architecture

Hexagonal (ports & adapters), no Spring Boot:

```
application/
  handler/
    EventNotificationHandler        ← interface
    OrderReceivedNotificationHandler
    ShipmentShippedNotificationHandler
    ShipmentDeliveredNotificationHandler
  NotificationDispatcher            ← routes eventType → handler
domain/
  model/   OrderInfo, EmailNotification
  ports/   EmailSender, OrderLookup, OperatorEmailLookup
infrastructure/
  config/  EnvConfig                ← reads env vars
  email/   SesEmailSender           ← SES v2
  order/   DynamoOrderLookup        ← DynamoDB GetItem
  operator/ CognitoOperatorEmailLookup ← adminGetUser
  messaging/dto/  payload DTOs
NotificationHandler                 ← Lambda entrypoint (RequestHandler<SQSEvent, SQSBatchResponse>)
```

Partial batch failure is supported: if a single message fails, only that `messageId` is returned as a batch item failure so SQS retries only the failing message.

## Required Environment Variables

| Variable              | Description                                          |
|-----------------------|------------------------------------------------------|
| `AWS_REGION`          | AWS region (e.g. `us-east-1`)                        |
| `ORDERS_TABLE`        | DynamoDB table name for orders                       |
| `COGNITO_USER_POOL_ID`| Cognito User Pool ID where operators are stored      |
| `SES_FROM_EMAIL`      | Verified sender address in SES                       |

## Required AWS Permissions

- `ses:SendEmail` on `*`
- `dynamodb:GetItem` on the orders table
- `cognito-idp:AdminGetUser` on the Cognito User Pool
- `sqs:ReceiveMessage`, `sqs:DeleteMessage`, `sqs:GetQueueAttributes` on `NotificationQueue`

## Infrastructure Setup

1. Create SNS topic `OrderEventsTopic`.
2. Subscribe existing `OrderEventsQueue` (order-state-processor) to the topic.
3. Create SQS queue `NotificationQueue` and subscribe it to the topic.
   - Optionally add a filter policy to limit messages to handled event types.
4. Update `events-publisher-lambda` env var: `SQS_QUEUE_URL` → `SNS_TOPIC_ARN`.
5. Add `NotificationQueue` as event source mapping for this Lambda.
6. Set the four env vars listed above on the Lambda function.
7. Verify `SES_FROM_EMAIL` in SES (or move the SES account out of sandbox).

## Building

```bash
./mvnw.cmd package -DskipTests          # produces target/notification-lambda-*.jar (fat JAR)
./mvnw.cmd test                          # runs unit tests (15 tests, no AWS calls)
```

## Running Tests

Tests are fully unit-tested with Mockito — no real AWS calls:

```
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
```
