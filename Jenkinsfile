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
                    // 환경 변수 파일을 작업 디렉토리에 임시로 저장
                    sh 'cat $ENV_FILE > ${WORKSPACE}/.env'

                    // 환경 변수 파일이 생성되었는지 확인
                    sh 'echo "환경 변수 파일이 생성되었습니다."'

                    // 환경 변수 내용 확인 (마스킹 처리 없이)
                    sh 'echo "환경 변수 파일 내용:" && cat ${WORKSPACE}/.env'

                    sh 'ls -la ${WORKSPACE}/'
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
        stage('Build & Deploy Frontend & Nginx') {
            steps {
                echo "Frontend를 재빌드 및 재배포합니다..."
                sh 'docker-compose up -d --no-deps --build fe'
            }
        }
    }
    post {
        success {
            script {
                def mattermostWebhook = 'https://meeting.ssafy.com/hooks/7wymxz3oztnfino8nt3sfc5dyo'
                def payload = """{
                    "text": "# :world_map: Jenkins Job < '${env.JOB_NAME}' > 빌드 성공!"
                }"""
                sh "curl -i -X POST -H 'Content-Type: application/json' -d '${payload}' ${mattermostWebhook}"
            }
        }
        failure {
            script {
                def mattermostWebhook = 'https://meeting.ssafy.com/hooks/7wymxz3oztnfino8nt3sfc5dyo'
                def payload = """{
                    "text": "# :warning: :world_map: Jenkins Job < '${env.JOB_NAME}' >에서 빌드 에러 발생! 확인이 필요합니다."
                }"""
                sh "curl -i -X POST -H 'Content-Type: application/json' -d '${payload}' ${mattermostWebhook}"
            }
        }
        always {
            echo "frontend Pipeline 완료."
        }
    }
}