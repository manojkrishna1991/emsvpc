variable "region" {
  default = "us-east-1"
}

variable "acm_cert_arn" {
  description = "ACM cert for ALB HTTPS (optional, not yet wired to a listener)"
  default     = ""
}

variable "db_name" {
  default = "postgres"
}

variable "db_username" {
  default = "entitlement_admin"
}

variable "db_instance_class" {
  default = "db.t4g.micro"
}

variable "github_repo" {
  description = "GitHub org/repo allowed to assume the deploy role"
  default     = "manojkrishna1991/emsvpc"
}
