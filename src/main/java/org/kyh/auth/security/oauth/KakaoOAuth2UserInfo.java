package org.kyh.auth.security.oauth;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class KakaoOAuth2UserInfo {

    private final Map<String, Object> attributes;

    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    /** 닉네임 (properties → kakao_account 순서로 조회) */
    @SuppressWarnings("unchecked")
    public String getNickname() {

        // 1. properties.nickname
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        if (properties != null && properties.get("nickname") != null) {
            return String.valueOf(properties.get("nickname"));
        }

        // 2. kakao_account.profile.nickname
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null) return null;

        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        if (profile == null) return null;

        return String.valueOf(profile.get("nickname"));
    }
}
