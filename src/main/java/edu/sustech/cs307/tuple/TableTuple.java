package edu.sustech.cs307.tuple;

import edu.sustech.cs307.meta.TabCol;
import edu.sustech.cs307.meta.TableMeta;
import edu.sustech.cs307.record.RID;
import edu.sustech.cs307.record.Record;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.value.ValueType;

import java.util.ArrayList;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.exception.ExceptionTypes;
import edu.sustech.cs307.meta.ColumnMeta;

import io.netty.buffer.ByteBuf;

public class TableTuple extends Tuple {
    private final String tableName;
    private final TableMeta tableMeta;
    private final Record record;
    private final RID rid;

    public TableTuple(String tableName, TableMeta tableMeta, Record record, RID rid) {
        this.tableName = tableName;
        this.tableMeta = tableMeta;
        this.record = record;
        this.rid = rid;
    }

    @Override
    public Value getValue(TabCol tabCol) throws DBException {
        // 如果 tabCol 的表名为空（无表前缀），按列名匹配即可
        if (!tabCol.getTableName().isEmpty() && !tabCol.getTableName().equals(tableName)) {
            return null;
        }
        ColumnMeta columnMeta = tableMeta.getColumnMeta(tabCol.getColumnName());
        if (columnMeta == null) {
            return null;
        }
        int offset = columnMeta.getOffset();
        int len = columnMeta.getLen();
        // 旧记录可能比新 schema 短（ALTER ADD COLUMN 后），超出范围则返回 null
        ByteBuf raw = record.getReadOnlyData();
        if (raw == null || offset + len > raw.capacity()) {
            return null;
        }
        ByteBuf columnValueBuf = raw.slice(offset, len);
        return convertByteBufToValue(columnValueBuf, columnMeta.type);
    }

    private Value convertByteBufToValue(ByteBuf byteBuf, ValueType columnType) throws DBException {
        if (columnType == ValueType.INTEGER) {
            return new Value(byteBuf.getLong(0));
        } else if (columnType == ValueType.CHAR) {
            return new Value(byteBuf.getCharSequence(0, 64, java.nio.charset.StandardCharsets.UTF_8).toString());
        } else if (columnType == ValueType.FLOAT) {
            return new Value(byteBuf.getDouble(0));
        } else {
            throw new DBException(ExceptionTypes.UnsupportedValueType(columnType));
        }
    }

    @Override
    public TabCol[] getTupleSchema() {
        ArrayList<TabCol> result = new ArrayList<TabCol>();
        // 使用column_list 而不是columns
        for (ColumnMeta columnMeta : tableMeta.columns_list) {
            TabCol tabCol = new TabCol(columnMeta.tableName, columnMeta.name);
            result.add(tabCol);
        }
        return result.toArray(new TabCol[0]);

    }

    @Override
    public Value[] getValues() throws DBException {
        // 通过 meta 顺序和信息获取所有 Value
        ArrayList<Value> values = new ArrayList<>();
        //使用column_list而不是columns
        for (ColumnMeta columnMeta : this.tableMeta.columns_list) {
            TabCol tabCol = new TabCol(columnMeta.tableName, columnMeta.name);
            Value value = getValue(tabCol);
            values.add(value);
        }
        return values.toArray(new Value[0]);
    }

    public RID getRID() {
        return this.rid;
    }

    public String getTableName() {
        return this.tableName;
    }
}
