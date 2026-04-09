resource "aws_dynamodb_table" "orders" {
  name         = "Orders"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "orderId"

  attribute {
    name = "orderId"
    type = "S"
  }
}

resource "aws_dynamodb_table" "order_state_history" {
  name         = "OrderStateHistory"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "historyId"

  attribute {
    name = "historyId"
    type = "S"
  }
}

resource "aws_dynamodb_table" "reservations" {
  name         = "Reservations"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "reservationId"

  attribute {
    name = "reservationId"
    type = "S"
  }
}

resource "aws_dynamodb_table" "shipments" {
  name         = "Shipments"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "shipmentId"

  attribute {
    name = "shipmentId"
    type = "S"
  }
}

resource "aws_dynamodb_table" "warehouses" {
  name         = "Warehouses"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "warehouseId"

  attribute {
    name = "warehouseId"
    type = "S"
  }
}


