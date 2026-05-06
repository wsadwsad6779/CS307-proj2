package edu.sustech.cs307.tuple;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.meta.TabCol;
import edu.sustech.cs307.value.Value;

import java.util.ArrayList;
import java.util.List;

public class TempTuple extends Tuple {
    private final List<Value> values;
    private final List<TabCol> schema;

    public TempTuple(List<Value> values) {
        this.values = values;
        this.schema = null;
    }

    public TempTuple(List<Value> values, List<TabCol> schema) {
        this.values = values;
        this.schema = schema;
    }

    @Override
    public Value getValue(TabCol tabCol) throws DBException {
        // 有 schema 时按列名匹配
        if (schema != null) {
            for (int i = 0; i < schema.size(); i++) {
                TabCol sc = schema.get(i);
                boolean tableMatch = sc.getTableName().isEmpty()
                        || tabCol.getTableName().isEmpty()
                        || sc.getTableName().equals(tabCol.getTableName());
                if (tableMatch && sc.getColumnName().equals(tabCol.getColumnName())) {
                    return i < values.size() ? values.get(i) : null;
                }
            }
            return null;
        }
        // 无 schema 时回退抛异常（保持向后兼容）
        throw new DBException(edu.sustech.cs307.exception.ExceptionTypes.GetValueFromTempTuple());
    }

    @Override
    public TabCol[] getTupleSchema() {
        if (schema != null) return schema.toArray(new TabCol[0]);
        return null;
    }

    @Override
    public Value[] getValues() throws DBException {
        return this.values.toArray(new Value[0]);
    }
}