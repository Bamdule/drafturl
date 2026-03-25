package com.drafturl.api.infra.oauth2;

import com.drafturl.api.domain.user.port.OAuth2Provider;
import com.drafturl.api.domain.user.port.OAuthTokenResponse;
import com.drafturl.api.domain.user.port.OAuthUserInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/**
 * GitHub OAuth2 Provider 구현체.
 * GitHub의 OAuth2 API를 통해 인가 코드 교환 및 사용자 정보 조회를 수행한다.
 */
@Component
public class GitHubOAuth2Provider implements OAuth2Provider {

    private static final Logger log = LoggerFactory.getLogger(GitHubOAuth2Provider.class);

    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_INFO_URL = "https://api.github.com/user";
    private static final String USER_EMAILS_URL = "https://api.github.com/user/emails";

    private final OAuth2Properties.ProviderProperties properties;
    private final RestClient restClient;

    public GitHubOAuth2Provider(OAuth2Properties oAuth2Properties, RestClient.Builder restClientBuilder) {
        this.properties = oAuth2Properties.github();
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String getProviderName() {
        return "github";
    }

    @Override
    public OAuthTokenResponse exchangeCode(String code, String redirectUri) {
        log.debug("Exchanging authorization code with GitHub");

        GitHubTokenRequest request = new GitHubTokenRequest(
                properties.clientId(),
                properties.clientSecret(),
                code,
                redirectUri
        );

        GitHubTokenResponse response;
        try {
            // GitHub은 Accept: application/json 헤더가 필요하다
            response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GitHubTokenResponse.class);
        } catch (RestClientResponseException e) {
            throw mapToOAuth2Exception("GitHub token exchange", e);
        }

        if (response == null) {
            throw new OAuth2Exception("GitHub token exchange returned null response");
        }

        if (response.error() != null) {
            throw new OAuth2Exception(
                    "GitHub token exchange failed: " + response.error() + " - " + response.errorDescription()
            );
        }

        return new OAuthTokenResponse(
                response.accessToken(),
                response.tokenType(),
                response.scope()
        );
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        log.debug("Fetching user info from GitHub");

        GitHubUserInfoResponse response;
        try {
            response = restClient.get()
                    .uri(USER_INFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GitHubUserInfoResponse.class);
        } catch (RestClientResponseException e) {
            throw mapToOAuth2Exception("GitHub user info fetch", e);
        }

        if (response == null) {
            throw new OAuth2Exception("GitHub user info returned null response");
        }

        // GitHub은 email이 비공개 설정이면 null을 반환한다.
        // 이 경우 /user/emails API를 추가 호출하여 primary email을 가져온다.
        // 그래도 없으면 noreply placeholder를 사용한다.
        String email = response.email();
        if (email == null) {
            email = fetchPrimaryEmail(accessToken);
        }
        if (email == null) {
            email = response.id() + "@users.noreply.github.com";
            log.info("GitHub email을 가져올 수 없어 placeholder 사용: {}", email);
        }

        return new OAuthUserInfo(
                String.valueOf(response.id()),
                email,
                response.name() != null ? response.name() : response.login(),
                response.avatarUrl()
        );
    }

    /**
     * GitHub /user/emails API를 호출하여 primary & verified email을 가져온다.
     * email이 없거나 API 호출 실패 시 null을 반환한다.
     */
    private String fetchPrimaryEmail(String accessToken) {
        try {
            List<GitHubEmailResponse> emails = restClient.get()
                    .uri(USER_EMAILS_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (emails == null || emails.isEmpty()) {
                log.warn("GitHub /user/emails returned empty response");
                return null;
            }

            return emails.stream()
                    .filter(e -> e.primary() && e.verified())
                    .map(GitHubEmailResponse::email)
                    .findFirst()
                    .orElseGet(() -> {
                        log.warn("GitHub /user/emails에서 primary & verified email을 찾을 수 없음");
                        return null;
                    });
        } catch (RestClientResponseException e) {
            log.warn("GitHub /user/emails API 호출 실패: status={}", e.getStatusCode(), e);
            return null;
        }
    }

    private OAuth2Exception mapToOAuth2Exception(String operation, RestClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        if (statusCode >= 500) {
            log.error("{} failed: GitHub server error, status={}", operation, statusCode, e);
            return new OAuth2Exception(HttpStatus.BAD_GATEWAY, "OAUTH2_PROVIDER_ERROR",
                    "OAuth2 제공자 서버 오류가 발생했습니다");
        }
        log.warn("{} failed: status={}, body={}", operation, statusCode, e.getResponseBodyAsString());
        return new OAuth2Exception(HttpStatus.BAD_REQUEST, "INVALID_OAUTH_CODE",
                "OAuth2 인증에 실패했습니다. 인가 코드가 유효하지 않거나 만료되었습니다.");
    }

    private record GitHubTokenRequest(
            @JsonProperty("client_id") String clientId,
            @JsonProperty("client_secret") String clientSecret,
            String code,
            @JsonProperty("redirect_uri") String redirectUri
    ) {
    }

    private record GitHubTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            String scope,
            String error,
            @JsonProperty("error_description") String errorDescription
    ) {
    }

    private record GitHubUserInfoResponse(
            long id,
            String login,
            String name,
            String email,
            @JsonProperty("avatar_url") String avatarUrl
    ) {
    }

    private record GitHubEmailResponse(
            String email,
            boolean primary,
            boolean verified
    ) {
    }
}
