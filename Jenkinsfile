import groovy.json.JsonOutput

pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Setup Environment Variables') {
            steps {
                echo "API key 환경 변수 설정 중..."
                withCredentials([file(credentialsId: 'backend_env', variable: 'ENV_FILE')]) {
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
                sh 'docker-compose build --force-rm'
                sh 'docker-compose up -d --no-deps'
                script {
                    if (currentBuild.number % 5 == 0) {
                        echo "5회 빌드 주기 도달 - 사용하지 않는 Docker 자원 정리합니다."
                        sh 'docker system prune -f'
                    } else {
                        echo "Docker 정리 건너뜁니다. (빌드 번호: ${currentBuild.number})"
                    }
                }
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
