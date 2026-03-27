package com.drafturl.api.global.controller;

import com.drafturl.api.global.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/v1/health")
    public ApiResponse<Map<String, String>> health() {
        String dbStatus;
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            dbStatus = "ok";
        } catch (Exception e) {
            dbStatus = "error";
        }
        return ApiResponse.success(Map.of("status", "ok", "database", dbStatus));
    }
}
