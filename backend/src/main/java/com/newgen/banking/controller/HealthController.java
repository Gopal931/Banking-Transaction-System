package com.newgen.banking.controller;

import com.newgen.banking.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<HealthResponse> getHealthStatus() {
        HealthResponse response = new HealthResponse(
                "UP",
                "Banking backend is running",
                "Banking Transaction Processing System"
        );
        return ResponseEntity.ok(response);
    }
}
