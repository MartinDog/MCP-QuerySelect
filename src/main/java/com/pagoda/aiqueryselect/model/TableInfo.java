package com.pagoda.aiqueryselect.model;

import java.util.List;

public record TableInfo(
        String owner,
        String tableName,
        String comments,
        Long numRows,
        List<ColumnInfo> columns,
        List<ConstraintInfo> constraints,
        List<ForeignKeyInfo> foreignKeys
) {
    public TableInfo(String owner, String tableName, String comments, Long numRows) {
        this(owner, tableName, comments, numRows, List.of(), List.of(), List.of());
    }

    public TableInfo withColumns(List<ColumnInfo> columns) {
        return new TableInfo(owner, tableName, comments, numRows, columns, constraints, foreignKeys);
    }

    public TableInfo withConstraints(List<ConstraintInfo> constraints) {
        return new TableInfo(owner, tableName, comments, numRows, columns, constraints, foreignKeys);
    }

    public TableInfo withForeignKeys(List<ForeignKeyInfo> foreignKeys) {
        return new TableInfo(owner, tableName, comments, numRows, columns, constraints, foreignKeys);
    }
}
