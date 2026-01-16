package org.kyh.auth.security.oauth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyh.auth.security.jwt.JwtTokenProvider;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

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

        // 해당 사용자의 JWT access token 생성
        String accessToken = jwtTokenProvider.createAccessToken(userId, role);

        log.info("OAuth2 LOGIN SUCCESS: userId={}, role={}", userId, role);

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
