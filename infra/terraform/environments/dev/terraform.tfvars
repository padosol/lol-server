# Dev Environment - terraform.tfvars

aws_region         = "ap-northeast-2"
project_name       = "lol"
environment        = "dev"
vpc_cidr           = "10.0.0.0/16"
enable_nat_gateway = true

# ACM Certificate ARN for HTTPS (leave empty for HTTP only)
certificate_arn = ""
