package com.anderk.ecommerce.adapter.in.web.health;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The health endpoint the deployment platform probes.
 *
 * <p>It answers without touching the database or the cache on purpose: a
 * health check that fails because a cache is down would take the whole
 * service out of rotation for a non-critical dependency.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", "xtrim-api",
                "timestamp", Instant.now().toString()));
    }
}
