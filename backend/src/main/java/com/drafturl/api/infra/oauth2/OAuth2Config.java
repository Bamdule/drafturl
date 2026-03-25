package com.drafturl.api.infra.oauth2;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OAuth2 관련 설정.
 * OAuth2Properties를 활성화하여 application.yml의 app.oauth2 설정을 바인딩한다.
 */
@Configuration
@EnableConfigurationProperties(OAuth2Properties.class)
public class OAuth2Config {
}
