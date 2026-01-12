# Dev Environment - main.tf

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
  }

  # Backend configuration - uncomment and configure for remote state
  # backend "s3" {
  #   bucket         = "your-terraform-state-bucket"
  #   key            = "lol/dev/terraform.tfstate"
  #   region         = "ap-northeast-2"
  #   encrypt        = true
  #   dynamodb_table = "terraform-lock"
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# VPC
module "vpc" {
  source = "../../modules/vpc"

  project_name       = var.project_name
  environment        = var.environment
  vpc_cidr           = var.vpc_cidr
  enable_nat_gateway = var.enable_nat_gateway
}

# ECR Repositories
module "ecr" {
  source = "../../modules/ecr"

  project_name     = var.project_name
  environment      = var.environment
  repository_names = ["service", "repository", "ui"]
}

# ECS Cluster
module "ecs_cluster" {
  source = "../../modules/ecs-cluster"

  project_name                = var.project_name
  environment                 = var.environment
  vpc_id                      = module.vpc.vpc_id
  service_discovery_namespace = "${var.project_name}.local"
  enable_container_insights   = false
  log_retention_days          = 7
}

# ALB
module "alb" {
  source = "../../modules/alb"

  project_name               = var.project_name
  environment                = var.environment
  vpc_id                     = module.vpc.vpc_id
  public_subnet_ids          = module.vpc.public_subnet_ids
  certificate_arn            = var.certificate_arn
  enable_deletion_protection = false
  service_health_check_path  = "/actuator/health"
}

# RDS
module "rds" {
  source = "../../modules/rds"

  project_name               = var.project_name
  environment                = var.environment
  vpc_id                     = module.vpc.vpc_id
  db_subnet_group_name       = module.vpc.db_subnet_group_name
  allowed_security_group_ids = [module.ecs_service_repository.security_group_id]
  instance_class             = "db.t3.micro"
  allocated_storage          = 20
  multi_az                   = false
  deletion_protection        = false
}

# ECS Service - UI (Next.js)
module "ecs_service_ui" {
  source = "../../modules/ecs-service"

  project_name                   = var.project_name
  environment                    = var.environment
  service_name                   = "ui"
  vpc_id                         = module.vpc.vpc_id
  private_subnet_ids             = module.vpc.private_subnet_ids
  ecs_cluster_id                 = module.ecs_cluster.cluster_id
  ecs_cluster_name               = module.ecs_cluster.cluster_name
  ecr_repository_url             = module.ecr.repository_urls["ui"]
  container_port                 = 3000
  cpu                            = 256
  memory                         = 512
  desired_count                  = 1
  task_execution_role_arn        = module.ecs_cluster.task_execution_role_arn
  task_role_arn                  = module.ecs_cluster.task_role_arn
  log_group_name                 = module.ecs_cluster.log_group_name
  aws_region                     = var.aws_region
  allowed_security_group_ids     = [module.alb.alb_security_group_id]
  target_group_arn               = module.alb.ui_target_group_arn
  enable_service_discovery       = true
  service_discovery_namespace_id = module.ecs_cluster.service_discovery_namespace_id

  environment_variables = [
    {
      name  = "NEXT_PUBLIC_API_URL"
      value = "http://${module.alb.alb_dns_name}/api"
    }
  ]
}

# ECS Service - API Service (Spring Boot)
module "ecs_service_api" {
  source = "../../modules/ecs-service"

  project_name                   = var.project_name
  environment                    = var.environment
  service_name                   = "service"
  vpc_id                         = module.vpc.vpc_id
  private_subnet_ids             = module.vpc.private_subnet_ids
  ecs_cluster_id                 = module.ecs_cluster.cluster_id
  ecs_cluster_name               = module.ecs_cluster.cluster_name
  ecr_repository_url             = module.ecr.repository_urls["service"]
  container_port                 = 8080
  cpu                            = 512
  memory                         = 1024
  desired_count                  = 1
  task_execution_role_arn        = module.ecs_cluster.task_execution_role_arn
  task_role_arn                  = module.ecs_cluster.task_role_arn
  log_group_name                 = module.ecs_cluster.log_group_name
  aws_region                     = var.aws_region
  allowed_security_group_ids     = [module.alb.alb_security_group_id]
  target_group_arn               = module.alb.service_target_group_arn
  enable_service_discovery       = true
  service_discovery_namespace_id = module.ecs_cluster.service_discovery_namespace_id

  environment_variables = [
    {
      name  = "SPRING_PROFILES_ACTIVE"
      value = "dev"
    },
    {
      name  = "REPOSITORY_URL"
      value = "http://repository.${var.project_name}.local:8081"
    }
  ]

  container_health_check = {
    command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
    interval    = 30
    timeout     = 5
    retries     = 3
    startPeriod = 60
  }
}

# ECS Service - Repository (Spring Boot - Internal Only)
module "ecs_service_repository" {
  source = "../../modules/ecs-service"

  project_name                   = var.project_name
  environment                    = var.environment
  service_name                   = "repository"
  vpc_id                         = module.vpc.vpc_id
  private_subnet_ids             = module.vpc.private_subnet_ids
  ecs_cluster_id                 = module.ecs_cluster.cluster_id
  ecs_cluster_name               = module.ecs_cluster.cluster_name
  ecr_repository_url             = module.ecr.repository_urls["repository"]
  container_port                 = 8081
  cpu                            = 512
  memory                         = 1024
  desired_count                  = 1
  task_execution_role_arn        = module.ecs_cluster.task_execution_role_arn
  task_role_arn                  = module.ecs_cluster.task_role_arn
  log_group_name                 = module.ecs_cluster.log_group_name
  aws_region                     = var.aws_region
  allowed_security_group_ids     = [module.ecs_service_api.security_group_id]
  target_group_arn               = "" # No ALB - internal only
  enable_service_discovery       = true
  service_discovery_namespace_id = module.ecs_cluster.service_discovery_namespace_id

  environment_variables = [
    {
      name  = "SPRING_PROFILES_ACTIVE"
      value = "dev"
    }
  ]

  secrets = [
    {
      name      = "DB_CREDENTIALS"
      valueFrom = module.rds.db_secret_arn
    }
  ]

  container_health_check = {
    command     = ["CMD-SHELL", "curl -f http://localhost:8081/actuator/health || exit 1"]
    interval    = 30
    timeout     = 5
    retries     = 3
    startPeriod = 60
  }
}
