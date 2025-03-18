pipeline {
    agent any
    environment {
        MYSQL_HOST = "host.docker.internal"
        MONGO_HOST = "host.docker.internal"
        ES_HOST = "host.docker.internal"
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build & Deploy Recommend') {
            steps {
                echo "Rebuilding and deploying Recommend service using docker-compose..."
                // docker-compose.yml 파일에 정의된 'recommend' 서비스만 재빌드 및 실행합니다.
                sh 'docker-compose up -d --no-deps --build recommend'
            }
        }
    }
    post {
        always {
            echo "Recommend Pipeline completed."
        }
    }
}