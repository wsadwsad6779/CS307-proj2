package edu.sustech.cs307.physicalOperator;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.tuple.ProjectTuple;
import edu.sustech.cs307.tuple.Tuple;
import edu.sustech.cs307.meta.TabCol;
import edu.sustech.cs307.value.ValueType;

import java.util.ArrayList;
import java.util.List;

public class ProjectOperator implements PhysicalOperator {
    private PhysicalOperator child;
    private List<TabCol> outputSchema; // Use bounded wildcard
    private Tuple currentTuple;

    public ProjectOperator(PhysicalOperator child, List<TabCol> outputSchema) { // Use bounded wildcard
        this.child = child;
        this.outputSchema = outputSchema;
        if (this.outputSchema.size() == 1 && this.outputSchema.get(0).getTableName().equals("*")) {
            List<TabCol> newOutputSchema = new ArrayList<>();
            for (ColumnMeta tabCol : child.outputSchema()) {
                newOutputSchema.add(new TabCol(tabCol.tableName, tabCol.name));
            }
            this.outputSchema = newOutputSchema;
        }
    }

    @Override
    public boolean hasNext() throws DBException {
        return child.hasNext();
    }

    @Override
    public void Begin() throws DBException {
        child.Begin();
    }

    @Override
    public void Next() throws DBException {
        // hasNext() has already been evaluated by the caller; do not call it again here
        child.Next();
        Tuple inputTuple = child.Current();
        if (inputTuple != null) {
            currentTuple = new ProjectTuple(inputTuple, outputSchema);
        } else {
            currentTuple = null;
        }
    }

    @Override
    public Tuple Current() {
        return currentTuple;
    }

    @Override
    public void Close() {
        child.Close();
        currentTuple = null;
    }

    @Override
    public ArrayList<ColumnMeta> outputSchema() {
        // 只返回投影选中的列，而不是子节点的全部列
        ArrayList<ColumnMeta> result = new ArrayList<>();
        ArrayList<ColumnMeta> childSchema = child.outputSchema();
        for (TabCol tc : outputSchema) {
            if (tc.getTableName().equals("*") && tc.getColumnName().equals("*")) {
                // SELECT *: 返回所有列，去掉表名前缀
                for (ColumnMeta cm : childSchema) {
                    result.add(new ColumnMeta("", cm.name, cm.type, cm.len, cm.offset));
                }
                return result;
            }
            for (ColumnMeta cm : childSchema) {
                boolean tableMatches = tc.getTableName().isEmpty() || cm.tableName.equals(tc.getTableName());
                if (tableMatches && cm.name.equals(tc.getColumnName())) {
                    // 投影列不再属于原表，清空表名
                    result.add(new ColumnMeta("", cm.name, cm.type, cm.len, cm.offset));
                    break;
                }
            }
        }
        return result;
    }
}
