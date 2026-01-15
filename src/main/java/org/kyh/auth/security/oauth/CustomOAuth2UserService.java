package org.kyh.auth.security.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyh.auth.user.domain.User;
import org.kyh.auth.user.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!"kakao".equals(registrationId)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"),
                    "Unsupported provider: " + registrationId);
        }

        Map<String, Object> attributes = oAuth2User.getAttributes();

        KakaoOAuth2UserInfo kakao = new KakaoOAuth2UserInfo(attributes);
        String providerId = kakao.getProviderId();
        String nickname = kakao.getNickname();

        if (providerId == null || providerId.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_provider_id"),
                    "Kakao providerId is null/blank");
        }

        // 1) 조회
        User user = userRepository.findByProviderAndProviderId(registrationId, providerId)
                // 2) 없으면 생성
                .orElseGet(() -> userRepository.save(
                        User.createOAuthUser(registrationId, providerId, nickname)
                ));

        // 3) 닉네임 변경됐으면 업데이트
        if (nickname != null && (user.getNickname() == null || !nickname.equals(user.getNickname()))) {
            user.updateNickname(nickname);
        }

        log.info("✅ OAuth user mapped. userId={}, providerId={}, nickname={}",
                user.getId(), providerId, nickname);

        // 지금 단계에서는 oAuth2User 그대로 반환해도 OK (세션 로그인 성공 목적)
        return oAuth2User;
    }

}
