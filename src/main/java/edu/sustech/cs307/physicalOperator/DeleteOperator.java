package edu.sustech.cs307.physicalOperator;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.index.BPlusTree;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.meta.TabCol;
import edu.sustech.cs307.record.RecordFileHandle;
import edu.sustech.cs307.record.RID;
import edu.sustech.cs307.tuple.TableTuple;
import edu.sustech.cs307.tuple.TempTuple;
import edu.sustech.cs307.tuple.Tuple;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.value.ValueType;
import net.sf.jsqlparser.expression.Expression;

import java.util.ArrayList;
import java.util.Map;

public class DeleteOperator implements PhysicalOperator {
    private final SeqScanOperator seqScanOperator;
    private final String tableName;
    private final Expression whereExpr;

    private int deleteCount;
    private boolean isDone;

    public DeleteOperator(PhysicalOperator inputOperator, String tableName, Expression whereExpr) {
        if (!(inputOperator instanceof SeqScanOperator)) {
            throw new RuntimeException("The delete operator only accepts SeqScanOperator as input");
        }
        this.seqScanOperator = (SeqScanOperator) inputOperator;
        this.tableName = tableName;
        this.whereExpr = whereExpr;
        this.deleteCount = 0;
        this.isDone = false;
    }

    @Override
    public boolean hasNext() {
        return !isDone;
    }

    @Override
    public void Begin() throws DBException {
        seqScanOperator.Begin();
        RecordFileHandle fileHandle = seqScanOperator.getFileHandle();

        // 该表上的索引（列名 -> B+ 树），删行时同步维护
        Map<String, BPlusTree> indexes =
                seqScanOperator.getDbManager().getIndexManager().getIndexesForTable(tableName);

        // 收集要删除的RID列表（避免在遍历时修改）
        ArrayList<RID> ridsToDelete = new ArrayList<>();
        while (seqScanOperator.hasNext()) {
            seqScanOperator.Next();
            TableTuple tuple = (TableTuple) seqScanOperator.Current();

            if (whereExpr == null || tuple.eval_expr(whereExpr)) {
                ridsToDelete.add(tuple.getRID());
                // 索引联动：从每棵树里删掉这一行（按 列值 + RID 定位具体行）
                RID ridToDelete = tuple.getRID();
                for (Map.Entry<String, BPlusTree> e : indexes.entrySet()) {
                    Value v = tuple.getValue(new TabCol("", e.getKey()));
                    if (v != null) {
                        e.getValue().delete(v, ridToDelete);
                    }
                }
            }
        }

        // 执行删除
        for (RID rid : ridsToDelete) {
            fileHandle.DeleteRecord(rid);
            deleteCount++;
        }
    }

    @Override
    public void Next() {
        isDone = true;
    }

    @Override
    public Tuple Current() {
        if (isDone) {
            ArrayList<Value> result = new ArrayList<>();
            result.add(new Value(deleteCount, ValueType.INTEGER));
            return new TempTuple(result);
        } else {
            throw new RuntimeException("Call Next() first");
        }
    }

    @Override
    public void Close() {
        seqScanOperator.Close();
    }

    @Override
    public ArrayList<ColumnMeta> outputSchema() {
        ArrayList<ColumnMeta> schema = new ArrayList<>();
        schema.add(new ColumnMeta("delete", "numberOfDeletedRows", ValueType.INTEGER, 0, 0));
        return schema;
    }
}
