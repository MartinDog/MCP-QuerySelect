package com.pagoda.aiqueryselect;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class AiQuerySelectApplicationTests {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Test
    void contextLoads() {
        // Given
        String sql = "SELECT 1 FROM DUAL";
        // When
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class);
        // Then
        assertThat(result).isEqualTo(1);
    }

}
