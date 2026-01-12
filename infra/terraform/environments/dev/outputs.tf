# Dev Environment - outputs.tf

output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "alb_dns_name" {
  description = "ALB DNS name"
  value       = module.alb.alb_dns_name
}

output "ecr_repository_urls" {
  description = "ECR repository URLs"
  value       = module.ecr.repository_urls
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = module.ecs_cluster.cluster_name
}

output "rds_endpoint" {
  description = "RDS endpoint"
  value       = module.rds.db_endpoint
}

output "rds_secret_arn" {
  description = "RDS credentials secret ARN"
  value       = module.rds.db_secret_arn
}

output "service_discovery_namespace" {
  description = "Service discovery namespace"
  value       = module.ecs_cluster.service_discovery_namespace_name
}

output "service_urls" {
  description = "Service URLs"
  value = {
    ui         = "http://${module.alb.alb_dns_name}"
    api        = "http://${module.alb.alb_dns_name}/api"
    repository = "repository.${module.ecs_cluster.service_discovery_namespace_name}:8081"
  }
}
