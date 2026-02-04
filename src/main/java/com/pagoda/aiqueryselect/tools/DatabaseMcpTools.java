package com.pagoda.aiqueryselect.tools;

import com.pagoda.aiqueryselect.model.ColumnInfo;
import com.pagoda.aiqueryselect.model.ConstraintInfo;
import com.pagoda.aiqueryselect.model.ForeignKeyInfo;
import com.pagoda.aiqueryselect.model.TableInfo;
import com.pagoda.aiqueryselect.service.QueryService;
import com.pagoda.aiqueryselect.service.QueryService.QueryResult;
import com.pagoda.aiqueryselect.service.SchemaService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DatabaseMcpTools {

    private final SchemaService schemaService;
    private final QueryService queryService;

    public DatabaseMcpTools(SchemaService schemaService, QueryService queryService) {
        this.schemaService = schemaService;
        this.queryService = queryService;
    }

    @McpTool(name = "list-tables", description = "Lists all accessible database tables, excluding system schemas. Returns table names with their owners, comments, and approximate row counts.")
    public String listTables() {
        try {
            List<TableInfo> tables = schemaService.listAllTables();

            if (tables.isEmpty()) {
                return "No accessible tables found.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(tables.size()).append(" tables:\n\n");

            String currentOwner = null;
            for (TableInfo table : tables) {
                if (!table.owner().equals(currentOwner)) {
                    currentOwner = table.owner();
                    sb.append("## Schema: ").append(currentOwner).append("\n\n");
                }

                sb.append("- **").append(table.tableName()).append("**");
                if (table.numRows() != null) {
                    sb.append(" (~").append(formatNumber(table.numRows())).append(" rows)");
                }
                if (table.comments() != null && !table.comments().isBlank()) {
                    sb.append(": ").append(table.comments());
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error listing tables: " + e.getMessage();
        }
    }

    @McpTool(name = "get-table-schema", description = "Returns detailed schema information for a specific table, including columns with their data types, constraints (primary keys, unique, check), and foreign key relationships.")
    public String getTableSchema(
            @McpToolParam(description = "The name of the table to describe. Can be just the table name or OWNER.TABLE_NAME format.", required = true) String tableName) {
        try {
            TableInfo tableInfo;

            if (tableName.contains(".")) {
                String[] parts = tableName.split("\\.", 2);
                tableInfo = schemaService.getFullTableInfo(parts[0], parts[1]);
            } else {
                tableInfo = schemaService.findTable(tableName);
            }

            if (tableInfo == null) {
                return "Table '" + tableName + "' not found or not accessible.";
            }

            return formatTableSchema(tableInfo);
        } catch (Exception e) {
            return "Error getting table schema: " + e.getMessage();
        }
    }

    @McpTool(name = "execute-select", description = "Executes a read-only SELECT query against the database. Only SELECT and WITH statements are allowed. Results are limited to prevent excessive data retrieval.")
    public String executeSelect(
            @McpToolParam(description = "The SELECT query to execute. Must be a valid Oracle SQL SELECT statement. INSERT, UPDATE, DELETE and other modifying statements are not allowed.", required = true) String query,
            @McpToolParam(description = "Maximum number of rows to return (default: 100, max: 1000)", required = false) Integer maxRows) {
        try {
            int effectiveMaxRows = maxRows != null ? Math.min(Math.max(maxRows, 1), 1000) : 100;

            QueryResult result = queryService.executeQuery(query, effectiveMaxRows);

            if (!result.success()) {
                return "Query failed: " + result.errorMessage();
            }

            return formatQueryResult(result);
        } catch (Exception e) {
            return "Error executing query: " + e.getMessage();
        }
    }

    private String formatTableSchema(TableInfo table) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Table: ").append(table.owner()).append(".").append(table.tableName()).append("\n\n");

        if (table.comments() != null && !table.comments().isBlank()) {
            sb.append("**Description:** ").append(table.comments()).append("\n\n");
        }

        if (table.numRows() != null) {
            sb.append("**Approximate Rows:** ").append(formatNumber(table.numRows())).append("\n\n");
        }

        // Columns
        sb.append("## Columns\n\n");
        sb.append("| # | Column | Type | Nullable | Default | Description |\n");
        sb.append("|---|--------|------|----------|---------|-------------|\n");

        for (ColumnInfo col : table.columns()) {
            sb.append("| ").append(col.columnPosition()).append(" | ");
            sb.append(col.columnName()).append(" | ");
            sb.append(col.getFormattedType()).append(" | ");
            sb.append(col.nullable() ? "YES" : "NO").append(" | ");
            sb.append(col.defaultValue() != null ? escapeMarkdown(col.defaultValue().trim()) : "").append(" | ");
            sb.append(col.comments() != null ? escapeMarkdown(col.comments()) : "").append(" |\n");
        }

        // Constraints
        if (!table.constraints().isEmpty()) {
            sb.append("\n## Constraints\n\n");

            for (ConstraintInfo constraint : table.constraints()) {
                sb.append("- **").append(constraint.constraintName()).append("** (");
                sb.append(constraint.getConstraintTypeDescription()).append("): ");
                sb.append(String.join(", ", constraint.columns()));
                if (constraint.searchCondition() != null && "C".equals(constraint.constraintType())) {
                    sb.append(" - ").append(constraint.searchCondition());
                }
                sb.append("\n");
            }
        }

        // Foreign Keys
        if (!table.foreignKeys().isEmpty()) {
            sb.append("\n## Foreign Keys\n\n");

            for (ForeignKeyInfo fk : table.foreignKeys()) {
                sb.append("- **").append(fk.constraintName()).append("**: ");
                sb.append(String.join(", ", fk.sourceColumns()));
                sb.append(" → ").append(fk.targetTable()).append("(");
                sb.append(String.join(", ", fk.targetColumns())).append(")");
                if (fk.deleteRule() != null && !"NO ACTION".equals(fk.deleteRule())) {
                    sb.append(" [ON DELETE ").append(fk.deleteRule()).append("]");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private String formatQueryResult(QueryResult result) {
        StringBuilder sb = new StringBuilder();

        if (result.rows().isEmpty()) {
            return "Query executed successfully. No rows returned.";
        }

        sb.append("Query returned ").append(result.rowCount()).append(" row(s)");
        if (result.truncated()) {
            sb.append(" (limited to ").append(result.maxRows()).append(")");
        }
        sb.append(".\n\n");

        // Build markdown table
        List<String> columns = result.columns();

        // Header
        sb.append("| ").append(String.join(" | ", columns)).append(" |\n");
        sb.append("|").append(columns.stream().map(c -> "---").collect(Collectors.joining("|"))).append("|\n");

        // Rows
        for (Map<String, Object> row : result.rows()) {
            sb.append("| ");
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) sb.append(" | ");
                Object value = row.get(columns.get(i));
                sb.append(formatValue(value));
            }
            sb.append(" |\n");
        }

        return sb.toString();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        String str = value.toString();
        if (str.length() > 100) {
            str = str.substring(0, 97) + "...";
        }
        return escapeMarkdown(str);
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("|", "\\|").replace("\n", " ").replace("\r", "");
    }

    private String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }
}
