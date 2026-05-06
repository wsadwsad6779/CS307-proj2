package edu.sustech.cs307.logicalOperator;

import net.sf.jsqlparser.expression.Expression;
import java.util.Collections;

public class LogicalExistsOperator extends LogicalOperator {
    private final LogicalOperator child;
    private final LogicalOperator subQueryPlan;
    private final boolean isNot;
    private final Expression subWhere; // 子查询的 WHERE 条件（用于关联检查）

    public LogicalExistsOperator(LogicalOperator child, LogicalOperator subQueryPlan, boolean isNot,
                                  Expression subWhere) {
        super(Collections.singletonList(child));
        this.child = child;
        this.subQueryPlan = subQueryPlan;
        this.isNot = isNot;
        this.subWhere = subWhere;
    }

    public LogicalOperator getChild() { return child; }
    public LogicalOperator getSubQueryPlan() { return subQueryPlan; }
    public boolean isNot() { return isNot; }
    public Expression getSubWhere() { return subWhere; }

    @Override
    public String toString() {
        return "ExistsOperator(not=" + isNot + ")\n ├── " + child + "\n └── subQuery: " + subQueryPlan;
    }
}
