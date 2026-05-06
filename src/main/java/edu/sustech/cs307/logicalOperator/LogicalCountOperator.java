package edu.sustech.cs307.logicalOperator;

import java.util.Collections;

public class LogicalCountOperator extends LogicalOperator {
    private final LogicalOperator child;
    private final boolean isCountAll; // true for COUNT(*), false otherwise

    public LogicalCountOperator(LogicalOperator child, boolean isCountAll) {
        super(Collections.singletonList(child));
        this.child = child;
        this.isCountAll = isCountAll;
    }

    public LogicalOperator getChild() {
        return child;
    }

    public boolean isCountAll() {
        return isCountAll;
    }

    @Override
    public String toString() {
        return "CountOperator(countAll=" + isCountAll + ")\n └── " + childern.get(0);
    }
}
