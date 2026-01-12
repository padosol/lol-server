# ECS Cluster Module - outputs.tf

output "cluster_id" {
  description = "ECS Cluster ID"
  value       = aws_ecs_cluster.main.id
}

output "cluster_arn" {
  description = "ECS Cluster ARN"
  value       = aws_ecs_cluster.main.arn
}

output "cluster_name" {
  description = "ECS Cluster name"
  value       = aws_ecs_cluster.main.name
}

output "service_discovery_namespace_id" {
  description = "Service discovery namespace ID"
  value       = aws_service_discovery_private_dns_namespace.main.id
}

output "service_discovery_namespace_arn" {
  description = "Service discovery namespace ARN"
  value       = aws_service_discovery_private_dns_namespace.main.arn
}

output "service_discovery_namespace_name" {
  description = "Service discovery namespace name"
  value       = aws_service_discovery_private_dns_namespace.main.name
}

output "log_group_name" {
  description = "CloudWatch log group name"
  value       = aws_cloudwatch_log_group.ecs.name
}

output "task_execution_role_arn" {
  description = "ECS Task Execution Role ARN"
  value       = aws_iam_role.ecs_task_execution.arn
}

output "task_role_arn" {
  description = "ECS Task Role ARN"
  value       = aws_iam_role.ecs_task.arn
}
