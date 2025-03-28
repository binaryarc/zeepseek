import groovy.json.JsonOutput

pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps {
                // SCM 설정이 되어 있으므로 checkout scm만 사용
                checkout scm
            }
        }
        stage('Setup Environment Variables') {
            steps {
                echo "API key 환경 변수 설정 중..."
                withCredentials([file(credentialsId: 'backend_env', variable: 'ENV_FILE')]) {
                    // 환경 변수 파일을 작업 디렉토리에 저장
                    sh 'cat $ENV_FILE > ${WORKSPACE}/.env'
                    sh 'echo "환경 변수 파일이 생성되었습니다."'
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
        stage('Build & Deploy Backend') {
            steps {
                echo "Backend 서비스를 재빌드 및 재배포합니다..."
                // 멀티스테이지 Dockerfile을 활용하여 빌드 (중간 컨테이너 제거 옵션 포함)
                sh 'docker-compose build --force-rm'
                // 지정된 서비스만 재시작 (의존 컨테이너는 그대로 사용)
                sh 'docker-compose up -d --no-deps'
                // 빌드 후 불필요한 이미지, 컨테이너, 네트워크 정리 (선택 사항)
                sh 'docker system prune -f'
            }
        }
    }
    post {
        success {
            script {
                def mattermostWebhook = 'https://meeting.ssafy.com/hooks/7wymxz3oztnfino8nt3sfc5dyo'
                def payload = JsonOutput.toJson([text: "# :data: Jenkins Job < '${env.JOB_NAME}' > 빌드 성공!"])
                sh "curl -i -X POST -H 'Content-Type: application/json' -d '${payload}' ${mattermostWebhook}"
            }
        }
        failure {
            script {
                def mattermostWebhook = 'https://meeting.ssafy.com/hooks/7wymxz3oztnfino8nt3sfc5dyo'
                def payload = JsonOutput.toJson([text: "# :warning: :data: Jenkins Job < '${env.JOB_NAME}' >에서 빌드 에러 발생! 확인이 필요합니다."])
                sh "curl -i -X POST -H 'Content-Type: application/json' -d '${payload}' ${mattermostWebhook}"
            }
        }
        always {
            echo "Backend Pipeline 완료."
        }
    }
}
