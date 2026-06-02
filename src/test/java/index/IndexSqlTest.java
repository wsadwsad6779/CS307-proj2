package index;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.logicalOperator.LogicalOperator;
import edu.sustech.cs307.meta.MetaManager;
import edu.sustech.cs307.optimizer.LogicalPlanner;
import edu.sustech.cs307.optimizer.PhysicalPlanner;
import edu.sustech.cs307.physicalOperator.PhysicalOperator;
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
