package edu.sustech.cs307.physicalOperator;

import java.util.ArrayList;
import java.util.Collection;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.meta.TabCol;
import edu.sustech.cs307.tuple.JoinTuple;
import edu.sustech.cs307.tuple.Tuple;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;

public class NestedLoopJoinOperator implements PhysicalOperator {

    private PhysicalOperator leftOperator;
    private PhysicalOperator rightOperator;
    private Collection<Expression> joinExprs;
    private Tuple currentTuple;
    private boolean isOpen;
    private Tuple leftTuple;
    private boolean rightReset;

    public NestedLoopJoinOperator(PhysicalOperator leftOperator, PhysicalOperator rightOperator,
            Collection<Expression> expr) {
        this.leftOperator = leftOperator;
        this.rightOperator = rightOperator;
        this.joinExprs = expr;
        this.currentTuple = null;
        this.isOpen = false;
    }

    @Override
    public void Begin() throws DBException {
        leftOperator.Begin();
        rightOperator.Begin();
        isOpen = true;
        currentTuple = null;
        leftTuple = null;
        rightReset = true;

        // 读取左表第一行
        if (leftOperator.hasNext()) {
            leftOperator.Next();
            leftTuple = leftOperator.Current();
        }
    }

    @Override
    public boolean hasNext() {
        if (!isOpen) return false;
        currentTuple = null;
        try {
            while (leftTuple != null) {
                // 如果右表需要重置
                if (rightReset) {
                    rightOperator.Close();
                    rightOperator.Begin();
                    rightReset = false;
                }
                // 扫描右表
                while (rightOperator.hasNext()) {
                    rightOperator.Next();
                    Tuple rightTuple = rightOperator.Current();
                    if (rightTuple == null) continue;

                    JoinTuple joined = new JoinTuple(leftTuple, rightTuple,
                            buildJoinSchema(leftTuple, rightTuple));
                    // 检查 equi-join 条件
                    if (checkJoinCondition(joined)) {
                        currentTuple = joined;
                        return true;
                    }
                }
                // 右表扫描完毕，读取左表下一行
                if (leftOperator.hasNext()) {
                    leftOperator.Next();
                    leftTuple = leftOperator.Current();
                    rightReset = true;
                } else {
                    leftTuple = null;
                }
            }
        } catch (DBException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean checkJoinCondition(Tuple joined) throws DBException {
        if (joinExprs == null || joinExprs.isEmpty()) return true;
        for (Expression expr : joinExprs) {
            if (expr instanceof BinaryExpression be) {
                Expression left = be.getLeftExpression();
                Expression right = be.getRightExpression();
                if (left instanceof Column lc && right instanceof Column rc) {
                    String ltn = lc.getTableName() != null ? lc.getTableName() : "";
                    String rtn = rc.getTableName() != null ? rc.getTableName() : "";
                    var lv = joined.getValue(new TabCol(ltn, lc.getColumnName()));
                    var rv = joined.getValue(new TabCol(rtn, rc.getColumnName()));
                    if (lv == null || rv == null) return false;
                    if (edu.sustech.cs307.value.ValueComparer.compare(lv, rv) != 0) return false;
                }
            }
        }
        return true;
    }

    private TabCol[] buildJoinSchema(Tuple left, Tuple right) {
        TabCol[] ls = left.getTupleSchema();
        TabCol[] rs = right.getTupleSchema();
        TabCol[] result = new TabCol[ls.length + rs.length];
        System.arraycopy(ls, 0, result, 0, ls.length);
        System.arraycopy(rs, 0, result, ls.length, rs.length);
        return result;
    }

    @Override
    public void Next() {
        // hasNext() 已找到并存储匹配元组，Next() 无需清空
    }

    @Override
    public Tuple Current() { return currentTuple; }

    @Override
    public void Close() {
        leftOperator.Close();
        rightOperator.Close();
        isOpen = false;
        currentTuple = null;
    }

    @Override
    public ArrayList<ColumnMeta> outputSchema() {
        ArrayList<ColumnMeta> schema = new ArrayList<>();
        schema.addAll(leftOperator.outputSchema());
        schema.addAll(rightOperator.outputSchema());
        return schema;
    }
}
