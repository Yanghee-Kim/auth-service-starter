# auth-service-starter

카카오 OAuth2 로그인을 기반으로 자체 JWT 인증(Access / Refresh)을 구현  
로그인 성공 시 Access Token(JWT)을 발급하고, Refresh Token은 Redis에 저장하여 재발급 및 로그아웃을 관리

---

## 1. 기능

### 인증
- GET /oauth2/authorization/kakao
  - 카카오 로그인 시작(표준 OAuth2 엔드포인트)
- 로그인 성공 시
  - Access Token: JWT 발급 (API 인증용)
  - Refresh Token: Redis 저장 + HttpOnly Cookie 전달

### 토큰 관리
- POST /auth/refresh
  - Cookie 기반 Refresh Token으로 Access Token 재발급
- POST /auth/logout
  - Redis에 저장된 Refresh Token 삭제 + 쿠키 제거

### 보호 API
- GET /api/me
  - Access Token 인증 필요

---

## 2. 흐름

1. 클라이언트가 /oauth2/authorization/kakao 요청
2. 카카오 로그인 및 사용자 동의
3. 인가 코드 발급 후 서버로 리다이렉트
4. 서버에서 사용자 정보 조회
5. Access Token(JWT) + Refresh Token 발급
6. Access Token으로 보호 API 접근
7. 만료 시 Refresh Token으로 재발급
8. 로그아웃 시 Refresh Token 폐기

---

## 3. 인프라 및 배포 구성
- EC2 (Ubuntu) 단일 서버
- Docker 기반 컨테이너 운영
  - Spring Boot App
  - MySQL
  - Redis
- CI: GitHub Actions
  - Docker 이미지 빌드
  - main 브랜치 기준 GHCR 이미지 push
- CD: Jenkins
  - GHCR 이미지 pull
  - 컨테이너 재기동을 통한 배포

---

## 5. 테스트
### 1) 카카오 로그인
브라우저에서 접속: http://54.116.58.222:8080/oauth2/authorization/kakao
로그인 성공 시:
- Access Token 발급
- Refresh Token이 HttpOnly Cookie로 설정됨

### 2) 인증 API 호출
curl -i http://{EC2_IP}:8080/api/me \
  -H "Authorization: Bearer {ACCESS_TOKEN}"

### 3) Access Token 재발급
curl -i -X POST http://{EC2_IP}:8080/auth/refresh \
  -H "Cookie: refreshToken={REFRESH_TOKEN}"

### 4) 로그아웃
curl -i -X POST http://{EC2_IP}:8080/auth/logout \
  -H "Cookie: refreshToken={REFRESH_TOKEN}"
