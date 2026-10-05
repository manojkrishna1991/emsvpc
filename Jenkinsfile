pipeline {
  agent any
  environment {
    S3_BUCKET = 'project-deploy-416754239002'
    AWS_REGION = 'us-east-1'
    APP_INSTANCE_ID = 'i-0fd0552ee37930855' // private app server 10-0-141-199
  }
  stages {
    stage('Build') {
      steps { sh './gradlew clean bootJar -x test' }
    }
    stage('Upload to S3') {
      steps {
        sh '''
          aws s3 cp build/libs/ems-service-0.0.1-SNAPSHOT.jar s3://$S3_BUCKET/entitlement/app.jar --region $AWS_REGION
          aws s3 ls s3://$S3_BUCKET/entitlement/ --region $AWS_REGION
        '''
      }
    }
    stage('Deploy via SSM') {
      steps {
        sh '''
cat > /tmp/deploy.py <<'PY'
import boto3, time, os

ssm = boto3.client('ssm', region_name=os.environ['AWS_REGION'])
instance_id = os.environ['APP_INSTANCE_ID']
bucket = os.environ['S3_BUCKET']
secret_name = "arn:aws:secretsmanager:us-east-1:416754239002:secret:rds!db-1be2ffeb-f5c1-489f-b0ef-f367b57963fe-LUCY1I"

cmds = [
    "if ! command -v aws >/dev/null 2>&1; then apt update && apt install -y awscli unzip curl; fi",
    "if ! command -v java >/dev/null 2>&1; then apt update && apt install -y openjdk-21-jre; fi",
    "sudo mkdir -p /opt/app && sudo chown ubuntu:ubuntu /opt/app",
    f"aws s3 cp s3://{bucket}/entitlement/app.jar /opt/app/app.jar --region {os.environ['AWS_REGION']}",
    "sudo chown ubuntu:ubuntu /opt/app/app.jar",
    f"""sudo tee /usr/local/bin/run-entitlement.sh > /dev/null <<'EOS'
#!/bin/bash
set -e
SECRET_JSON=$(aws secretsmanager get-secret-value --secret-id {secret_name} --region us-east-1 --query SecretString --output text)
export DB_USER=$(echo "$SECRET_JSON" | python3 -c "import json,sys; print(json.load(sys.stdin)['username'])")
export DB_PASS=$(echo "$SECRET_JSON" | python3 -c "import json,sys; print(json.load(sys.stdin)['password'])")
DB_HOST_VAL=$(echo "$SECRET_JSON" | python3 -c "import json,sys; print(json.load(sys.stdin).get('host',''))")
if [ -n "$DB_HOST_VAL" ]; then export DB_HOST="$DB_HOST_VAL"; fi
exec /usr/bin/java -jar /opt/app/app.jar --server.port=8080
EOS
sudo chmod +x /usr/local/bin/run-entitlement.sh""",
    """sudo tee /etc/systemd/system/entitlement.service > /dev/null <<'EOF'
[Unit]
Description=Entitlement
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/opt/app
ExecStart=/usr/local/bin/run-entitlement.sh
Restart=always
Environment=JAVA_OPTS=-Xms256m -Xmx512m

[Install]
WantedBy=multi-user.target
EOF""",
    "sudo systemctl daemon-reload",
    "sudo systemctl enable entitlement",
    "sudo systemctl restart entitlement",
    "sleep 10",
    "sudo systemctl status entitlement --no-pager -l || true",
    "curl -s http://localhost:8080/api/v1/health || (echo 'HEALTH FAILED - checking logs:'; sudo journalctl -u entitlement -n 30 --no-pager); true"
]

resp = ssm.send_command(
    Targets=[{"Key":"InstanceIds","Values":[instance_id]}],
    DocumentName="AWS-RunShellScript",
    Parameters={"commands": cmds}
)
cid = resp['Command']['CommandId']
print(f"SSM CommandId: {cid}")
time.sleep(25)
inv = ssm.list_command_invocations(CommandId=cid, Details=True)['CommandInvocations'][0]
print(inv['CommandPlugins'][0]['Output'])
if inv['Status'] not in ['Success','InProgress']:
    raise Exception(f"Deploy failed: {inv['Status']}")
PY
python3 /tmp/deploy.py
        '''
      }
    }
  }
}