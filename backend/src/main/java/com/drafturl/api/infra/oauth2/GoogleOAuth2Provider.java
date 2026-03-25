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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Google OAuth2 Provider 구현체.
 * Google의 OAuth2 API를 통해 인가 코드 교환 및 사용자 정보 조회를 수행한다.
 */
@Component
public class GoogleOAuth2Provider implements OAuth2Provider {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuth2Provider.class);

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final OAuth2Properties.ProviderProperties properties;
    private final RestClient restClient;

    public GoogleOAuth2Provider(OAuth2Properties oAuth2Properties, RestClient.Builder restClientBuilder) {
        this.properties = oAuth2Properties.google();
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String getProviderName() {
        return "google";
    }

    @Override
    public OAuthTokenResponse exchangeCode(String code, String redirectUri) {
        log.debug("Exchanging authorization code with Google");

        GoogleTokenRequest request = new GoogleTokenRequest(
                code,
                properties.clientId(),
                properties.clientSecret(),
                redirectUri,
                "authorization_code"
        );

        GoogleTokenResponse response;
        try {
            response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
        } catch (RestClientResponseException e) {
            throw mapToOAuth2Exception("Google token exchange", e);
        }

        if (response == null) {
            throw new OAuth2Exception("Google token exchange returned null response");
        }

        return new OAuthTokenResponse(
                response.accessToken(),
                response.tokenType(),
                response.scope()
        );
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        log.debug("Fetching user info from Google");

        GoogleUserInfoResponse response;
        try {
            response = restClient.get()
                    .uri(USER_INFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleUserInfoResponse.class);
        } catch (RestClientResponseException e) {
            throw mapToOAuth2Exception("Google user info fetch", e);
        }

        if (response == null) {
            throw new OAuth2Exception("Google user info returned null response");
        }

        return new OAuthUserInfo(
                response.id(),
                response.email(),
                response.name(),
                response.picture()
        );
    }

    private OAuth2Exception mapToOAuth2Exception(String operation, RestClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        if (statusCode >= 500) {
            log.error("{} failed: Google server error, status={}", operation, statusCode, e);
            return new OAuth2Exception(HttpStatus.BAD_GATEWAY, "OAUTH2_PROVIDER_ERROR",
                    "OAuth2 제공자 서버 오류가 발생했습니다");
        }
        log.warn("{} failed: status={}, body={}", operation, statusCode, e.getResponseBodyAsString());
        return new OAuth2Exception(HttpStatus.BAD_REQUEST, "INVALID_OAUTH_CODE",
                "OAuth2 인증에 실패했습니다. 인가 코드가 유효하지 않거나 만료되었습니다.");
    }

    private record GoogleTokenRequest(
            String code,
            @JsonProperty("client_id") String clientId,
            @JsonProperty("client_secret") String clientSecret,
            @JsonProperty("redirect_uri") String redirectUri,
            @JsonProperty("grant_type") String grantType
    ) {
    }

    private record GoogleTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            String scope
    ) {
    }

    private record GoogleUserInfoResponse(
            String id,
            String email,
            String name,
            String picture
    ) {
    }
}
