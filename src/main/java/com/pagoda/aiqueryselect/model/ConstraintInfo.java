package com.pagoda.aiqueryselect.model;

import java.util.List;

public record ConstraintInfo(
        String constraintName,
        String constraintType,
        List<String> columns,
        String searchCondition
) {
    public String getConstraintTypeDescription() {
        return switch (constraintType) {
            case "P" -> "PRIMARY KEY";
            case "U" -> "UNIQUE";
            case "C" -> "CHECK";
            case "R" -> "FOREIGN KEY";
            default -> constraintType;
        };
    }
}
