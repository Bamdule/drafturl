package com.drafturl.api.infra.oauth2;

import com.drafturl.api.domain.user.port.OAuth2Provider;
import com.drafturl.api.domain.user.port.OAuthTokenResponse;
import com.drafturl.api.domain.user.port.OAuthUserInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Kakao OAuth2 Provider 구현체.
 * 카카오의 OAuth2 API를 통해 인가 코드 교환 및 사용자 정보 조회를 수행한다.
 */
@Component
public class KakaoOAuth2Provider implements OAuth2Provider {

    private static final Logger log = LoggerFactory.getLogger(KakaoOAuth2Provider.class);

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final OAuth2Properties.ProviderProperties properties;
    private final RestClient restClient;

    public KakaoOAuth2Provider(OAuth2Properties oAuth2Properties, RestClient.Builder restClientBuilder) {
        this.properties = oAuth2Properties.kakao();
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String getProviderName() {
        return "kakao";
    }

    @Override
    public OAuthTokenResponse exchangeCode(String code, String redirectUri) {
        log.debug("Exchanging authorization code with Kakao");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", properties.clientId());
        formData.add("client_secret", properties.clientSecret());
        formData.add("redirect_uri", redirectUri);
        formData.add("code", code);

        KakaoTokenResponse response;
        try {
            response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
        } catch (RestClientResponseException e) {
            throw mapToOAuth2Exception("Kakao token exchange", e);
        }

        if (response == null) {
            throw new OAuth2Exception("Kakao token exchange returned null response");
        }

        return new OAuthTokenResponse(
                response.accessToken(),
                response.tokenType(),
                response.scope()
        );
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        log.debug("Fetching user info from Kakao");

        KakaoUserInfoResponse response;
        try {
            response = restClient.get()
                    .uri(USER_INFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);
        } catch (RestClientResponseException e) {
            throw mapToOAuth2Exception("Kakao user info fetch", e);
        }

        if (response == null) {
            throw new OAuth2Exception("Kakao user info returned null response");
        }

        KakaoAccount account = response.kakaoAccount();
        String email = account != null ? account.email() : null;
        String name = null;
        String profileImage = null;

        if (account != null && account.profile() != null) {
            name = account.profile().nickname();
            profileImage = account.profile().profileImageUrl();
        }

        return new OAuthUserInfo(
                String.valueOf(response.id()),
                email,
                name,
                profileImage
        );
    }

    private OAuth2Exception mapToOAuth2Exception(String operation, RestClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        if (statusCode >= 500) {
            log.error("{} failed: Kakao server error, status={}", operation, statusCode, e);
            return new OAuth2Exception(HttpStatus.BAD_GATEWAY, "OAUTH2_PROVIDER_ERROR",
                    "OAuth2 제공자 서버 오류가 발생했습니다");
        }
        log.warn("{} failed: status={}, body={}", operation, statusCode, e.getResponseBodyAsString());
        return new OAuth2Exception(HttpStatus.BAD_REQUEST, "INVALID_OAUTH_CODE",
                "OAuth2 인증에 실패했습니다. 인가 코드가 유효하지 않거나 만료되었습니다.");
    }

    private record KakaoTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            String scope
    ) {}

    private record KakaoUserInfoResponse(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {}

    private record KakaoAccount(
            String email,
            KakaoProfile profile
    ) {}

    private record KakaoProfile(
            String nickname,
            @JsonProperty("profile_image_url") String profileImageUrl
    ) {}
}
