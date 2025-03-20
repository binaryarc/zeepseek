# 1단계: Build Stage - Node.js 22를 사용하여 Vite 앱 빌드
FROM node:22-alpine AS builder
WORKDIR /app
# package.json 및 package-lock.json (또는 yarn.lock) 복사 후 의존성 설치
COPY package*.json ./
RUN npm install
# 전체 소스 복사 후 빌드 (Vite의 기본 빌드 명령어)
COPY . .
RUN npm run build

# 2단계: Production Stage - 정적 파일을 제공하기 위해 serve 사용
FROM node:22-alpine
WORKDIR /app
# 빌드 단계에서 생성된 빌드 결과물을 복사 (보통 Vite는 'dist' 폴더에 빌드 결과물을 생성합니다)
COPY --from=builder /app/dist ./dist
# serve 패키지를 전역으로 설치하여 정적 파일 서빙
RUN npm install -g serve
EXPOSE 5173
CMD ["serve", "-s", "dist", "-l", "5173"]