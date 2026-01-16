package org.kyh.auth.security.config;

import lombok.RequiredArgsConstructor;
import org.kyh.auth.security.jwt.JwtAuthenticationFilter;
import org.kyh.auth.security.jwt.RestAuthenticationEntryPoint;
import org.kyh.auth.security.oauth.CustomOAuth2UserService;
import org.kyh.auth.security.oauth.handler.OAuth2FailureHandler;
import org.kyh.auth.security.oauth.handler.OAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    /**
     * API 전용 체인: JWT만 허용 (세션 사용 X)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**") // 이 체인은 /api/**만 적용

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 X

                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint))

                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * OAuth2 로그인 + 나머지 경로 체인
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                // OAuth2 로그인 과정에서는 세션이 필요할 수 있어 IF_REQUIRED 유지
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // OAuth 로그인 관련 URL은 인증 없이 허용, 그 외 요청은 모두 허용
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login/**", "/oauth2/**", "/error").permitAll()
                        .anyRequest().permitAll()
                )

                // 기본 HTTP Basic / Form 로그인 미사용 (OAuth + JWT만 사용)
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())

                // Oauth2 login 설정
                // - 사용자 정보 로딩: CustomOAuth2UserService
                // - 성공 후 처리(JWT 발급): OAuth2SuccessHandler
                // - 실패 처리: OAuth2FailureHandler
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                );

        return http.build();
    }
}

