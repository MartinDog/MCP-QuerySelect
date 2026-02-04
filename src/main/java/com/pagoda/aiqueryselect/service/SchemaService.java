package com.pagoda.aiqueryselect.service;

import com.pagoda.aiqueryselect.model.ColumnInfo;
import com.pagoda.aiqueryselect.model.ConstraintInfo;
import com.pagoda.aiqueryselect.model.ForeignKeyInfo;
import com.pagoda.aiqueryselect.model.TableInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SchemaService {

    private static final Set<String> EXCLUDED_SCHEMAS = Set.of(
            "SYS", "SYSTEM", "OUTLN", "DIP", "ORACLE_OCM", "DBSNMP", "APPQOSSYS",
            "WMSYS", "EXFSYS", "CTXSYS", "XDB", "ANONYMOUS", "ORDSYS", "ORDDATA",
            "ORDPLUGINS", "SI_INFORMTN_SCHEMA", "MDSYS", "OLAPSYS", "MDDATA",
            "SPATIAL_WFS_ADMIN_USR", "SPATIAL_CSW_ADMIN_USR", "LBACSYS", "DVSYS",
            "DVF", "GSMADMIN_INTERNAL", "GSMCATUSER", "GSMUSER", "AUDSYS",
            "DBSFWUSER", "REMOTE_SCHEDULER_AGENT", "SYSBACKUP", "SYSDG", "SYSKM",
            "SYSRAC", "OJVMSYS", "APEX_PUBLIC_USER", "APEX_040000", "APEX_050000",
            "FLOWS_FILES", "ORDS_PUBLIC_USER", "ORDS_METADATA"
    );

    private final JdbcTemplate jdbcTemplate;

    public SchemaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TableInfo> listAllTables() {
        String sql = """
                SELECT t.OWNER, t.TABLE_NAME, tc.COMMENTS, t.NUM_ROWS
                FROM ALL_TABLES t
                LEFT JOIN ALL_TAB_COMMENTS tc ON t.OWNER = tc.OWNER AND t.TABLE_NAME = tc.TABLE_NAME
                WHERE t.OWNER NOT IN (%s)
                  AND t.TEMPORARY = 'N'
                  AND t.SECONDARY = 'N'
                ORDER BY t.OWNER, t.TABLE_NAME
                """.formatted(buildExcludedSchemasList());

        return jdbcTemplate.query(sql, (rs, rowNum) -> new TableInfo(
                rs.getString("OWNER"),
                rs.getString("TABLE_NAME"),
                rs.getString("COMMENTS"),
                rs.getObject("NUM_ROWS") != null ? rs.getLong("NUM_ROWS") : null
        ));
    }

    public List<ColumnInfo> getTableColumns(String owner, String tableName) {
        String sql = """
                SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.DATA_PRECISION,
                       c.DATA_SCALE, c.NULLABLE, c.DATA_DEFAULT, cc.COMMENTS, c.COLUMN_ID
                FROM ALL_TAB_COLUMNS c
                LEFT JOIN ALL_COL_COMMENTS cc
                    ON c.OWNER = cc.OWNER AND c.TABLE_NAME = cc.TABLE_NAME AND c.COLUMN_NAME = cc.COLUMN_NAME
                WHERE c.OWNER = ? AND c.TABLE_NAME = ?
                ORDER BY c.COLUMN_ID
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ColumnInfo(
                rs.getString("COLUMN_NAME"),
                rs.getString("DATA_TYPE"),
                rs.getObject("DATA_LENGTH") != null ? rs.getInt("DATA_LENGTH") : null,
                rs.getObject("DATA_PRECISION") != null ? rs.getInt("DATA_PRECISION") : null,
                rs.getObject("DATA_SCALE") != null ? rs.getInt("DATA_SCALE") : null,
                "Y".equals(rs.getString("NULLABLE")),
                rs.getString("DATA_DEFAULT"),
                rs.getString("COMMENTS"),
                rs.getInt("COLUMN_ID")
        ), owner.toUpperCase(), tableName.toUpperCase());
    }

    public List<ConstraintInfo> getTableConstraints(String owner, String tableName) {
        String sql = """
                SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.SEARCH_CONDITION,
                       LISTAGG(cc.COLUMN_NAME, ', ') WITHIN GROUP (ORDER BY cc.POSITION) AS COLUMNS
                FROM ALL_CONSTRAINTS c
                LEFT JOIN ALL_CONS_COLUMNS cc
                    ON c.OWNER = cc.OWNER AND c.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
                WHERE c.OWNER = ? AND c.TABLE_NAME = ?
                  AND c.CONSTRAINT_TYPE IN ('P', 'U', 'C')
                GROUP BY c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.SEARCH_CONDITION
                ORDER BY c.CONSTRAINT_TYPE, c.CONSTRAINT_NAME
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String columnsStr = rs.getString("COLUMNS");
            List<String> columns = columnsStr != null ? List.of(columnsStr.split(", ")) : List.of();
            return new ConstraintInfo(
                    rs.getString("CONSTRAINT_NAME"),
                    rs.getString("CONSTRAINT_TYPE"),
                    columns,
                    rs.getString("SEARCH_CONDITION")
            );
        }, owner.toUpperCase(), tableName.toUpperCase());
    }

    public List<ForeignKeyInfo> getTableForeignKeys(String owner, String tableName) {
        String sql = """
                SELECT c.CONSTRAINT_NAME, c.DELETE_RULE,
                       c.R_OWNER, rc.TABLE_NAME AS R_TABLE_NAME,
                       LISTAGG(cc.COLUMN_NAME, ', ') WITHIN GROUP (ORDER BY cc.POSITION) AS SOURCE_COLUMNS,
                       LISTAGG(rcc.COLUMN_NAME, ', ') WITHIN GROUP (ORDER BY rcc.POSITION) AS TARGET_COLUMNS
                FROM ALL_CONSTRAINTS c
                JOIN ALL_CONS_COLUMNS cc
                    ON c.OWNER = cc.OWNER AND c.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
                JOIN ALL_CONSTRAINTS rc
                    ON c.R_OWNER = rc.OWNER AND c.R_CONSTRAINT_NAME = rc.CONSTRAINT_NAME
                JOIN ALL_CONS_COLUMNS rcc
                    ON rc.OWNER = rcc.OWNER AND rc.CONSTRAINT_NAME = rcc.CONSTRAINT_NAME
                WHERE c.OWNER = ? AND c.TABLE_NAME = ?
                  AND c.CONSTRAINT_TYPE = 'R'
                GROUP BY c.CONSTRAINT_NAME, c.DELETE_RULE, c.R_OWNER, rc.TABLE_NAME
                ORDER BY c.CONSTRAINT_NAME
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String sourceColumnsStr = rs.getString("SOURCE_COLUMNS");
            String targetColumnsStr = rs.getString("TARGET_COLUMNS");
            return new ForeignKeyInfo(
                    rs.getString("CONSTRAINT_NAME"),
                    tableName.toUpperCase(),
                    sourceColumnsStr != null ? List.of(sourceColumnsStr.split(", ")) : List.of(),
                    rs.getString("R_TABLE_NAME"),
                    targetColumnsStr != null ? List.of(targetColumnsStr.split(", ")) : List.of(),
                    rs.getString("DELETE_RULE")
            );
        }, owner.toUpperCase(), tableName.toUpperCase());
    }

    public List<ForeignKeyInfo> getAllForeignKeys() {
        String sql = """
                SELECT c.OWNER, c.TABLE_NAME, c.CONSTRAINT_NAME, c.DELETE_RULE,
                       c.R_OWNER, rc.TABLE_NAME AS R_TABLE_NAME,
                       LISTAGG(cc.COLUMN_NAME, ', ') WITHIN GROUP (ORDER BY cc.POSITION) AS SOURCE_COLUMNS,
                       LISTAGG(rcc.COLUMN_NAME, ', ') WITHIN GROUP (ORDER BY rcc.POSITION) AS TARGET_COLUMNS
                FROM ALL_CONSTRAINTS c
                JOIN ALL_CONS_COLUMNS cc
                    ON c.OWNER = cc.OWNER AND c.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
                JOIN ALL_CONSTRAINTS rc
                    ON c.R_OWNER = rc.OWNER AND c.R_CONSTRAINT_NAME = rc.CONSTRAINT_NAME
                JOIN ALL_CONS_COLUMNS rcc
                    ON rc.OWNER = rcc.OWNER AND rc.CONSTRAINT_NAME = rcc.CONSTRAINT_NAME
                WHERE c.OWNER NOT IN (%s)
                  AND c.CONSTRAINT_TYPE = 'R'
                GROUP BY c.OWNER, c.TABLE_NAME, c.CONSTRAINT_NAME, c.DELETE_RULE, c.R_OWNER, rc.TABLE_NAME
                ORDER BY c.OWNER, c.TABLE_NAME, c.CONSTRAINT_NAME
                """.formatted(buildExcludedSchemasList());

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String sourceColumnsStr = rs.getString("SOURCE_COLUMNS");
            String targetColumnsStr = rs.getString("TARGET_COLUMNS");
            return new ForeignKeyInfo(
                    rs.getString("CONSTRAINT_NAME"),
                    rs.getString("OWNER") + "." + rs.getString("TABLE_NAME"),
                    sourceColumnsStr != null ? List.of(sourceColumnsStr.split(", ")) : List.of(),
                    rs.getString("R_OWNER") + "." + rs.getString("R_TABLE_NAME"),
                    targetColumnsStr != null ? List.of(targetColumnsStr.split(", ")) : List.of(),
                    rs.getString("DELETE_RULE")
            );
        });
    }

    public TableInfo getFullTableInfo(String owner, String tableName) {
        String sql = """
                SELECT t.OWNER, t.TABLE_NAME, tc.COMMENTS, t.NUM_ROWS
                FROM ALL_TABLES t
                LEFT JOIN ALL_TAB_COMMENTS tc ON t.OWNER = tc.OWNER AND t.TABLE_NAME = tc.TABLE_NAME
                WHERE t.OWNER = ? AND t.TABLE_NAME = ?
                """;

        List<TableInfo> tables = jdbcTemplate.query(sql, (rs, rowNum) -> new TableInfo(
                rs.getString("OWNER"),
                rs.getString("TABLE_NAME"),
                rs.getString("COMMENTS"),
                rs.getObject("NUM_ROWS") != null ? rs.getLong("NUM_ROWS") : null
        ), owner.toUpperCase(), tableName.toUpperCase());

        if (tables.isEmpty()) {
            return null;
        }

        TableInfo tableInfo = tables.get(0);
        return tableInfo
                .withColumns(getTableColumns(owner, tableName))
                .withConstraints(getTableConstraints(owner, tableName))
                .withForeignKeys(getTableForeignKeys(owner, tableName));
    }

    public TableInfo findTable(String tableName) {
        // First try to find exact match
        String sql = """
                SELECT t.OWNER, t.TABLE_NAME, tc.COMMENTS, t.NUM_ROWS
                FROM ALL_TABLES t
                LEFT JOIN ALL_TAB_COMMENTS tc ON t.OWNER = tc.OWNER AND t.TABLE_NAME = tc.TABLE_NAME
                WHERE t.TABLE_NAME = ? AND t.OWNER NOT IN (%s)
                ORDER BY t.OWNER
                """.formatted(buildExcludedSchemasList());

        List<TableInfo> tables = jdbcTemplate.query(sql, (rs, rowNum) -> new TableInfo(
                rs.getString("OWNER"),
                rs.getString("TABLE_NAME"),
                rs.getString("COMMENTS"),
                rs.getObject("NUM_ROWS") != null ? rs.getLong("NUM_ROWS") : null
        ), tableName.toUpperCase());

        if (tables.isEmpty()) {
            return null;
        }

        TableInfo tableInfo = tables.get(0);
        return tableInfo
                .withColumns(getTableColumns(tableInfo.owner(), tableInfo.tableName()))
                .withConstraints(getTableConstraints(tableInfo.owner(), tableInfo.tableName()))
                .withForeignKeys(getTableForeignKeys(tableInfo.owner(), tableInfo.tableName()));
    }

    public Map<String, List<TableInfo>> getSchemaOverview() {
        List<TableInfo> allTables = listAllTables();
        Map<String, List<TableInfo>> schemaMap = new LinkedHashMap<>();

        for (TableInfo table : allTables) {
            TableInfo fullTable = table
                    .withColumns(getTableColumns(table.owner(), table.tableName()));
            schemaMap.computeIfAbsent(table.owner(), k -> new ArrayList<>()).add(fullTable);
        }

        return schemaMap;
    }

    private String buildExcludedSchemasList() {
        return EXCLUDED_SCHEMAS.stream()
                .map(s -> "'" + s + "'")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}
