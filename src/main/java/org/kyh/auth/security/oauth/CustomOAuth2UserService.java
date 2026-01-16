package org.kyh.auth.security.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyh.auth.user.domain.User;
import org.kyh.auth.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        // 카카오에서 내려준 정보
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // kakao 인지 확인
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!"kakao".equals(registrationId)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"), "Unsupported provider: " + registrationId);
        }

        // providerId와 nickname 추출
        Map<String, Object> attributes = oAuth2User.getAttributes();
        KakaoOAuth2UserInfo kakao = new KakaoOAuth2UserInfo(attributes);
        String providerId = kakao.getProviderId();
        String nickname = kakao.getNickname();

        if (providerId == null || providerId.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_provider_id"), "Kakao providerId is null/blank");
        }

        // 내 DB에서 사용자 조회
        User user = userRepository.findByProviderAndProviderId(registrationId, providerId)
                .orElseGet(() -> userRepository.save(   // 없으면 생성
                        User.createOAuthUser(registrationId, providerId, nickname)
                ));

        log.info("✅ OAuth user mapped. userId={}, providerId={}, nickname={}", user.getId(), providerId, nickname);

        Map<String, Object> enriched = new HashMap<>(attributes);
        enriched.put("userId", user.getId());
        enriched.put("role", "ROLE_USER");

        // 카카오 원본 attributes에 우리 서비스에서 필요한 식별자/권한을 추가로 주입
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                enriched,
                "id" // 카카오 user-name-attribute가 id라서 그대로
        );
    }

}
