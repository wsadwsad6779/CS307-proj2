package edu.sustech.cs307.physicalOperator;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.meta.TabCol;
import edu.sustech.cs307.tuple.TempTuple;
import edu.sustech.cs307.tuple.Tuple;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.value.ValueType;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SelectItem;

import java.util.*;

public class GroupByOperator implements PhysicalOperator {
    private final PhysicalOperator child;
    private final List<Expression> groupByExprs;
    private final List<SelectItem<?>> selectItems;
    private final List<Tuple> resultTuples;
    private int index;
    private boolean isOpen;

    public GroupByOperator(PhysicalOperator child, List<Expression> groupByExprs,
                           List<SelectItem<?>> selectItems) {
        this.child = child;
        this.groupByExprs = groupByExprs;
        this.selectItems = selectItems;
        this.resultTuples = new ArrayList<>();
        this.index = 0;
        this.isOpen = false;
    }

    @Override
    public void Begin() throws DBException {
        child.Begin();
        // 构建 schema（列名列表）
        List<TabCol> outSchema = new ArrayList<>();
        for (SelectItem<?> item : selectItems) {
            Expression expr = item.getExpression();
            if (expr instanceof Column c) {
                outSchema.add(new TabCol("", c.getColumnName()));
            } else {
                outSchema.add(new TabCol("", expr.toString()));
            }
        }

        // 收集所有元组并按分组键分组
        Map<String, List<Tuple>> groups = new LinkedHashMap<>();
        Map<String, List<Value>> groupKeys = new LinkedHashMap<>();

        while (child.hasNext()) {
            child.Next();
            Tuple t = child.Current();
            if (t == null) continue;

            StringBuilder key = new StringBuilder();
            List<Value> keyValues = new ArrayList<>();
            for (Expression expr : groupByExprs) {
                String colName = extractColName(expr);
                Value v = t.getValue(new TabCol("", colName));
                if (v != null) {
                    key.append(v.toString()).append("|");
                    keyValues.add(v);
                }
            }
            String keyStr = key.toString();
            groups.computeIfAbsent(keyStr, k -> new ArrayList<>()).add(t);
            if (!groupKeys.containsKey(keyStr)) groupKeys.put(keyStr, keyValues);
        }

        // 对每个分组计算聚合
        for (Map.Entry<String, List<Tuple>> entry : groups.entrySet()) {
            List<Tuple> group = entry.getValue();
            List<Value> aggValues = new ArrayList<>();

            for (SelectItem<?> item : selectItems) {
                Expression expr = item.getExpression();
                if (expr instanceof Function func) {
                    String funcName = func.getName().toUpperCase();
                    String colName = extractFuncColName(func);
                    switch (funcName) {
                        case "MAX" -> aggValues.add(computeMax(group, colName));
                        case "MIN" -> aggValues.add(computeMin(group, colName));
                        case "COUNT" -> aggValues.add(new Value(group.size(), ValueType.INTEGER));
                        default -> aggValues.add(new Value(0, ValueType.INTEGER));
                    }
                } else if (expr instanceof Column col) {
                    // 直接分组列
                    Value v = group.get(0).getValue(new TabCol("", col.getColumnName()));
                    if (v != null) aggValues.add(v);
                    else aggValues.add(new Value(0, ValueType.INTEGER));
                }
            }
            resultTuples.add(new TempTuple(aggValues, outSchema));
        }
        index = 0;
        isOpen = true;
    }

    private Value computeMax(List<Tuple> group, String colName) throws DBException {
        Value max = null;
        for (Tuple t : group) {
            Value v = t.getValue(new TabCol("", colName));
            if (v != null && (max == null ||
                edu.sustech.cs307.value.ValueComparer.compare(v, max) > 0)) max = v;
        }
        return max != null ? max : new Value(0, ValueType.INTEGER);
    }

    private Value computeMin(List<Tuple> group, String colName) throws DBException {
        Value min = null;
        for (Tuple t : group) {
            Value v = t.getValue(new TabCol("", colName));
            if (v != null && (min == null ||
                edu.sustech.cs307.value.ValueComparer.compare(v, min) < 0)) min = v;
        }
        return min != null ? min : new Value(0, ValueType.INTEGER);
    }

    private String extractColName(Expression expr) {
        if (expr instanceof Column c) return c.getColumnName();
        return expr.toString();
    }

    private String extractFuncColName(Function func) {
        if (func.getParameters() != null) {
            var params = func.getParameters().getExpressions();
            if (params != null && !params.isEmpty()) {
                var p = params.get(0);
                if (p instanceof Column c) return c.getColumnName();
                return p.toString();
            }
        }
        return "*";
    }

    @Override public boolean hasNext() { return isOpen && index < resultTuples.size(); }
    @Override public void Next() { index++; }
    @Override
    public Tuple Current() {
        if (index > 0 && index <= resultTuples.size()) return resultTuples.get(index - 1);
        return null;
    }
    @Override public void Close() { child.Close(); resultTuples.clear(); isOpen = false; }

    @Override
    public ArrayList<ColumnMeta> outputSchema() {
        ArrayList<ColumnMeta> schema = new ArrayList<>();
        for (SelectItem<?> item : selectItems) {
            String name = item.toString();
            // 表名为空，避免显示 "group.xxx"
            schema.add(new ColumnMeta("", name, ValueType.CHAR, 0, 0));
        }
        return schema;
    }
}
