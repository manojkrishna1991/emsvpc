pipeline {
  agent any
  environment {
    S3_BUCKET = 'project-deploy-416754239002'
    AWS_REGION = 'us-east-1'
    APP_INSTANCE_ID = 'i-0fd0552ee37930855' // 10-0-141-199 private app server
  }
  stages {
    stage('Build') { steps { sh './gradlew clean bootJar -x test' } }
    stage('Upload') { steps { sh 'aws s3 cp build/libs/ems-service-0.0.1-SNAPSHOT.jar s3://$S3_BUCKET/entitlement/app.jar --region $AWS_REGION' } }
    stage('Deploy') {
      steps {
        sh '''
cat > /tmp/deploy.py <<'PY'
import boto3, time, os
ssm = boto3.client('ssm', region_name='us-east-1')
app_id = os.environ['APP_INSTANCE_ID']
cmds = [
    "sudo mkdir -p /opt/app",
    f"aws s3 cp s3://{os.environ['S3_BUCKET']}/entitlement/app.jar /opt/app/app.jar --region us-east-1",
    "printf '[Unit]\\nDescription=Entitlement\\nAfter=network.target\\n[Service]\\nUser=ubuntu\\nWorkingDirectory=/opt/app\\nExecStart=/usr/bin/java -jar /opt/app/app.jar --server.port=8080\\nRestart=always\\n[Install]\\nWantedBy=multi-user.target\\n' | sudo tee /etc/systemd/system/entitlement.service",
    "sudo systemctl daemon-reload",
    "sudo systemctl enable entitlement",
    "sudo systemctl restart entitlement",
    "sleep 5",
    "curl -s http://localhost:8080/api/v1/health || echo FAILED"
]
resp = ssm.send_command(Targets=[{"Key":"InstanceIds","Values":[app_id]}], DocumentName="AWS-RunShellScript", Parameters={"commands": cmds})
print("CommandId:", resp['Command']['CommandId'])
PY
python3 /tmp/deploy.py
        '''
      }
    }
  }
}