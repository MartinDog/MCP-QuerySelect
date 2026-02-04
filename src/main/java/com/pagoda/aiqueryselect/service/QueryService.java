package com.pagoda.aiqueryselect.service;

import com.pagoda.aiqueryselect.config.DatabaseConfig;
import com.pagoda.aiqueryselect.security.QueryValidator;
import com.pagoda.aiqueryselect.security.QueryValidator.ValidationResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QueryService {

    private final JdbcTemplate jdbcTemplate;
    private final QueryValidator queryValidator;
    private final DatabaseConfig databaseConfig;

    public QueryService(JdbcTemplate jdbcTemplate, QueryValidator queryValidator, DatabaseConfig databaseConfig) {
        this.jdbcTemplate = jdbcTemplate;
        this.queryValidator = queryValidator;
        this.databaseConfig = databaseConfig;
    }

    public QueryResult executeQuery(String query) {
        return executeQuery(query, databaseConfig.getMaxRows());
    }

    public QueryResult executeQuery(String query, int maxRows) {
        ValidationResult validation = queryValidator.validate(query);

        if (!validation.valid()) {
            return QueryResult.error(validation.errorMessage());
        }

        int effectiveMaxRows = Math.min(maxRows, databaseConfig.getMaxRows());
        String limitedQuery = applyRowLimit(validation.cleanedQuery(), effectiveMaxRows);

        try {
            jdbcTemplate.setQueryTimeout(databaseConfig.getTimeoutSeconds());

            List<Map<String, Object>> results = new ArrayList<>();
            List<String> columns = new ArrayList<>();

            jdbcTemplate.query(limitedQuery, rs -> {
                if (columns.isEmpty()) {
                    var metaData = rs.getMetaData();
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        columns.add(metaData.getColumnLabel(i));
                    }
                }

                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 0; i < columns.size(); i++) {
                    row.put(columns.get(i), rs.getObject(i + 1));
                }
                results.add(row);
            });

            boolean truncated = results.size() >= effectiveMaxRows;

            return QueryResult.success(columns, results, truncated, effectiveMaxRows);
        } catch (Exception e) {
            return QueryResult.error("Query execution failed: " + e.getMessage());
        }
    }

    private String applyRowLimit(String query, int maxRows) {
        String upperQuery = query.toUpperCase().trim();

        // Check if query already has FETCH or ROWNUM limit
        if (upperQuery.contains("FETCH FIRST") || upperQuery.contains("FETCH NEXT") ||
                upperQuery.contains("ROWNUM")) {
            return query;
        }

        // Add FETCH FIRST N ROWS ONLY (Oracle 12c+ syntax)
        return query + " FETCH FIRST " + maxRows + " ROWS ONLY";
    }

    public record QueryResult(
            boolean success,
            List<String> columns,
            List<Map<String, Object>> rows,
            String errorMessage,
            boolean truncated,
            int maxRows
    ) {
        public static QueryResult success(List<String> columns, List<Map<String, Object>> rows,
                                          boolean truncated, int maxRows) {
            return new QueryResult(true, columns, rows, null, truncated, maxRows);
        }

        public static QueryResult error(String errorMessage) {
            return new QueryResult(false, List.of(), List.of(), errorMessage, false, 0);
        }

        public int rowCount() {
            return rows.size();
        }
    }
}
