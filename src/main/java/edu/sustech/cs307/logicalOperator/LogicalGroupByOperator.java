package edu.sustech.cs307.logicalOperator;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.SelectItem;
import java.util.Collections;
import java.util.List;

public class LogicalGroupByOperator extends LogicalOperator {
    private final LogicalOperator child;
    private final List<Expression> groupByColumns;
    private final List<SelectItem<?>> selectItems; // 可能包含 MAX(col), MIN(col) 等

    public LogicalGroupByOperator(LogicalOperator child, List<Expression> groupByColumns,
                                   List<SelectItem<?>> selectItems) {
        super(Collections.singletonList(child));
        this.child = child;
        this.groupByColumns = groupByColumns;
        this.selectItems = selectItems;
    }

    public LogicalOperator getChild() { return child; }
    public List<Expression> getGroupByColumns() { return groupByColumns; }
    public List<SelectItem<?>> getSelectItems() { return selectItems; }

    @Override
    public String toString() {
        return "LogicalGroupBy(groupBy=" + groupByColumns + ", selects=" + selectItems + ")\n └── " + child;
    }
}
