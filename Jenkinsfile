pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps {
                // SCM 설정은 이미 Jenkins Item에서 구성되어 있으므로 checkout scm만 사용합니다.
                checkout scm
            }
        }
        stage('Build & Deploy Frontend & Nginx') {
            steps {
                echo "Rebuilding and deploying Frontend and Nginx proxy using docker-compose..."
                // fe와 nginx 서비스만 재빌드 및 재시작합니다.
                sh 'docker-compose up -d --no-deps --build fe nginx'
            }
        }
    }
    post {
        always {
            echo "Frontend & Nginx Pipeline completed."
        }
    }
}