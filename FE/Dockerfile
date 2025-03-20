# 1단계: Build Stage - Node.js를 사용하여 Vite 앱 빌드
FROM node:16-alpine AS builder
WORKDIR /app
# package.json 및 package-lock.json (또는 yarn.lock) 복사 후 의존성 설치
COPY package*.json ./
RUN npm install
# 전체 소스 복사 후 빌드 (Vite의 기본 빌드 명령어)
COPY . .
RUN npm run build

# 2단계: Production Stage - 정적 파일을 제공하기 위해 lightweight Node.js static server 사용
FROM node:16-alpine
WORKDIR /app
# Build 단계에서 생성된 빌드 결과물을 복사
COPY --from=builder /app/dist ./dist
# 'serve' 패키지를 전역 설치하여 정적 파일 서빙
RUN npm install -g serve
EXPOSE 5173
CMD ["serve", "-s", "dist", "-l", "5173"]