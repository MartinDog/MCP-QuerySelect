package com.pagoda.aiqueryselect.config;

import com.pagoda.aiqueryselect.utils.VersionComparator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StartSetting implements CommandLineRunner {
    public StartSetting(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate=jdbcTemplate;
    }
    private final JdbcTemplate jdbcTemplate;
    @Override
    public void run(String... args) throws Exception {
        String sql = """
            SELECT
                VERSION
            FROM
                PRODUCT_COMPONENT_VERSION
            WHERE PRODUCT LIKE 'Oracle Database%'
            """;
        String version = jdbcTemplate.queryForObject(sql,String.class);
        if (version!=null&&!version.isEmpty() ){
            ConfigValue.oracleVersion = version;
            ConfigValue.isOver12 = VersionComparator.isLatestVersion(version,"12");
        }
    }
}
