resource "aws_s3_bucket" "shipping_guides" {
  bucket = var.shipping_guides_bucket_name

  tags = local.common_tags
}

resource "aws_s3_bucket_public_access_block" "shipping_guides" {
  bucket = aws_s3_bucket.shipping_guides.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "shipping_guides" {
  bucket = aws_s3_bucket.shipping_guides.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_versioning" "shipping_guides" {
  bucket = aws_s3_bucket.shipping_guides.id

  versioning_configuration {
    status = "Enabled"
  }
}
