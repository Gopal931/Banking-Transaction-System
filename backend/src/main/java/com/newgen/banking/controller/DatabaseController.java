
package com.newgen.banking.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DatabaseController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/v1/health/database")
    public Map<String, Object> checkDatabase() {

        String database = jdbcTemplate.queryForObject(
            "SELECT current_database()",
            String.class
        );

        return Map.of(
            "status", "CONNECTED",
            "database", database
        );
    }
}
