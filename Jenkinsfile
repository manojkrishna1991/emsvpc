pipeline {
  agent any
  environment {
    S3_BUCKET = 'project-deploy-416754239002'
    AWS_REGION = 'us-east-1'
    APP_TAG = 'project-app-server'
  }
  stages {
    stage('Checkout') {
      steps { checkout scm }
    }
    stage('Build JAR') {
      steps {
        sh 'chmod +x gradlew'
        sh './gradlew clean build -x test'
      }
    }
    stage('Upload to S3') {
      steps {
        sh 'aws s3 cp build/libs/ems-service-0.0.1-SNAPSHOT.jar s3://$S3_BUCKET/entitlement/app.jar --region $AWS_REGION'
      }
    }
    stage('Deploy to Private EC2 via SSM') {
      steps {
        sh '''
          cat > /tmp/ssm-params.json <<'JSON'
          {
            "commands": [
              "sudo mkdir -p /opt/app",
              "aws s3 cp s3://project-deploy-416754239002/entitlement/app.jar /opt/app/app.jar --region us-east-1",
              "sudo systemctl restart entitlement || sudo systemctl start entitlement",
              "sleep 3",
              "sudo systemctl status entitlement --no-pager -l"
            ]
          }
          JSON

          aws ssm send-command \
            --targets Key=tag:Name,Values=project-app-server \
            --document-name AWS-RunShellScript \
            --parameters file:///tmp/ssm-params.json \
            --region ap-south-1
        '''
      }
    }
  }
}