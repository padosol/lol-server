# ECS Service Module - outputs.tf

output "service_name" {
  description = "ECS Service name"
  value       = aws_ecs_service.main.name
}

output "service_id" {
  description = "ECS Service ID"
  value       = aws_ecs_service.main.id
}

output "task_definition_arn" {
  description = "Task Definition ARN"
  value       = aws_ecs_task_definition.main.arn
}

output "task_definition_family" {
  description = "Task Definition family"
  value       = aws_ecs_task_definition.main.family
}

output "security_group_id" {
  description = "Security Group ID"
  value       = aws_security_group.ecs_service.id
}

output "service_discovery_arn" {
  description = "Service discovery service ARN"
  value       = var.enable_service_discovery ? aws_service_discovery_service.main[0].arn : null
}

output "service_discovery_name" {
  description = "Service discovery DNS name"
  value       = var.enable_service_discovery ? "${var.service_name}.${var.service_discovery_namespace_id}" : null
}
