# auth-service-starter

카카오 OAuth2 로그인을 기반으로 자체 JWT 인증(Access / Refresh)을 구현  
로그인 성공 시 Access Token(JWT)을 발급하고, Refresh Token은 Redis에 저장하여 재발급 및 로그아웃을 관리

---

## 1. 인프라 및 배포 구성
- EC2 (Ubuntu) 단일 서버
- Docker Compose 활용한 멀티 컨테이너 구성
  - Spring Boot App
  - MySQL
  - Redis
- CI: GitHub Actions
  - Docker 이미지 빌드
  - main 브랜치 기준 GHCR 이미지 push
- CD: Jenkins (http://54.116.58.222:8081) (Docker 컨테이너로 구동)
  - GHCR 이미지 pull
  - 컨테이너 재기동을 통한 배포

---

## 2. 기능

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

## 3. 처리 흐름

### 1) Kakao OAuth2 로그인
- SecurityConfig
  - OAuth2 로그인 및 성공/실패 핸들러 설정
- CustomOAuth2UserService
  - 인가 코드 기반 카카오 사용자 정보 조회
  - KakaoOAuth2UserInfo를 통해 응답 데이터 파싱
  - UserRepository로 사용자 조회/저장 후 OAuth2User 반환
- Oauth2SuccessHandler
  - 인증 성공 시 JWT 발급
- JwtTokenProvider
  - Access Token 생성
- RefreshTokenService
  - Refresh Token 생성 및 Redis 저장 (HttpOnly Cookie 설정)

### 2) 인증 API 접근 (/api/**)
- JwtAuthenticationFilter
  - Authorization 헤더의 Access Token 검증
- JwtTokenProvider
  - 토큰 유효성 검사 및 사용자 정보 추출
- SecurityContext에 인증 정보 저장 후 컨트롤러 접근
- 실패 시 RestAuthenticationEntryPoint에서 401 응답

### 3) Access Token 재발급 (/auth/refresh)
- AuthController
  - refresh 요청 처리
- RefreshTokenService
  - Redis에 저장된 Refresh Token 검증
- JwtTokenProvider
  - 새로운 Access Token 발급

### 4) 로그아웃 (/auth/logout)
- AuthController
  - 로그아웃 요청 처리
- RefreshTokenService
  - Refresh Token 삭제 및 쿠키 무효화

---

## 4. 테스트
### 1) 카카오 로그인
브라우저에서 접속: http://54.116.58.222:8080/oauth2/authorization/kakao
로그인 성공 시:
- Access Token 발급
- Refresh Token이 HttpOnly Cookie로 설정됨
- response
  {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
  }

### 2) 인증 API 호출
Postman
- GET http://54.116.58.222:8080/api/me
- Authorization: Bearer {ACCESS_TOKEN}
- response
  {
    "principal": "1",
    "authorities": [
      {
        "authority": "ROLE_USER"
      }
    ]
  }

### 3) Access Token 재발급
Postman
- http://54.116.58.222:8080/auth/refresh
- Cookie: refreshToken={REFRESH_TOKEN}
- response
  {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
  }

### 4) 로그아웃
Postman
- http://54.116.58.222:8080/auth/logout
- Cookie: refreshToken={REFRESH_TOKEN}
- response
  {
    "message": "logged out"
  }

  
