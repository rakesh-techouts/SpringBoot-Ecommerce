package com.techouts.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class UserRoleBackfillConfig {
    private static final Logger log = LoggerFactory.getLogger(UserRoleBackfillConfig.class);

    @Bean
    public ApplicationRunner backfillUserRole(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                int admins = jdbcTemplate.update(
                        "update users set role = 'ROLE_ADMIN' where role is null and lower(username) = 'admin'");
                int users = jdbcTemplate.update(
                        "update users set role = 'ROLE_USER' where role is null");
                if (admins > 0 || users > 0) {
                    log.info("Backfilled user roles: {} admin, {} user", admins, users);
                }
            } catch (Exception ex) {
                log.warn("Skipping user role backfill: {}", ex.getMessage());
            }
        };
    }
}

