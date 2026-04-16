resource "aws_instance" "shipping_service" {
  ami                    = "ami-02dfbd4ff395f2a1b"
  instance_type          = "t3.micro"
  subnet_id              = "subnet-03cb1155be977076a"
  vpc_security_group_ids = ["sg-07cecdd919462fdc3"]
  iam_instance_profile   = "FulfillmentEc2Role"

  tags = {
    Name = "shipping-service"
  }
}

resource "aws_instance" "warehouse_inventory" {
  ami                    = "ami-02dfbd4ff395f2a1b"
  instance_type          = "t3.micro"
  subnet_id              = "subnet-04833e273ac825742"
  vpc_security_group_ids = ["sg-03f846aa2ceec9ffe"]
  iam_instance_profile   = "FulfillmentEc2Role"

  tags = {
    Name = "warehouseInventory-ec2"
  }
}


resource "aws_instance" "order_state" {
  ami                    = "ami-0f3caa1cf4417e51b"
  instance_type          = "t3.micro"
  subnet_id              = "subnet-0755cc52680ce2bc2"
  vpc_security_group_ids = ["sg-025ae6cbe22d229e1"]
  iam_instance_profile   = "FulfillmentEc2Role"

  credit_specification {
    cpu_credits = "unlimited"
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_protocol_ipv6          = "disabled"
    http_put_response_hop_limit = 2
    http_tokens                 = "required"
    instance_metadata_tags      = "disabled"
  }

  private_dns_name_options {
    enable_resource_name_dns_a_record    = true
    enable_resource_name_dns_aaaa_record = false
    hostname_type                        = "ip-name"
  }

  root_block_device {
    delete_on_termination = true
    encrypted             = false
    iops                  = 3000
    throughput            = 125
    volume_size           = 8
    volume_type           = "gp3"
  }

  tags = {
    Name = "order-state"
  }
}


resource "aws_instance" "order_service" {
  ami                    = "ami-0f3caa1cf4417e51b"
  instance_type          = "t3.micro"
  subnet_id              = "subnet-0755cc52680ce2bc2"
  vpc_security_group_ids = ["sg-025ae6cbe22d229e1"]
  iam_instance_profile   = "FulfillmentEc2Role"

  credit_specification {
    cpu_credits = "unlimited"
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_protocol_ipv6          = "disabled"
    http_put_response_hop_limit = 2
    http_tokens                 = "required"
    instance_metadata_tags      = "disabled"
  }

  private_dns_name_options {
    enable_resource_name_dns_a_record    = true
    enable_resource_name_dns_aaaa_record = false
    hostname_type                        = "ip-name"
  }

  root_block_device {
    delete_on_termination = true
    encrypted             = false
    iops                  = 3000
    throughput            = 125
    volume_size           = 8
    volume_type           = "gp3"
  }

  tags = {
    Name = "order-service"
  }
}


resource "aws_instance" "warehouse_inventory" {
  ami                    = "ami-02dfbd4ff395f2a1b"
  instance_type          = "t3.micro"
  subnet_id              = "subnet-04833e273ac825742"
  vpc_security_group_ids = ["sg-03f846aa2ceec9ffe"]
  iam_instance_profile   = "FulfillmentEc2Role"

  credit_specification {
    cpu_credits = "unlimited"
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_protocol_ipv6          = "disabled"
    http_put_response_hop_limit = 2
    http_tokens                 = "required"
    instance_metadata_tags      = "disabled"
  }

  private_dns_name_options {
    enable_resource_name_dns_a_record    = true
    enable_resource_name_dns_aaaa_record = false
    hostname_type                        = "ip-name"
  }

  root_block_device {
    delete_on_termination = true
    encrypted             = false
    iops                  = 3000
    throughput            = 125
    volume_size           = 8
    volume_type           = "gp3"
  }

  tags = {
    Name = "warehouseInventory-ec2"
  }
}
