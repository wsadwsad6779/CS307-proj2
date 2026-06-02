package edu.sustech.cs307.physicalOperator;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.index.BPlusTree;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.meta.TableMeta;
import edu.sustech.cs307.record.RID;
import edu.sustech.cs307.record.Record;
import edu.sustech.cs307.record.RecordFileHandle;
import edu.sustech.cs307.system.DBManager;
import edu.sustech.cs307.tuple.TableTuple;
import edu.sustech.cs307.tuple.Tuple;
import edu.sustech.cs307.value.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * 索引扫描算子：等值查询时走 B+ 树，直接拿到匹配行的 RID 列表，
 * 再按 RID 去表里取记录，避免全表扫描。
 */
public class IndexScanOperator implements PhysicalOperator {

    private final String tableName;
    private final DBManager dbManager;
    private final BPlusTree index;
    private final Value key;          // 等值查询的值，如 age = 19 里的 19

    private TableMeta tableMeta;
    private RecordFileHandle fileHandle;
    private List<RID> rids;
    private int pos;
    private TableTuple current;
    private boolean isOpen = false;

    public IndexScanOperator(String tableName, BPlusTree index, Value key, DBManager dbManager) {
        this.tableName = tableName;
        this.index = index;
        this.key = key;
        this.dbManager = dbManager;
    }

    @Override
    public void Begin() throws DBException {
        tableMeta = dbManager.getMetaManager().getTable(tableName);
        fileHandle = dbManager.getRecordManager().OpenFile(tableName);
        rids = index.searchAll(key);      // 走索引，一次拿到所有匹配行的位置
        pos = 0;
        isOpen = true;
    }

    @Override
    public boolean hasNext() {
        return isOpen && rids != null && pos < rids.size();
    }

    @Override
    public void Next() {
        if (!hasNext()) {
            current = null;
            return;
        }
        try {
            RID rid = rids.get(pos++);
            Record rec = fileHandle.GetRecord(rid);
            current = new TableTuple(tableName, tableMeta, rec, rid);
        } catch (DBException e) {
            e.printStackTrace();
            current = null;
        }
    }

    @Override
    public Tuple Current() {
        return current;
    }

    @Override
    public void Close() {
        if (!isOpen) {
            return;
        }
        try {
            dbManager.getRecordManager().CloseFile(fileHandle);
        } catch (DBException e) {
            e.printStackTrace();
        }
        fileHandle = null;
        isOpen = false;
    }

    @Override
    public ArrayList<ColumnMeta> outputSchema() {
        return tableMeta.columns_list;
    }
}
