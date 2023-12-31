package com.project.semipermbackend.auth.service;

import com.project.semipermbackend.auth.entity.CustomOAuth2UserInfo;
import com.project.semipermbackend.auth.entity.KakaoOAuth2UserInfo;
import com.project.semipermbackend.auth.entity.SocialType;
import com.project.semipermbackend.auth.jwt.JwtTokenProvider;
import com.project.semipermbackend.domain.account.Account;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

/**
 * response ex :
 * {
 *   "aud": "${APP_KEY}",
 *   "sub": "${USER_ID}",
 *   "auth_time": 1661967952,
 *   "iss": "https://kauth.kakao.com",
 *   "exp": 1661967972,
 *   "iat": 1661967952,
 *   "nickname": "JordyTest",
 *   "picture": "http://yyy.kakao.com/.../img_110x110.jpg",
 *   "email": "jordy@kakao.com"
 * }
 */
public class KakaoLoadStrategy extends SocialLoadStrategy {

    @Override
    public HttpEntity<MultiValueMap<String, String>> prepareRequest(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", JwtTokenProvider.TOKEN_PREFIX + accessToken);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        return new HttpEntity<>(params, headers);
    }

    @Override
    protected KakaoOAuth2UserInfo sendRequestToSocialApi (HttpEntity<MultiValueMap<String, String>> request) {
        ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(SocialType.KAKAO.getUserInfoRequestUrl(),
                SocialType.KAKAO.getMethod(),
                request,
                responseType);
        return makeOAuth2User(responseEntity.getBody());
    }


    @Override
    protected KakaoOAuth2UserInfo makeOAuth2User(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccounts = (Map<String, Object>)attributes.get("kakao_account");

        Map<String, Object> profile =  (Map<String, Object>)kakaoAccounts.get("profile");
        String email = kakaoAccounts.get("email").toString();

        return KakaoOAuth2UserInfo.builder()
                .socialType(SocialType.KAKAO)
                .socialId(attributes.get("id").toString())
                .email(email)
                .profileImgUrl(profile.get("thumbnail_image_url").toString())
//                .nickname(profile.get("nickname").toString())
                .build();
    }

    @Override
    public Account makeAccount(CustomOAuth2UserInfo oAuth2User) {
        return Account.builder()
                .socialId(oAuth2User.getSocialId())
                .socialType(SocialType.KAKAO)
                .email(oAuth2User.getEmail())
                .profileImageUrl(oAuth2User.getProfileImgUrl())
                .build();
    }
}
