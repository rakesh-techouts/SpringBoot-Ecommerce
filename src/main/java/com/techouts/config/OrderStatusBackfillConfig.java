package com.techouts.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class OrderStatusBackfillConfig {
    private static final Logger log = LoggerFactory.getLogger(OrderStatusBackfillConfig.class);

    @Bean
    public ApplicationRunner backfillOrderStatus(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                int updated = jdbcTemplate.update("update orders set status = 'PLACED' where status is null");
                if (updated > 0) {
                    log.info("Backfilled {} order rows with default status PLACED", updated);
                }
            } catch (Exception ex) {
                log.warn("Skipping order status backfill: {}", ex.getMessage());
            }
        };
    }
}
