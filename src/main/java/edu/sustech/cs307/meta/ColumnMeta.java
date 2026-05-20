package edu.sustech.cs307.meta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.sustech.cs307.value.*;


public class ColumnMeta {

    @JsonProperty("name")
    public String name;

    @JsonProperty("type")
    public ValueType type;

    @JsonProperty("displayType")
    public String displayType;

    @JsonProperty("len")
    public int len;

    @JsonProperty("offset")
    public int offset;

    @JsonProperty("tableName")
    public String tableName;


    @JsonCreator
    public ColumnMeta(@JsonProperty("tableName") String tableName,
                      @JsonProperty("name") String name,
                      @JsonProperty("type") ValueType type,
                      @JsonProperty("len") int len,
                      @JsonProperty("offset") int offset,
                      @JsonProperty("displayType") String displayType) {
        this.tableName = tableName;
        this.name = name;
        this.type = type;
        this.len = len;
        this.offset = offset;
        if (displayType != null && !displayType.isBlank()) {
            this.displayType = displayType;
        } else {
            this.displayType = normalizeDisplayType(type);
        }
    }

    private static String normalizeDisplayType(ValueType type) {
        if (type == null) return "unknown";
        return switch (type) {
            case CHAR -> "varchar";
            case FLOAT -> "double";
            default -> type.toString();
        };
    }

    public ColumnMeta(String tableName, String name, ValueType type, int len, int offset) {
        this(tableName, name, type, len, offset, null);
    }

    public int getLen() {
        return len;
    }

    public int getOffset() {
        return offset;
    }
}
