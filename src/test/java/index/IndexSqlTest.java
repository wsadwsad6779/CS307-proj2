package index;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.logicalOperator.LogicalOperator;
import edu.sustech.cs307.meta.MetaManager;
import edu.sustech.cs307.optimizer.LogicalPlanner;
import edu.sustech.cs307.optimizer.PhysicalPlanner;
import edu.sustech.cs307.physicalOperator.PhysicalOperator;
import edu.sustech.cs307.physicalOperator.IndexScanOperator;
import edu.sustech.cs307.storage.BufferPool;
import edu.sustech.cs307.storage.DiskManager;
import edu.sustech.cs307.storage.replacer.ClockReplacer;
import edu.sustech.cs307.storage.replacer.PageReplacer;
import edu.sustech.cs307.system.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexSqlTest {

    @TempDir
    Path tempDir;

    @Test
    void createIndexInsertDeletePrint() throws DBException {
        DBManager dbManager = buildDbManager();

        run(dbManager, "CREATE TABLE t (id int)");
        for (int i = 1; i <= 8; i++) {
            run(dbManager, "INSERT INTO t (id) VALUES (" + i + ")");
        }

        System.out.println("==== CREATE INDEX ix ON t(id)（基于已有 8 行建树）====");
        run(dbManager, "CREATE INDEX ix ON t (id)");
        run(dbManager, "PRINT INDEX ix");

        // 重复创建同名索引应报错，且程序继续可用
        org.junit.jupiter.api.Assertions.assertThrows(DBException.class,
                () -> run(dbManager, "CREATE INDEX ix ON t (id)"));

        System.out.println("==== 再 INSERT 9,10（树应动态长大）====");
        run(dbManager, "INSERT INTO t (id) VALUES (9)");
        run(dbManager, "INSERT INTO t (id) VALUES (10)");
        run(dbManager, "PRINT INDEX ix");

        System.out.println("==== DELETE id=3,4,5（树应同步删除）====");
        run(dbManager, "DELETE FROM t WHERE id = 3");
        run(dbManager, "DELETE FROM t WHERE id = 4");
        run(dbManager, "DELETE FROM t WHERE id = 5");
        run(dbManager, "PRINT INDEX ix");

        System.out.println("==== DROP INDEX ix ====");
        run(dbManager, "DROP INDEX ix");
        run(dbManager, "PRINT INDEX ix");
    }

    @Test
    void indexScanReturnsAllMatchingRows() throws DBException {
        DBManager dbManager = buildDbManager();
        run(dbManager, "CREATE TABLE p (id int, age int)");
        // age=19 有 3 行(id=2,7,12)，age=18 有 2 行
        int[][] rows = {{2, 19}, {7, 19}, {12, 19}, {3, 18}, {5, 18}, {9, 20}};
        for (int[] r : rows) {
            run(dbManager, "INSERT INTO p (id, age) VALUES (" + r[0] + ", " + r[1] + ")");
        }
        run(dbManager, "CREATE INDEX ix_age ON p (age)");

        // 1) 该查询的物理计划应是 IndexScanOperator（证明走了索引）
        LogicalOperator logical = LogicalPlanner.resolveAndPlan(dbManager, "select * from p where age = 19");
        LogicalOperator filter = logical.getChildren().get(0);   // project -> filter
        PhysicalOperator phys = PhysicalPlanner.generateOperator(dbManager, filter);
        org.junit.jupiter.api.Assertions.assertTrue(phys instanceof IndexScanOperator,
                "where age=19 应走 IndexScan，实际是 " + phys.getClass().getSimpleName());

        // 2) 结果正确：返回 id 集合 = {2,7,12}
        java.util.Set<Long> ids = selectIds(dbManager, "select id from p where age = 19");
        assertEquals(java.util.Set.of(2L, 7L, 12L), ids);

        // 3) 插入一行 age=19 后，再查应多一行(动态联动)
        run(dbManager, "INSERT INTO p (id, age) VALUES (37, 19)");
        assertEquals(java.util.Set.of(2L, 7L, 12L, 37L),
                selectIds(dbManager, "select id from p where age = 19"));

        // 4) 删除一行后，再查应少一行
        run(dbManager, "DELETE FROM p WHERE id = 7");
        assertEquals(java.util.Set.of(2L, 12L, 37L),
                selectIds(dbManager, "select id from p where age = 19"));
    }

    /** 跑一条 SELECT，收集第一列(id)的值。 */
    private java.util.Set<Long> selectIds(DBManager dbManager, String sql) throws DBException {
        LogicalOperator logical = LogicalPlanner.resolveAndPlan(dbManager, sql);
        PhysicalOperator op = PhysicalPlanner.generateOperator(dbManager, logical);
        java.util.Set<Long> ids = new java.util.HashSet<>();
        op.outputSchema();   // 模拟 DBEntry：Begin() 之前先取 schema，防止 tableMeta 未初始化
        op.Begin();
        while (op.hasNext()) {
            op.Next();
            var tuple = op.Current();
            if (tuple != null) {
                ids.add((Long) tuple.getValues()[0].value);
            }
        }
        op.Close();
        return ids;
    }

    private DBManager buildDbManager() throws DBException {
        HashMap<String, Integer> fileOffsets = new HashMap<>();
        DiskManager diskManager = new DiskManager(tempDir.toString(), fileOffsets);
        IntFunction<PageReplacer> replacerFactory = ClockReplacer::new;
        BufferPool bufferPool = new BufferPool(16, diskManager, replacerFactory.apply(16));
        RecordManager recordManager = new RecordManager(diskManager, bufferPool);
        MetaManager metaManager = new MetaManager(tempDir.resolve("meta").toString());
        DBManager dbManager = new DBManager(diskManager, bufferPool, recordManager, metaManager, null,
                replacerFactory);
        dbManager.setTransactionManager(new TransactionManager(dbManager));
        return dbManager;
    }

    private void run(DBManager dbManager, String sql) throws DBException {
        LogicalOperator logicalOperator = LogicalPlanner.resolveAndPlan(dbManager, sql);
        if (logicalOperator == null) {
            return;
        }
        PhysicalOperator op = PhysicalPlanner.generateOperator(dbManager, logicalOperator);
        op.Begin();
        while (op.hasNext()) {
            op.Next();
            op.Current();
        }
        op.Close();
        dbManager.getBufferPool().FlushAllPages("");
    }
}
