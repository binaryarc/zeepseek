pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps {
                // SCM 설정은 이미 Jenkins Item에서 구성되어 있으므로 checkout scm만 사용합니다.
                checkout scm
            }
        }
        stage('Build & Deploy Frontend') {
            steps {
                echo "Rebuilding and deploying Frontend service using docker-compose..."
                // docker-compose.yml에 정의된 'fe' 서비스만 재빌드 및 재시작합니다.
                sh 'docker-compose up -d --no-deps --build fe'
            }
        }
    }
    post {
        always {
            echo "Frontend Pipeline completed."
        }
    }
}