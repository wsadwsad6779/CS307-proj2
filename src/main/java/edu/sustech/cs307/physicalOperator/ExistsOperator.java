package edu.sustech.cs307.physicalOperator;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.meta.TabCol;
import edu.sustech.cs307.tuple.JoinTuple;
import edu.sustech.cs307.tuple.Tuple;
import net.sf.jsqlparser.expression.Expression;
import java.util.ArrayList;

public class ExistsOperator implements PhysicalOperator {
    private final PhysicalOperator outerChild;
    private final PhysicalOperator subScan;
    private final boolean isNot;
    private final Expression subWhere;
    private boolean isOpen;
    private Tuple resultTuple;

    public ExistsOperator(PhysicalOperator outerChild, PhysicalOperator subScan, boolean isNot,
                          Expression subWhere) {
        this.outerChild = outerChild;
        this.subScan = subScan;
        this.isNot = isNot;
        this.subWhere = subWhere;
    }

    @Override
    public void Begin() throws DBException {
        outerChild.Begin();
        isOpen = true;
        resultTuple = null;
    }

    @Override
    public boolean hasNext() {
        if (!isOpen) return false;
        try {
            while (outerChild.hasNext()) {
                outerChild.Next();
                Tuple outerTuple = outerChild.Current();
                if (outerTuple == null) continue;

                // 扫描内表（无过滤，全表扫描）
                subScan.Close();
                subScan.Begin();
                boolean found = false;
                while (subScan.hasNext()) {
                    subScan.Next();
                    Tuple innerTuple = subScan.Current();
                    if (innerTuple == null) continue;

                    // 用 JoinTuple 使关联列对 WHERE 条件可见
                    JoinTuple joined = new JoinTuple(outerTuple, innerTuple,
                            buildSchema(outerTuple, innerTuple));
                    // 评估关联条件
                    if (subWhere == null || joined.eval_expr(subWhere)) {
                        found = true;
                        break;
                    }
                }
                if (isNot ? !found : found) {
                    resultTuple = outerTuple;
                    return true;
                }
            }
        } catch (DBException e) {
            e.printStackTrace();
        }
        return false;
    }

    private TabCol[] buildSchema(Tuple a, Tuple b) {
        TabCol[] sa = a.getTupleSchema();
        TabCol[] sb = b.getTupleSchema();
        if (sa == null) sa = new TabCol[0];
        if (sb == null) sb = new TabCol[0];
        TabCol[] r = new TabCol[sa.length + sb.length];
        System.arraycopy(sa, 0, r, 0, sa.length);
        System.arraycopy(sb, 0, r, sa.length, sb.length);
        return r;
    }

    @Override public void Next() { resultTuple = null; }
    @Override public Tuple Current() { return resultTuple; }
    @Override public void Close() { outerChild.Close(); subScan.Close(); isOpen = false; }

    @Override
    public ArrayList<ColumnMeta> outputSchema() { return outerChild.outputSchema(); }
}
