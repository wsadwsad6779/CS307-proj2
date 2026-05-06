package edu.sustech.cs307.logicalOperator.ddl;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.logicalOperator.LogicalOperator;
import edu.sustech.cs307.optimizer.LogicalPlanner;
import edu.sustech.cs307.system.DBManager;
import net.sf.jsqlparser.statement.ExplainStatement;
import org.pmw.tinylog.Logger;

public class ExplainExecutor implements DMLExecutor {

    private final ExplainStatement explainStatement;
    private final DBManager dbManager;

    public ExplainExecutor(ExplainStatement explainStatement, DBManager dbManager) {
        this.explainStatement = explainStatement;
        this.dbManager = dbManager;
    }

    @Override
    public void execute() throws DBException {
        // 获取EXPLAIN后面的SQL语句，解析为逻辑算子树并输出
        String innerSql = explainStatement.getStatement().toString();
        LogicalOperator operator = LogicalPlanner.resolveAndPlan(dbManager, innerSql);
        if (operator != null) {
            Logger.info(operator);
        }
    }
}
