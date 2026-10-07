# Entitlement Service — ECS Deployment

Spring Boot entitlement service, deployed to AWS ECS Fargate behind an ALB, with its own VPC and RDS Postgres instance, provisioned by Terraform. GitHub Actions builds, tests, and redeploys on every push to `main`.

## Architecture

```
GitHub (push to main)
  └─ GitHub Actions (OIDC → GitHubActionsDeployRole)
       ├─ ./gradlew test
       ├─ docker build & push → ECR
       └─ aws ecs update-service --force-new-deployment
                                      │
                                      ▼
Internet → ALB (:80) → Target Group (ip) → ECS Fargate service (3 tasks, awsvpc)
                                                   │
                                                   ▼
                                         RDS Postgres (private subnets)
```

Everything except the RDS master password lives in [terraform/](terraform/):
- `main.tf` — VPC (3 AZ, public+private subnets, NAT), ECR repo, ALB, target group, ECS cluster/task definition/service
- `iam.tf` — ECS task/execution roles, security groups, CloudWatch log group, GitHub OIDC provider + deploy role
- `rds.tf` — RDS Postgres instance, its subnet group and security group
- `variables.tf` — everything you're expected to override per account/environment

The app's health endpoint is `/api/v1/health` (plain `@RestController`, **not** Spring Actuator — there's no `spring-boot-starter-actuator` dependency). The Dockerfile's `HEALTHCHECK` and the Terraform ALB target group both point at this path. If you ever add actuator, update both or `/actuator/health` will 404 and the ALB will mark every task unhealthy forever.

## Prerequisites

- AWS CLI v2, authenticated (`aws sts get-caller-identity`) with permissions to create VPC/IAM/RDS/ECS/ALB/ECR resources
- [Terraform](https://developer.hashicorp.com/terraform/install) ≥ 1.5
- Docker (with `buildx`)
- A GitHub repo you can push to and configure Actions on

## First-time setup in a new AWS account

1. **Point Terraform at your account/repo.** Edit [terraform/variables.tf](terraform/variables.tf):
   - `region` — target AWS region (default `us-east-1`)
   - `github_repo` — your `org/repo`, used to scope the GitHub OIDC trust policy (e.g. `"manojkrishna1991/emsvpc"`)
   - `db_name`, `db_username`, `db_instance_class` — dev defaults are a `db.t4g.micro`, single-AZ, no deletion protection; **bump these for anything beyond dev**

2. **Init and review:**
   ```bash
   cd terraform
   terraform init
   terraform plan -out=tfplan
   ```
   On a genuinely fresh account this should be a clean plan with nothing to import. (We only needed `terraform import` in this repo's original account because an ECR repo, `ecsTaskExecutionRole`, and a CloudWatch log group already existed from manual testing before Terraform was introduced — if `apply` fails with `EntityAlreadyExists`/`ResourceInUse`, that's why; `terraform import <resource>.<name> <id>` adopts the existing resource instead of fighting it.)

3. **Apply.** This takes ~10–15 minutes (NAT gateways and RDS are the slow parts):
   ```bash
   terraform apply "tfplan"
   ```

4. **Build and push the image.** The task definition always deploys whatever `:latest` currently resolves to in ECR, so the first push has to happen manually:
   ```bash
   aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <account-id>.dkr.ecr.<region>.amazonaws.com
   docker buildx build --platform linux/amd64 -t <account-id>.dkr.ecr.<region>.amazonaws.com/entitlement:latest --push .
   ```
   **Always build with `--platform linux/amd64`**, even on Apple Silicon — Fargate's task definition here doesn't set `runtime_platform`, so it defaults to x86_64. A plain `docker build` on an M-series Mac produces an arm64 image that passes `docker build` but crashes every task at startup with `exec format error`. GitHub Actions' `ubuntu-latest` runners are amd64 natively, so CI-built images aren't affected — only manual local builds need the explicit platform flag.

5. **Verify:**
   ```bash
   aws ecs describe-services --cluster entitlement-cluster --services entitlement --query "services[0].{running:runningCount,desired:desiredCount}"
   aws elbv2 describe-target-health --target-group-arn $(aws elbv2 describe-target-groups --names entitlement-tg --query "TargetGroups[0].TargetGroupArn" --output text)
   curl http://$(aws elbv2 describe-load-balancers --names entitlement-alb --query "LoadBalancers[0].DNSName" --output text)/api/v1/health
   ```

6. **Enable the GitHub Actions pipeline** — Terraform already created the OIDC provider and `GitHubActionsDeployRole` scoped to `var.github_repo`, and [.github/workflows/deploy.yml](.github/workflows/deploy.yml) is wired to assume it. Update the hardcoded account ID/region in that workflow's `env:` block to match your account, then push to `main`.

## Continuous build & deploy

[.github/workflows/deploy.yml](.github/workflows/deploy.yml) runs on every push to `main`:
1. `./gradlew test` — uses an in-memory H2 database ([src/test/resources/application.properties](src/test/resources/application.properties)), not the real RDS instance, so CI never needs network access to the VPC
2. Builds and pushes the image to ECR, tagged both `:latest` and `:<git-sha>`
3. `aws ecs update-service --force-new-deployment` — pulls the fresh `:latest` image into the existing service

This redeploys in place; there's no separate stage/prod split yet (everything here is one environment). Promoting to multiple environments would mean parameterizing the Terraform (env-scoped resource names, separate state) and deploying by immutable image digest/SHA tag rather than `:latest`, gated by a manual approval for prod — ask if you want that built out.

## Known limitations (this is a dev setup)

- `spring.jpa.hibernate.ddl-auto=update` lets Hibernate auto-manage the schema — fine for dev, **not safe for a real environment** (risk of unintended schema changes). Swap for a migration tool (Flyway/Liquibase) or `validate` once the schema is stable.
- RDS is single-AZ, `db.t4g.micro`, `skip_final_snapshot=true`, `deletion_protection=false` — a `terraform destroy` deletes it with **no backup**.
- ALB is HTTP-only on port 80. `var.acm_cert_arn` exists but isn't wired to an HTTPS listener yet.
- One environment, one AWS account, one Terraform state file (local, not remote). Add an S3+DynamoDB (or equivalent) backend before anyone else touches this.

## Tearing it down

```bash
cd terraform
terraform destroy
```
This deletes the RDS instance with no snapshot and the image in ECR along with it. Double-check you actually want this before running it.
