package com.drafturl.api.infra.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final String apiKey;
    private final String from;

    public EmailService(@Value("${app.resend.api-key:}") String apiKey,
                         @Value("${app.resend.from:DraftURL <noreply@drafturl.com>}") String from) {
        this.apiKey = apiKey;
        this.from = from;
    }

    public void send(String to, String subject, String html) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Resend API key not configured, skipping email: to={}, subject={}", to, subject);
            return;
        }

        String body = """
                {"from":"%s","to":["%s"],"subject":"%s","html":"%s"}"""
                .formatted(
                        escapeJson(from),
                        escapeJson(to),
                        escapeJson(subject),
                        escapeJson(html)
                );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Resend API error: status={}, body={}", response.statusCode(), response.body());
            } else {
                log.info("Email sent: to={}, subject={}", to, subject);
            }
        } catch (Exception e) {
            log.error("Failed to send email: to={}, subject={}", to, subject, e);
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
