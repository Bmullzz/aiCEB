package com.yourorg.eventdashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"test", "mock"})
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allTablesExistAfterMigration() {
        for (String table : new String[]{"event_category", "event", "subscription", "notification_log", "admin_user"}) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE lower(table_name) = ?",
                    Integer.class, table);
            assertThat(count).as("Table '%s' should exist", table).isEqualTo(1);
        }
    }

    @Test
    void schemaVersionIsFive() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true AND version = '5'",
                Integer.class);
        assertThat(count).as("Flyway schema version 5 should be present and successful").isEqualTo(1);
    }
}
