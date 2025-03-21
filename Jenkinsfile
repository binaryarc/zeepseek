pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps {
                // 기존 SCM 설정 사용
                checkout scm
            }
        }

        // 환경 변수 설정 -> OAuth용 credentials 추가
        stage('Setup Environment Variables') {
            steps {
                echo "OAuth 환경 변수 설정 중..."
                withCredentials([file(credentialsId: 'oauth2_env', variable: 'ENV_FILE')]) {
                    // FE 디렉토리에 .env 파일 복사
                    sh 'cp $ENV_FILE ${WORKSPACE}/FE/.env'
            
                    // 복사 확인
                    sh 'echo "환경 변수 파일이 복사되었습니다."'
                    sh 'ls -la ${WORKSPACE}/FE/'
                }
            }
        }

        stage('Prepare Environment') {
            steps {
                echo "네트워크 e203 확인 및 생성..."
                sh '''
                    if ! docker network inspect e203 > /dev/null 2>&1; then
                        echo "네트워크 e203가 존재하지 않습니다. 생성합니다."
                        docker network create e203
                    else
                        echo "네트워크 e203가 이미 존재합니다."
                    fi
                '''
            }
        }
        stage('Cleanup Old Containers') {
            steps {
                echo "기존 컨테이너 정리 중..."
                // docker-compose.yml이 있는 디렉토리에서 기존 컨테이너 중지 및 삭제
                sh 'docker-compose down'
            }
        }
        stage('Build & Deploy Frontend & Nginx') {
            steps {
                echo "Frontend 및 Nginx proxy를 재빌드 및 재배포합니다..."
                sh 'docker-compose up -d --no-deps --build fe nginx'
            }
        }
    }
    post {
        always {
            echo "Frontend & Nginx Pipeline 완료."
        }
    }
}