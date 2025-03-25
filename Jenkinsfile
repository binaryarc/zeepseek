import groovy.json.JsonOutput

pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps {
                // SCM 설정은 이미 Jenkins Item에서 구성되어 있으므로, checkout scm 만 사용합니다.
                checkout scm
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
                sh 'docker-compose down'
            }
        }
        stage('Build & Deploy Backend') {
            steps {
                echo "Backend 서비스를 재빌드 및 재배포합니다..."
                // docker-compose.yml에 정의된 'be' 서비스만 재빌드 및 재시작합니다.
                sh 'docker-compose up -d --no-deps --build be'
            }
        }
    }
    post {
        success {
            script {
                def mattermostWebhook = 'https://meeting.ssafy.com/hooks/7wymxz3oztnfino8nt3sfc5dyo'
                def payload = """{
                    "text": "# Jenkins Job < '${env.JOB_NAME}' > 빌드 성공!"
                }"""
                sh "curl -i -X POST -H 'Content-Type: application/json' -d '${payload}' ${mattermostWebhook}"
            }
        }
        failure {
            script {
                def mattermostWebhook = 'https://meeting.ssafy.com/hooks/7wymxz3oztnfino8nt3sfc5dyo'
                def payload = """{
                    "text": "# Jenkins Job < '${env.JOB_NAME}' >에서 빌드 에러 발생! 확인이 필요합니다."
                }"""
                sh "curl -i -X POST -H 'Content-Type: application/json' -d '${payload}' ${mattermostWebhook}"
            }
        }
        always {
            echo "Backend Pipeline 완료."
        }
    }
}
