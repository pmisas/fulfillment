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

resource "aws_dynamodb_table" "inventory_items" {
  name         = "InventoryItems"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "warehouseId"
  range_key    = "sku"

  attribute {
    name = "warehouseId"
    type = "S"
  }

  attribute {
    name = "sku"
    type = "S"
  }
}

resource "aws_dynamodb_table" "outbox_events" {
  name         = "OutboxEvents"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "eventId"

  attribute {
    name = "eventId"
    type = "S"
  }

  attribute {
    name = "publishStatus"
    type = "S"
  }

  attribute {
    name = "createdAt"
    type = "N"
  }

  global_secondary_index {
    name            = "ByPublishStatus"
    hash_key        = "publishStatus"
    range_key       = "createdAt"
    projection_type = "ALL"
  }

  ttl {
    attribute_name = "ttl"
    enabled        = true
  }
}

resource "aws_dynamodb_table" "warehouse_access" {
  name         = "WarehouseAccess"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "userId"

  attribute {
    name = "userId"
    type = "S"
  }

  attribute {
    name = "warehouseId"
    type = "S"
  }

  global_secondary_index {
    name            = "warehouseId-userId-index"
    hash_key        = "warehouseId"
    range_key       = "userId"
    projection_type = "ALL"
  }
}

