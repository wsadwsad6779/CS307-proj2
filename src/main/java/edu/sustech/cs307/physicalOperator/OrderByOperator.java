package edu.sustech.cs307.physicalOperator;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.meta.TabCol;
import edu.sustech.cs307.tuple.TempTuple;
import edu.sustech.cs307.tuple.Tuple;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.value.ValueType;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.OrderByElement;

import java.util.*;

public class OrderByOperator implements PhysicalOperator {
    private final PhysicalOperator child;
    private final List<OrderByElement> orderByElements;
    private final List<Tuple> sortedTuples;
    private int index;
    private boolean isOpen;

    public OrderByOperator(PhysicalOperator child, List<OrderByElement> orderByElements) {
        this.child = child;
        this.orderByElements = orderByElements;
        this.sortedTuples = new ArrayList<>();
        this.index = 0;
        this.isOpen = false;
    }

    @Override
    public void Begin() throws DBException {
        child.Begin();
        // 收集所有元组
        List<Tuple> tuples = new ArrayList<>();
        while (child.hasNext()) {
            child.Next();
            Tuple t = child.Current();
            if (t != null) tuples.add(t);
        }
        // 排序
        tuples.sort((a, b) -> {
            for (OrderByElement elem : orderByElements) {
                try {
                    Expression expr = elem.getExpression();
                    String colName;
                    String tblName = "";
                    if (expr instanceof Column c) {
                        colName = c.getColumnName();
                        tblName = c.getTableName() != null ? c.getTableName() : "";
                    } else {
                        continue;
                    }
                    Value va = a.getValue(new TabCol(tblName, colName));
                    Value vb = b.getValue(new TabCol(tblName, colName));
                    if (va == null || vb == null) continue;
                    int cmp = edu.sustech.cs307.value.ValueComparer.compare(va, vb);
                    if (cmp != 0) {
                        return elem.isAsc() ? cmp : -cmp;
                    }
                } catch (DBException e) { e.printStackTrace(); }
            }
            return 0;
        });
        sortedTuples.addAll(tuples);
        index = 0;
        isOpen = true;
    }

    @Override
    public boolean hasNext() { return isOpen && index < sortedTuples.size(); }

    @Override
    public void Next() { index++; }

    @Override
    public Tuple Current() {
        if (index > 0 && index <= sortedTuples.size()) return sortedTuples.get(index - 1);
        return null;
    }

    @Override
    public void Close() { child.Close(); sortedTuples.clear(); isOpen = false; }

    @Override
    public ArrayList<ColumnMeta> outputSchema() { return child.outputSchema(); }
}
