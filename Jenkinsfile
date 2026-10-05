pipeline {
  agent any
  environment {
    S3_BUCKET = 'project-deploy-416754239002'
    AWS_REGION = 'us-east-1'
    APP_NAME = 'project-app-server'
  }
  stages {
    stage('Build') {
      steps {
        sh './gradlew clean bootJar -x test'
      }
    }
    stage('Upload to S3') {
      steps {
        sh '''
          aws s3 cp build/libs/ems-service-0.0.1-SNAPSHOT.jar s3://$S3_BUCKET/entitlement/app.jar --region $AWS_REGION
          echo "Uploaded to s3://$S3_BUCKET/entitlement/app.jar"
        '''
      }
    }
    stage('Deploy via SSM') {
      steps {
        sh '''
          cat > /tmp/deploy.py <<'PY'
import boto3, time, sys
region = "us-east-1"
ssm = boto3.client('ssm', region_name=region)

commands = [
    "sudo mkdir -p /opt/app",
    "aws s3 cp s3://project-deploy-416754239002/entitlement/app.jar /opt/app/app.jar --region us-east-1",
    "sudo bash -c 'cat > /etc/systemd/system/entitlement.service <<EOF\n[Unit]\nDescription=Entitlement Service\nAfter=network.target\n[Service]\nUser=ubuntu\nWorkingDirectory=/opt/app\nExecStart=/usr/bin/java -jar /opt/app/app.jar --server.port=8080\nRestart=always\nRestartSec=10\n[Install]\nWantedBy=multi-user.target\nEOF'",
    "sudo systemctl daemon-reload",
    "sudo systemctl enable entitlement",
    "sudo systemctl restart entitlement",
    "sleep 8",
    "sudo systemctl status entitlement --no-pager -l",
    "curl -s http://localhost:8080/api/v1/health || echo FAILED",
    "sudo journalctl -u entitlement -n 30 --no-pager"
]

resp = ssm.send_command(
    Targets=[{"Key":"tag:Name","Values":["project-app-server"]}],
    DocumentName="AWS-RunShellScript",
    Parameters={"commands": commands}
)
cid = resp['Command']['CommandId']
print(f"CommandId: {cid}")

# wait and fetch output
for i in range(12):
    time.sleep(5)
    invs = ssm.list_command_invocations(CommandId=cid, Details=True)['CommandInvocations']
    if invs and invs[0]['Status'] not in ['Pending','InProgress']:
        print(invs[0]['CommandPlugins'][0]['Output'])
        if 'FAILED' in invs[0]['CommandPlugins'][0]['Output']:
            sys.exit(1)
        break
    print(f"Waiting... {invs[0]['Status'] if invs else 'Pending'}")
PY
          python3 /tmp/deploy.py
        '''
      }
    }
  }
}