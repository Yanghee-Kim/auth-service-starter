package org.kyh.auth.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.kyh.auth.security.jwt.JwtTokenProvider;
import org.kyh.auth.security.jwt.RefreshTokenService;
import org.kyh.auth.user.domain.User;
import org.kyh.auth.user.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        String refreshToken = extractCookie(request, "refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("message", "Refresh token not found"));
        }

        Long userId = refreshTokenService.findUserIdByRefreshToken(refreshToken)
                .orElse(null);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid refresh token"));
        }

        // role은 DB에서 가져오는 게 제일 안전 (토큰에 role 넣어놨더라도 refresh 때는 DB 조회 추천)
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            // 사용자 삭제 등 예외 케이스: refresh 토큰도 폐기
            refreshTokenService.delete(refreshToken);
            return ResponseEntity.status(401).body(Map.of("message", "User not found"));
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), "ROLE_USER");

        return ResponseEntity.ok(Map.of(
                "tokenType", "Bearer",
                "accessToken", newAccessToken
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, "refreshToken");

        // Redis에서 refresh token 삭제 (있으면)
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.delete(refreshToken);
        }

        // refreshToken 쿠키 만료
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 운영(HTTPS)에서는 true
        cookie.setPath("/");
        cookie.setMaxAge(0);     // 즉시 만료
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("message", "logged out"));
    }

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) return null;

        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}

