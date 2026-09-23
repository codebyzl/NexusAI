package io.github.codebyzl.nexus.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "MYSQL_PASSWORD=test-placeholder")
class NexusAuthApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesV1Migration() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = 'account'
                """, Integer.class);

        assertThat(tableCount).isEqualTo(1);
    }
}