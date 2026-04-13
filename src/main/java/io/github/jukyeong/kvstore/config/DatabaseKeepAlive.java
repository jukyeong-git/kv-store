package io.github.jukyeong.kvstore.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseKeepAlive {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedRate = 1800000)
    public void keepAlive() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            log.info("Database keep-alive ping successful");
        } catch (Exception e) {
            log.warn("Database keep-alive ping failed: {}", e.getMessage());
        }
    }
}