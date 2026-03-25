package com.drafturl.api.infra.oauth2;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth2 Provider 설정 프로퍼티.
 * application.yml의 app.oauth2 프리픽스에 바인딩된다.
 *
 * @param google Google OAuth2 설정
 * @param github GitHub OAuth2 설정
 */
@ConfigurationProperties(prefix = "app.oauth2")
public record OAuth2Properties(
        ProviderProperties google,
        ProviderProperties github
) {

    /**
     * 개별 OAuth2 Provider 설정.
     *
     * @param clientId     클라이언트 ID
     * @param clientSecret 클라이언트 시크릿
     */
    public record ProviderProperties(
            String clientId,
            String clientSecret
    ) {
    }
}
