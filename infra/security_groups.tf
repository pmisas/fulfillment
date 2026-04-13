resource "aws_security_group" "api_services" {
  count = var.vpc_id == "" ? 0 : 1

  name        = "fulfillment-api-services-sg"
  description = "Ingress for public fulfillment Spring APIs on EC2"
  vpc_id      = var.vpc_id

  ingress {
    description = "order-service"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = var.public_http_cidr_blocks
  }

  ingress {
    description = "warehouse-service"
    from_port   = 8081
    to_port     = 8081
    protocol    = "tcp"
    cidr_blocks = var.public_http_cidr_blocks
  }

  ingress {
    description = "inventory-service"
    from_port   = 8082
    to_port     = 8082
    protocol    = "tcp"
    cidr_blocks = var.public_http_cidr_blocks
  }

  ingress {
    description = "shipping-service"
    from_port   = 8083
    to_port     = 8083
    protocol    = "tcp"
    cidr_blocks = var.public_http_cidr_blocks
  }

  dynamic "ingress" {
    for_each = length(var.ssh_cidr_blocks) == 0 ? [] : [1]

    content {
      description = "SSH admin access"
      from_port   = 22
      to_port     = 22
      protocol    = "tcp"
      cidr_blocks = var.ssh_cidr_blocks
    }
  }

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "fulfillment-api-services-sg"
  })
}

resource "aws_security_group" "worker" {
  count = var.vpc_id == "" ? 0 : 1

  name        = "fulfillment-worker-sg"
  description = "Outbound-only access for background workers on EC2"
  vpc_id      = var.vpc_id

  dynamic "ingress" {
    for_each = length(var.ssh_cidr_blocks) == 0 ? [] : [1]

    content {
      description = "SSH admin access"
      from_port   = 22
      to_port     = 22
      protocol    = "tcp"
      cidr_blocks = var.ssh_cidr_blocks
    }
  }

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "fulfillment-worker-sg"
  })
}

resource "aws_security_group" "redis" {
  count = var.vpc_id == "" ? 0 : 1

  name        = "fulfillment-redis-sg"
  description = "Redis access from fulfillment API services"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Redis from API services"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.api_services[0].id]
  }

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "fulfillment-redis-sg"
  })
}
