package org.kyh.auth.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                // 추후 JWT 인증 기반으로 전환 예정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health").permitAll()
                        .anyRequest().permitAll()
                )

                // 기본 로그인 제거
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

                // OAuth2 Login은 kakao 설정이 추가되는 feat/oauth-kakao 브랜치에서 활성화
//                .oauth2Login(Customizer.withDefaults());

        return http.build();
    }
}
