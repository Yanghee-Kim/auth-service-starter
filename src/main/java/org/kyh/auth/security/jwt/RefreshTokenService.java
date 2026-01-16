package org.kyh.auth.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "refresh:";

    public void save(String refreshToken, Long userId, Duration ttl) {
        redisTemplate.opsForValue().set(PREFIX + refreshToken, String.valueOf(userId), ttl);
    }

    public Optional<Long> findUserIdByRefreshToken(String refreshToken) {
        String v = redisTemplate.opsForValue().get(PREFIX + refreshToken);
        if (v == null) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(v));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public void delete(String refreshToken) {
        redisTemplate.delete(PREFIX + refreshToken);
    }
}
