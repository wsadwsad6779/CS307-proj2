package edu.sustech.cs307.logicalOperator.ddl;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.logicalOperator.LogicalFilterOperator;
import edu.sustech.cs307.logicalOperator.LogicalOperator;
import edu.sustech.cs307.logicalOperator.LogicalProjectOperator;
import edu.sustech.cs307.logicalOperator.LogicalTableScanOperator;
import edu.sustech.cs307.optimizer.LogicalPlanner;
import edu.sustech.cs307.system.DBManager;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
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
            Logger.info(explainPlan(operator));
        }
    }

    /**
     * 渲染执行计划。若是 “被索引列 = 值” 的过滤，把 Filter+TableScan 显示成 IndexScanOperator，
     * 反映优化器实际会走索引扫描；否则按原逻辑计划输出。
     */
    private String explainPlan(LogicalOperator root) {
        LogicalOperator node = root;
        String projectHeader = null;
        if (node instanceof LogicalProjectOperator) {
            projectHeader = node.toString().split("\\R")[0];   // 取 "ProjectOperator(...)" 这一行
            node = node.getChildren().get(0);
        }
        if (node instanceof LogicalFilterOperator filter
                && filter.getChild() instanceof LogicalTableScanOperator scan) {
            String indexName = indexedColumnIndexName(scan.getTableName(), filter.getWhereExpr());
            if (indexName != null) {
                String scanLine = "IndexScanOperator(table=" + scan.getTableName()
                        + ", index=" + indexName
                        + ", condition=" + filter.getWhereExpr() + ")";
                return projectHeader != null ? projectHeader + "\n    └── " + scanLine : scanLine;
            }
        }
        return root.toString();   // 其它情况：原样输出逻辑计划
    }

    /** 若 where 是 “某列 = 值” 且该列在该表上有索引，返回索引名；否则 null。 */
    private String indexedColumnIndexName(String table, Expression where) {
        if (!(where instanceof EqualsTo eq)) {
            return null;
        }
        Column col = null;
        if (eq.getLeftExpression() instanceof Column c) {
            col = c;
        } else if (eq.getRightExpression() instanceof Column c) {
            col = c;
        }
        if (col == null) {
            return null;
        }
        if (!dbManager.getIndexManager().getIndexesForTable(table).containsKey(col.getColumnName())) {
            return null;
        }
        return dbManager.getIndexManager().getIndexNameByColumn(table, col.getColumnName());
    }
}
