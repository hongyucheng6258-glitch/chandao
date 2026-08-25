package com.pms.security;

import com.pms.common.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigurationTest {

    @Test
    void applicationConfigMustNotContainCommittedSecrets() throws Exception {
        String config = Files.readString(
                Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);

        assertTrue(config.contains("password: ${PMS_DB_PASSWORD:"));
        assertTrue(config.contains("secret: ${PMS_JWT_SECRET:"));
        assertFalse(config.contains("allow-circular-references: true"));
    }

    @Test
    void corsMustNotAllowEveryOriginWithCredentials() {
        SecurityConfig securityConfig = new SecurityConfig(null);
        ReflectionTestUtils.setField(securityConfig, "allowedOriginsConfig", "http://localhost:5173");
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Origin", "http://localhost:5173");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertNotNull(config);
        assertNotNull(config.getAllowedOrigins());
        assertFalse(config.getAllowedOrigins().contains("*"));
        assertTrue(config.getAllowedMethods().stream().allMatch(method ->
                method.equals("GET") || method.equals("POST") || method.equals("PUT") || method.equals("DELETE") || method.equals("OPTIONS")));
    }
}
