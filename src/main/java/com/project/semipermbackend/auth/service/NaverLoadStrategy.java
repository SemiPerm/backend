package com.project.semipermbackend.auth.service;

import com.project.semipermbackend.auth.entity.CustomOAuth2UserInfo;
import com.project.semipermbackend.auth.entity.NaverOAuth2UserInfo;
import com.project.semipermbackend.auth.entity.SocialType;
import com.project.semipermbackend.auth.exception.LoginDisableException;
import com.project.semipermbackend.auth.jwt.JwtTokenProvider;
import com.project.semipermbackend.domain.account.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

@Slf4j
public class NaverLoadStrategy extends SocialLoadStrategy {

    @Override
    public HttpEntity<MultiValueMap<String, String>> prepareRequest(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", JwtTokenProvider.TOKEN_PREFIX + accessToken);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        return new HttpEntity<>(params, headers);
    }

    /**
     * 네이버 OpenAPI에 사용자 정보 조회 요청한다.
     */
    @Override
    protected NaverOAuth2UserInfo sendRequestToSocialApi (HttpEntity<MultiValueMap<String, String>> request) {

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(SocialType.NAVER.getUserInfoRequestUrl(),
                SocialType.NAVER.getMethod(),
                request,
                responseType);

        if (Objects.isNull(response.getBody())) {
            throw new LoginDisableException();
        }

        return makeOAuth2User((Map<String, Object>)response.getBody().get("response"));
    }

    @Override
    protected NaverOAuth2UserInfo makeOAuth2User(Map<String, Object> attributes) {
        return NaverOAuth2UserInfo.builder()
                .socialType(SocialType.NAVER)
                .socialId(attributes.get("id").toString())
                .email(attributes.get("email").toString())
                .profileImgUrl(attributes.get("profile_image").toString())
//                .nickname(attributes.get("nickname").toString())
                .build();
    }

    @Override
    public Account makeAccount(CustomOAuth2UserInfo oAuth2User) {
        return Account.builder()
                .socialId(oAuth2User.getSocialId())
                .socialType(SocialType.NAVER)
                .email(oAuth2User.getEmail())
                .profileImageUrl(oAuth2User.getProfileImgUrl())
                .build();
    }
}
