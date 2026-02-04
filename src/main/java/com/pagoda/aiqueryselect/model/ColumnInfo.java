package com.pagoda.aiqueryselect.model;

public record ColumnInfo(
        String columnName,
        String dataType,
        Integer dataLength,
        Integer dataPrecision,
        Integer dataScale,
        boolean nullable,
        String defaultValue,
        String comments,
        int columnPosition
) {
    public String getFormattedType() {
        if (dataPrecision != null && dataScale != null && dataScale > 0) {
            return String.format("%s(%d,%d)", dataType, dataPrecision, dataScale);
        } else if (dataPrecision != null) {
            return String.format("%s(%d)", dataType, dataPrecision);
        } else if (dataLength != null && (dataType.contains("CHAR") || dataType.contains("RAW"))) {
            return String.format("%s(%d)", dataType, dataLength);
        }
        return dataType;
    }
}
