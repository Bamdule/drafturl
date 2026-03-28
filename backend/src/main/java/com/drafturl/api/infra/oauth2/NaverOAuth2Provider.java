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
 * Naver OAuth2 Provider 구현체.
 * 네이버의 OAuth2 API를 통해 인가 코드 교환 및 사용자 정보 조회를 수행한다.
 */
@Component
public class NaverOAuth2Provider implements OAuth2Provider {

    private static final Logger log = LoggerFactory.getLogger(NaverOAuth2Provider.class);

    private static final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    private final OAuth2Properties.ProviderProperties properties;
    private final RestClient restClient;

    public NaverOAuth2Provider(OAuth2Properties oAuth2Properties, RestClient.Builder restClientBuilder) {
        this.properties = oAuth2Properties.naver();
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String getProviderName() {
        return "naver";
    }

    @Override
    public OAuthTokenResponse exchangeCode(String code, String redirectUri) {
        log.debug("Exchanging authorization code with Naver");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", properties.clientId());
        formData.add("client_secret", properties.clientSecret());
        formData.add("code", code);

        NaverTokenResponse response;
        try {
            response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(NaverTokenResponse.class);
        } catch (RestClientResponseException e) {
            throw mapToOAuth2Exception("Naver token exchange", e);
        }

        if (response == null) {
            throw new OAuth2Exception("Naver token exchange returned null response");
        }

        if (response.error() != null) {
            log.warn("Naver token exchange error: {} - {}", response.error(), response.errorDescription());
            throw new OAuth2Exception(HttpStatus.BAD_REQUEST, "INVALID_OAUTH_CODE",
                    "네이버 OAuth2 인증에 실패했습니다: " + response.errorDescription());
        }

        return new OAuthTokenResponse(
                response.accessToken(),
                response.tokenType(),
                null
        );
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        log.debug("Fetching user info from Naver");

        NaverUserInfoWrapper response;
        try {
            response = restClient.get()
                    .uri(USER_INFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(NaverUserInfoWrapper.class);
        } catch (RestClientResponseException e) {
            throw mapToOAuth2Exception("Naver user info fetch", e);
        }

        if (response == null || response.response() == null) {
            throw new OAuth2Exception("Naver user info returned null response");
        }

        NaverUserInfo userInfo = response.response();
        return new OAuthUserInfo(
                userInfo.id(),
                userInfo.email(),
                userInfo.name(),
                userInfo.profileImage()
        );
    }

    private OAuth2Exception mapToOAuth2Exception(String operation, RestClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        if (statusCode >= 500) {
            log.error("{} failed: Naver server error, status={}", operation, statusCode, e);
            return new OAuth2Exception(HttpStatus.BAD_GATEWAY, "OAUTH2_PROVIDER_ERROR",
                    "OAuth2 제공자 서버 오류가 발생했습니다");
        }
        log.warn("{} failed: status={}, body={}", operation, statusCode, e.getResponseBodyAsString());
        return new OAuth2Exception(HttpStatus.BAD_REQUEST, "INVALID_OAUTH_CODE",
                "OAuth2 인증에 실패했습니다. 인가 코드가 유효하지 않거나 만료되었습니다.");
    }

    private record NaverTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            String error,
            @JsonProperty("error_description") String errorDescription
    ) {}

    private record NaverUserInfoWrapper(
            String resultcode,
            String message,
            NaverUserInfo response
    ) {}

    private record NaverUserInfo(
            String id,
            String email,
            String name,
            @JsonProperty("profile_image") String profileImage
    ) {}
}
