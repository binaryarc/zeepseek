# 1단계: Build Stage - Node.js 22를 사용하여 Vite 앱 빌드
FROM node:22-alpine AS builder
WORKDIR /app

# package.json 및 package-lock.json (또는 yarn.lock) 복사 후 의존성 설치
COPY package*.json ./
RUN npm install

# 환경 변수를 .env 파일로 생성
RUN echo "VITE_API_BASE_URL=https://j12e203.p.ssafy.io/api" > .env && \
    echo "VITE_APP_KAKAO_MAP_API_KEY=2f0c32ff649b3fdadea601325ca90f62" >> .env && \
    echo "VITE_KAKAO_CLIENT_ID=38c66a61f9b3af37dec7da2f800c1199" >> .env && \
    echo "VITE_KAKAO_REDIRECT_URI=https://j12e203.p.ssafy.io/api/v1/auth/redirect" >> .env && \
    echo "VITE_NAVER_CLIENT_ID=a4Okl4nf4Y547xepGxtu" >> .env && \
    echo "VITE_NAVER_REDIRECT_URI=https://j12e203.p.ssafy.io/api/v1/auth/redirect" >> .env && \
    echo "VITE_NAVER_STATE_STRING=HELLOTHISISAIRDEXSPEAKING" >> .env

# 소스 코드 복사 후 빌드
COPY . .
RUN npm run build

# 2단계: Production Stage - 정적 파일을 제공하기 위해 serve 사용
FROM node:22-alpine
WORKDIR /app

# 빌드 단계에서 생성된 빌드 결과물을 복사
COPY --from=builder /app/dist ./dist
# .env 파일도 복사
COPY --from=builder /app/.env ./.env

# serve 패키지를 전역으로 설치하여 정적 파일 서빙
RUN npm install -g serve
EXPOSE 5173
CMD ["serve", "-s", "dist", "-l", "5173"]