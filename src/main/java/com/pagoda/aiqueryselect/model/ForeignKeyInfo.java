package com.pagoda.aiqueryselect.model;

import java.util.List;

public record ForeignKeyInfo(
        String constraintName,
        String sourceTable,
        List<String> sourceColumns,
        String targetTable,
        List<String> targetColumns,
        String deleteRule
) {
}
