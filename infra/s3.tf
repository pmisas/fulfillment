resource "aws_s3_bucket" "shipping_guides" {
  bucket        = var.shipping_guides_bucket_name
  force_destroy = false

  lifecycle {
    ignore_changes = [tags, tags_all]
  }
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
    bucket_key_enabled = true

    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}