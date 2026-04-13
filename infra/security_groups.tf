resource "aws_security_group" "order_state_worker" {
  count = var.vpc_id == "" ? 0 : 1

  name        = "order-state-worker-sg"
  description = "permite acceso a instancia worker"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = ""
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = ""
  }

  tags = {
    Name = "order-state-worker-sg"
  }

  lifecycle {
    ignore_changes = [tags, tags_all]
  }
}

resource "aws_security_group" "inventory_warehouse" {
  count = var.vpc_id == "" ? 0 : 1

  name        = "inventory-warehouse.sg"
  description = "inventory and warehouse service sg"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 8081
    to_port     = 8081
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = ""
  }

  ingress {
    from_port   = 8082
    to_port     = 8082
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = ""
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = ""
  }

  tags = {
    Name = "inventory-warehouse.sg"
  }

  lifecycle {
    ignore_changes = [tags, tags_all]
  }
}

resource "aws_security_group" "shipping" {
  count = var.vpc_id == "" ? 0 : 1

  name        = "shipping-sg"
  description = "Security group for Shipping Service EC2 instances"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 8083
    to_port     = 8083
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = ""
  }

  ingress {
    from_port       = 8083
    to_port         = 8083
    protocol        = "tcp"
    security_groups = [aws_security_group.order_state_worker[0].id]
    description     = ""
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = ""
  }

  tags = {
    Name = "shipping-sg"
  }

  lifecycle {
    ignore_changes = [tags, tags_all, ingress]
  }
}