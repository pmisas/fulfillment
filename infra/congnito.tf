resource "aws_cognito_user_pool" "main" {
  name                = "User pool - o8faaq"
  deletion_protection = "ACTIVE"

  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]

  username_configuration {
    case_sensitive = false
  }

  admin_create_user_config {
    allow_admin_create_user_only = false
  }

  password_policy {
    minimum_length                   = 8
    password_history_size            = 0
    require_lowercase                = true
    require_numbers                  = true
    require_symbols                  = true
    require_uppercase                = true
    temporary_password_validity_days = 7
  }

  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }

    recovery_mechanism {
      name     = "verified_phone_number"
      priority = 2
    }
  }

  email_configuration {
    email_sending_account = "COGNITO_DEFAULT"
  }

  sign_in_policy {
    allowed_first_auth_factors = ["PASSWORD"]
  }

  verification_message_template {
    default_email_option = "CONFIRM_WITH_CODE"
  }

  schema {
    attribute_data_type      = "String"
    developer_only_attribute = false
    mutable                  = true
    name                     = "email"
    required                 = true

    string_attribute_constraints {
      min_length = "0"
      max_length = "2048"
    }
  }
}

resource "aws_cognito_user_pool_client" "http_api_client" {
  name         = "fulfillment-http-api-client"
  user_pool_id = aws_cognito_user_pool.main.id

  explicit_auth_flows = [
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_AUTH",
    "ALLOW_USER_SRP_AUTH"
  ]

  access_token_validity  = 60
  id_token_validity      = 60
  refresh_token_validity = 5
  auth_session_validity  = 3

  token_validity_units {
    access_token  = "minutes"
    id_token      = "minutes"
    refresh_token = "days"
  }

  enable_token_revocation       = true
  prevent_user_existence_errors = "ENABLED"

  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["email", "openid", "profile"]
  supported_identity_providers         = ["COGNITO"]

  callback_urls = [
    "http://localhost:3000",
    "https://oauth.pstmn.io/v1/browser-callback",
    "https://oauth.pstmn.io/v1/callback",
    "https://oauth.pstmn.io/v1/vscode-callback"
  ]

  logout_urls      = []
  read_attributes  = []
  write_attributes = []
}

resource "aws_cognito_user_group" "admin" {
  user_pool_id = aws_cognito_user_pool.main.id
  name         = "ADMIN"
  description  = "Acceso total a la plataforma"
}

resource "aws_cognito_user_group" "operator" {
  user_pool_id = aws_cognito_user_pool.main.id
  name         = "OPERATOR"
  description  = "Puede crear y consultar órdenes, pero no modificar configuración ni inventario."
}

resource "aws_cognito_user_group" "warehouse_manager" {
  user_pool_id = aws_cognito_user_pool.main.id
  name         = "WAREHOUSE_MANAGER"
  description  = "Puede gestionar inventario y operaciones de warehouse"
}