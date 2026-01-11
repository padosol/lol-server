# ALB Module - outputs.tf

output "alb_arn" {
  description = "ALB ARN"
  value       = aws_lb.main.arn
}

output "alb_dns_name" {
  description = "ALB DNS name"
  value       = aws_lb.main.dns_name
}

output "alb_zone_id" {
  description = "ALB Zone ID"
  value       = aws_lb.main.zone_id
}

output "alb_security_group_id" {
  description = "ALB Security Group ID"
  value       = aws_security_group.alb.id
}

output "ui_target_group_arn" {
  description = "UI Target Group ARN"
  value       = aws_lb_target_group.ui.arn
}

output "service_target_group_arn" {
  description = "Service Target Group ARN"
  value       = aws_lb_target_group.service.arn
}

output "listener_arn" {
  description = "Active listener ARN (HTTPS or HTTP)"
  value       = var.certificate_arn != "" ? aws_lb_listener.https[0].arn : aws_lb_listener.http_dev[0].arn
}
