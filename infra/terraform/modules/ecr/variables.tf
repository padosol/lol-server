# ECR Module - variables.tf

variable "project_name" {
  description = "Project name for resource naming"
  type        = string
}

variable "environment" {
  description = "Environment (dev, staging, prod)"
  type        = string
}

variable "repository_names" {
  description = "List of ECR repository names"
  type        = list(string)
  default     = ["service", "repository", "ui"]
}

variable "image_count_to_keep" {
  description = "Number of images to keep in each repository"
  type        = number
  default     = 10
}
