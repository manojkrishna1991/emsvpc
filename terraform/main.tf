provider "aws" {
  region = var.region
}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "5.8.1"

  name               = "entitlement-vpc"
  cidr               = "10.0.0.0/16"
  azs                = ["${var.region}a", "${var.region}b", "${var.region}c"]
  private_subnets    = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets     = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]
  enable_nat_gateway = true
  single_nat_gateway = false
}

resource "aws_ecr_repository" "app" {
  name = "entitlement"
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_lb" "app" {
  name               = "entitlement-alb"
  internal           = false
  load_balancer_type = "application"
  subnets            = module.vpc.public_subnets
  security_groups    = [aws_security_group.alb.id]
}

resource "aws_lb_target_group" "app" {
  name_prefix = "ent-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = module.vpc.vpc_id
  target_type = "ip"

  lifecycle {
    create_before_destroy = true
  }

  health_check {
    path                = "/api/v1/health"
    port                = "8080"
    interval            = 10
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.app.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}

resource "aws_ecs_cluster" "main" {
  name = "entitlement-cluster"
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_ecs_task_definition" "app" {
  family                   = "entitlement"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_exec.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name         = "entitlement"
    image        = "${aws_ecr_repository.app.repository_url}:latest"
    portMappings = [{ containerPort : 8080 }]
    secrets = [
      { name : "DB_USER", valueFrom : "${aws_db_instance.app.master_user_secret[0].secret_arn}:username::" },
      { name : "DB_PASS", valueFrom : "${aws_db_instance.app.master_user_secret[0].secret_arn}:password::" }
    ]
    environment = [
      { name : "SPRING_PROFILES_ACTIVE", value : "prod" },
      { name : "DB_HOST", value : aws_db_instance.app.address }
    ]
    logConfiguration = {
      logDriver : "awslogs",
      options : {
        "awslogs-group" : aws_cloudwatch_log_group.app.name,
        "awslogs-region" : var.region,
        "awslogs-stream-prefix" : "ecs"
      }
    }
  }])
}

resource "aws_ecs_service" "app" {
  name            = "entitlement"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 3
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = module.vpc.private_subnets
    security_groups = [aws_security_group.ecs.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = "entitlement"
    container_port   = 8080
  }
}
