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
                    "text": "## Jenkins 빌드 알림\n**Job:** \`${env.JOB_NAME}\`\n:rocket: **빌드 성공!** 배포가 완료되었습니다.\n\n자세한 정보는 Jenkins 콘솔 로그를 확인하세요."
                }"""
                sh "curl -i -X POST -H 'Content-Type: application/json' -d '${payload}' ${mattermostWebhook}"
            }
        }
        failure {
            script {
                // Mattermost Incoming Webhook URL
                def mattermostWebhook = 'https://meeting.ssafy.com/hooks/7wymxz3oztnfino8nt3sfc5dyo'
                // 전송할 메시지 내용 (원하는 메시지로 변경 가능)
                def payload = """{
                    "text": "## Jenkins 빌드 알림\n**Job:** \`${env.JOB_NAME}\`\n:warning: **빌드 에러 발생!** 확인이 필요합니다.\n\n자세한 로그는 Jenkins 콘솔 로그를 확인하세요.\n\n*에러 발생 시 즉각적인 확인 바랍니다.*"
                }"""
                // curl 명령어를 이용해 HTTP POST 요청 전송
                sh "curl -i -X POST -H 'Content-Type: application/json' -d '${payload}' ${mattermostWebhook}"
            }
        }
        always {
            echo "Backend Pipeline 완료."
        }
    }
}