package com.pagoda.aiqueryselect.resources;

import com.pagoda.aiqueryselect.model.ColumnInfo;
import com.pagoda.aiqueryselect.model.ConstraintInfo;
import com.pagoda.aiqueryselect.model.ForeignKeyInfo;
import com.pagoda.aiqueryselect.model.TableInfo;
import com.pagoda.aiqueryselect.service.SchemaService;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DatabaseMcpResources {

    private final SchemaService schemaService;

    public DatabaseMcpResources(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @McpResource(
            uri = "schema://overview",
            name = "Database Schema Overview",
            description = "Complete overview of all accessible database schemas, tables, and their columns",
            mimeType = "text/markdown"
    )
    public String getSchemaOverview() {
        try {
            Map<String, List<TableInfo>> schemas = schemaService.getSchemaOverview();

            if (schemas.isEmpty()) {
                return "# Database Schema Overview\n\nNo accessible schemas or tables found.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("# Database Schema Overview\n\n");

            int totalTables = schemas.values().stream().mapToInt(List::size).sum();
            sb.append("**Total Schemas:** ").append(schemas.size()).append("\n");
            sb.append("**Total Tables:** ").append(totalTables).append("\n\n");

            for (Map.Entry<String, List<TableInfo>> entry : schemas.entrySet()) {
                String schemaName = entry.getKey();
                List<TableInfo> tables = entry.getValue();

                sb.append("## Schema: ").append(schemaName).append("\n\n");
                sb.append("*").append(tables.size()).append(" table(s)*\n\n");

                for (TableInfo table : tables) {
                    sb.append("### ").append(table.tableName()).append("\n\n");

                    if (table.comments() != null && !table.comments().isBlank()) {
                        sb.append("*").append(table.comments()).append("*\n\n");
                    }

                    if (!table.columns().isEmpty()) {
                        sb.append("| Column | Type | Nullable |\n");
                        sb.append("|--------|------|----------|\n");

                        for (ColumnInfo col : table.columns()) {
                            sb.append("| ").append(col.columnName()).append(" | ");
                            sb.append(col.getFormattedType()).append(" | ");
                            sb.append(col.nullable() ? "YES" : "NO").append(" |\n");
                        }
                        sb.append("\n");
                    }
                }
            }

            return sb.toString();
        } catch (Exception e) {
            return "# Database Schema Overview\n\nError generating schema overview: " + e.getMessage();
        }
    }

    @McpResource(
            uri = "schema://relationships",
            name = "Table Relationships",
            description = "All foreign key relationships between tables in the database",
            mimeType = "text/markdown"
    )
    public String getRelationships() {
        try {
            List<ForeignKeyInfo> foreignKeys = schemaService.getAllForeignKeys();

            if (foreignKeys.isEmpty()) {
                return "# Table Relationships\n\nNo foreign key relationships found.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("# Table Relationships\n\n");
            sb.append("**Total Foreign Keys:** ").append(foreignKeys.size()).append("\n\n");

            sb.append("## Foreign Key Relationships\n\n");
            sb.append("| Source Table | Source Column(s) | Target Table | Target Column(s) | Delete Rule |\n");
            sb.append("|--------------|------------------|--------------|------------------|-------------|\n");

            for (ForeignKeyInfo fk : foreignKeys) {
                sb.append("| ").append(fk.sourceTable()).append(" | ");
                sb.append(String.join(", ", fk.sourceColumns())).append(" | ");
                sb.append(fk.targetTable()).append(" | ");
                sb.append(String.join(", ", fk.targetColumns())).append(" | ");
                sb.append(fk.deleteRule() != null ? fk.deleteRule() : "NO ACTION").append(" |\n");
            }

            sb.append("\n## Relationship Diagram (Text)\n\n");
            sb.append("```\n");
            for (ForeignKeyInfo fk : foreignKeys) {
                sb.append(fk.sourceTable()).append(" --[").append(fk.constraintName()).append("]--> ");
                sb.append(fk.targetTable()).append("\n");
            }
            sb.append("```\n");

            return sb.toString();
        } catch (Exception e) {
            return "# Table Relationships\n\nError generating relationships overview: " + e.getMessage();
        }
    }

    @McpResource(
            uri = "schema://table/{tableName}",
            name = "Table Schema",
            description = "Detailed schema information for a specific table including columns, constraints, and foreign keys",
            mimeType = "text/markdown"
    )
    public String getTableSchema(String tableName) {
        try {
            TableInfo tableInfo;

            if (tableName.contains(".")) {
                String[] parts = tableName.split("\\.", 2);
                tableInfo = schemaService.getFullTableInfo(parts[0], parts[1]);
            } else {
                tableInfo = schemaService.findTable(tableName);
            }

            if (tableInfo == null) {
                return "# Table: " + tableName + "\n\nTable not found or not accessible.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("# Table: ").append(tableInfo.owner()).append(".").append(tableInfo.tableName()).append("\n\n");

            if (tableInfo.comments() != null && !tableInfo.comments().isBlank()) {
                sb.append("**Description:** ").append(tableInfo.comments()).append("\n\n");
            }

            if (tableInfo.numRows() != null) {
                sb.append("**Approximate Rows:** ").append(tableInfo.numRows()).append("\n\n");
            }

            // Columns
            sb.append("## Columns\n\n");
            sb.append("| # | Column | Type | Nullable | Default | Description |\n");
            sb.append("|---|--------|------|----------|---------|-------------|\n");

            for (ColumnInfo col : tableInfo.columns()) {
                sb.append("| ").append(col.columnPosition()).append(" | ");
                sb.append(col.columnName()).append(" | ");
                sb.append(col.getFormattedType()).append(" | ");
                sb.append(col.nullable() ? "YES" : "NO").append(" | ");
                sb.append(col.defaultValue() != null ? escapeMarkdown(col.defaultValue().trim()) : "").append(" | ");
                sb.append(col.comments() != null ? escapeMarkdown(col.comments()) : "").append(" |\n");
            }

            // Constraints
            if (!tableInfo.constraints().isEmpty()) {
                sb.append("\n## Constraints\n\n");

                for (ConstraintInfo constraint : tableInfo.constraints()) {
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
            if (!tableInfo.foreignKeys().isEmpty()) {
                sb.append("\n## Foreign Keys\n\n");

                for (ForeignKeyInfo fk : tableInfo.foreignKeys()) {
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
        } catch (Exception e) {
            return "# Table: " + tableName + "\n\nError generating table schema: " + e.getMessage();
        }
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("|", "\\|").replace("\n", " ").replace("\r", "");
    }
}
