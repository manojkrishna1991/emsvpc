
# Entitlement - Cloud Native Prod Template

## What companies do
1. docker build → ECR (immutable tag: git SHA)
2. terraform apply → VPC 3AZ + ALB HTTPS + ECS Fargate 3 replicas
3. GitHub Actions → test, scan, push, update-service
4. Secrets via Secrets Manager → External Secrets Operator
5. Logs → CloudWatch, Metrics → Prometheus/Grafana, Tracing → X-Ray

## Quick start
1. Replace 416754239002 with your account
2. cd terraform && terraform init && terraform apply -var="acm_cert_arn=arn:..." -var="db_secret_arn=arn:..."
3. Push to main → GitHub Action deploys

## Checklist vs your current EC2 jar
- [ ] EC2 jar → ECS Fargate (HA)
- [ ] Manual SSM → GitOps
- [ ] Hardcoded pass → Secrets Manager
- [ ] HTTP ALB → HTTPS + WAF
- [ ] Single AZ → 3 AZ
