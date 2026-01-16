package org.kyh.auth.security.oauth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyh.auth.security.jwt.JwtTokenProvider;
import org.kyh.auth.security.jwt.RefreshTokenService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.time.Duration;
import java.util.UUID;
import jakarta.servlet.http.Cookie;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        // 사용자 정보 get
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        Long userId = Long.valueOf(String.valueOf(principal.getAttributes().get("userId")));
        String role = String.valueOf(principal.getAttributes().get("role"));

        log.info("OAuth2 LOGIN SUCCESS: userId={}, role={}", userId, role);

        // JWT access token 생성
        String accessToken = jwtTokenProvider.createAccessToken(userId, role);

        // JWT refresh token 생성
        String refreshToken = UUID.randomUUID().toString();

        // Redis 저장 (예: 7일)
//        Duration refreshTtl = Duration.ofDays(7);
        Duration refreshTtl = Duration.ofMinutes(2);
        refreshTokenService.save(refreshToken, userId, refreshTtl);

        // HttpOnly Cookie 세팅
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 운영(https)에서는 true 권장
        cookie.setPath("/");     // /auth/refresh에서도 전송되게
        cookie.setMaxAge((int) refreshTtl.getSeconds());
        response.addCookie(cookie);

        // response JSON 반환
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), Map.of(
                "accessToken", accessToken,
                "tokenType", "Bearer"
        ));
    }
}
