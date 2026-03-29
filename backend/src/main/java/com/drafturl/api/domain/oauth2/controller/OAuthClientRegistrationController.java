package com.drafturl.api.domain.oauth2.controller;

import com.drafturl.api.domain.oauth2.ClientRegistrationStore;
import com.drafturl.api.domain.oauth2.RegisteredClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class OAuthClientRegistrationController {

    private final ClientRegistrationStore clientRegistrationStore;

    public OAuthClientRegistrationController(ClientRegistrationStore clientRegistrationStore) {
        this.clientRegistrationStore = clientRegistrationStore;
    }

    @PostMapping(value = "/oauth2/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> request) {

        // redirect_uris 추출
        @SuppressWarnings("unchecked")
        List<String> redirectUris = (List<String>) request.get("redirect_uris");

        // token_endpoint_auth_method 검증
        String authMethod = (String) request.get("token_endpoint_auth_method");
        if (authMethod != null && !"none".equals(authMethod)) {
            return errorResponse("invalid_client_metadata",
                    "Only token_endpoint_auth_method 'none' is supported");
        }

        // grant_types 검증
        @SuppressWarnings("unchecked")
        List<String> grantTypes = (List<String>) request.get("grant_types");
        if (grantTypes != null && !grantTypes.contains("authorization_code")) {
            return errorResponse("invalid_client_metadata",
                    "grant_types must include 'authorization_code'");
        }

        // client_name 추출
        String clientName = (String) request.get("client_name");

        // 등록
        RegisteredClient client = clientRegistrationStore.register(redirectUris, clientName);

        // RFC 7591 응답
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("client_id", client.clientId());
        response.put("client_name", client.clientName());
        response.put("redirect_uris", client.redirectUris());
        response.put("token_endpoint_auth_method", "none");
        response.put("grant_types", grantTypes != null ? grantTypes : List.of("authorization_code"));
        response.put("response_types", List.of("code"));
        response.put("client_id_issued_at", client.createdAt().getEpochSecond());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ExceptionHandler(ClientRegistrationStore.InvalidRegistrationException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRegistration(
            ClientRegistrationStore.InvalidRegistrationException e) {
        return errorResponse(e.getError(), e.getErrorDescription());
    }

    private ResponseEntity<Map<String, Object>> errorResponse(String error, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", description);
        return ResponseEntity.badRequest().body(body);
    }
}
