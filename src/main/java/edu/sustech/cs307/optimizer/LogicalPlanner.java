package edu.sustech.cs307.optimizer;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.parser.JSqlParser;
import net.sf.jsqlparser.statement.Commit;
import net.sf.jsqlparser.statement.ExplainStatement;
import net.sf.jsqlparser.statement.ShowStatement;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.schema.Column;

import edu.sustech.cs307.exception.ExceptionTypes;
import edu.sustech.cs307.logicalOperator.*;
import edu.sustech.cs307.system.DBManager;
import edu.sustech.cs307.logicalOperator.ddl.CreateTableExecutor;
import edu.sustech.cs307.logicalOperator.ddl.DescribeTableExecutor;
import edu.sustech.cs307.logicalOperator.ddl.ExplainExecutor;
import edu.sustech.cs307.logicalOperator.ddl.ShowDatabaseExecutor;
import edu.sustech.cs307.logicalOperator.ddl.AlterTableExecutor;
import edu.sustech.cs307.exception.DBException;
import net.sf.jsqlparser.statement.alter.Alter;

public class LogicalPlanner {
    private static final Pattern BEGIN_PATTERN = Pattern.compile("(?i)^BEGIN(?:\\s+(?:WORK|TRANSACTION))?$");
    private static final Pattern START_TRANSACTION_PATTERN = Pattern.compile("(?i)^START\\s+TRANSACTION$");
    private static final Pattern RELEASE_SAVEPOINT_PATTERN =
            Pattern.compile("(?i)^RELEASE(?:\\s+SAVEPOINT)?\\s+([A-Za-z_][A-Za-z0-9_]*)$");
    private static final Pattern DESCRIBE_PATTERN =
            Pattern.compile("(?i)^DESCRIBE\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*;?\\s*$");
    private static final Pattern SHOW_TABLES_PATTERN =
            Pattern.compile("(?i)^SHOW\\s+TABLES\\s*;?\\s*$");
    private static final Pattern SAVEPOINT_PATTERN =
            Pattern.compile("(?i)^SAVEPOINT\\s+([A-Za-z_][A-Za-z0-9_]*)$");
    private static final Pattern ROLLBACK_TO_PATTERN =
            Pattern.compile("(?i)^ROLLBACK\\s+TO(?:\\s+SAVEPOINT)?\\s+([A-Za-z_][A-Za-z0-9_]*)$");
    private static final Pattern ROLLBACK_PATTERN =
            Pattern.compile("(?i)^ROLLBACK(?:\\s+WORK|\\s+TRANSACTION)?$");

    public static LogicalOperator resolveAndPlan(DBManager dbManager, String sql) throws DBException {
        if (sql == null || sql.isBlank()) {
            return null;
        }
        if (handleManualTransactionCommand(dbManager, sql)) {
            return null;
        }
        // 处理 DESCRIBE 命令
        if (handleDescribeCommand(dbManager, sql)) {
            return null;
        }
        // 处理 SHOW TABLES 命令
        if (handleShowTablesCommand(dbManager, sql)) {
            return null;
        }
        JSqlParser parser = new CCJSqlParserManager();
        Statement stmt = null;
        try {
            stmt = parser.parse(new StringReader(sql));
        } catch (JSQLParserException e) {
            throw new DBException(ExceptionTypes.InvalidSQL(sql, e.getMessage()));
        }
        LogicalOperator operator = null;
        // Query
        if (stmt instanceof Select selectStmt) {
            operator = handleSelect(dbManager, selectStmt);
        } else if (stmt instanceof Insert insertStmt) {
            operator = handleInsert(dbManager, insertStmt);
        } else if (stmt instanceof Update updateStmt) {
            operator = handleUpdate(dbManager, updateStmt);
        } else if (stmt instanceof Delete deleteStmt) {
            operator = handleDelete(dbManager, deleteStmt);
        } else if (stmt instanceof Commit) {
            dbManager.commitTransaction();
            return null;
        }
        // DDL and functional
        else if (stmt instanceof CreateTable createTableStmt) {
            CreateTableExecutor createTable = new CreateTableExecutor(createTableStmt, dbManager, sql);
            createTable.execute();
            return null;
        } else if (stmt instanceof Drop dropStmt) {
            String tableName = dropStmt.getName().getName();
            dbManager.dropTable(tableName);
            return null;
        } else if (stmt instanceof ExplainStatement explainStatement) {
            ExplainExecutor explainExecutor = new ExplainExecutor(explainStatement, dbManager);
            explainExecutor.execute();
            return null;
        } else if (stmt instanceof ShowStatement showStatement) {
            ShowDatabaseExecutor showDatabaseExecutor = new ShowDatabaseExecutor(showStatement, dbManager);
            showDatabaseExecutor.execute();
            return null;
        } else if (stmt instanceof Alter alterStmt) {
            AlterTableExecutor alterExecutor = new AlterTableExecutor(alterStmt, dbManager);
            alterExecutor.execute();
            return null;
        } else {
            throw new DBException(ExceptionTypes.UnsupportedCommand((stmt.toString())));
        }
        return operator;
    }


    public static LogicalOperator handleSelect(DBManager dbManager, Select selectStmt) throws DBException {
        PlainSelect plainSelect = selectStmt.getPlainSelect();
        if (plainSelect.getFromItem() == null) {
            throw new DBException(ExceptionTypes.UnsupportedCommand((plainSelect.toString())));
        }
        LogicalOperator root = new LogicalTableScanOperator(plainSelect.getFromItem().toString(), dbManager);

        int depth = 0;
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                root = new LogicalJoinOperator(
                        root,
                        new LogicalTableScanOperator(join.getRightItem().toString(), dbManager),
                        join.getOnExpressions(),
                        depth);
                depth += 1;
            }
        }

        // 在 Join 之后应用 Filter 或 EXISTS
        if (plainSelect.getWhere() != null) {
            var whereExpr = plainSelect.getWhere();
            var unwrapped = unwrapParenthesis(whereExpr);
            if (unwrapped instanceof net.sf.jsqlparser.expression.NotExpression notExpr
                    && notExpr.getExpression() instanceof ExistsExpression existsExpr) {
                existsExpr.setNot(true);
                root = handleExists(dbManager, root, existsExpr);
            } else if (unwrapped instanceof ExistsExpression existsExpr) {
                root = handleExists(dbManager, root, existsExpr);
            } else {
                root = new LogicalFilterOperator(root, whereExpr);
            }
        }

        // 检测是否为 COUNT 操作
        if (isCountQuery(plainSelect.getSelectItems())) {
            root = new LogicalCountOperator(root, true);
            return root;
        }

        // 检测是否为聚合函数操作 (MAX/MIN without GROUP BY)
        if (isAggregateQuery(plainSelect.getSelectItems()) && 
            (plainSelect.getGroupBy() == null || plainSelect.getGroupBy().getGroupByExpressions().isEmpty())) {
            root = new LogicalGroupByOperator(root, new ArrayList<>(), plainSelect.getSelectItems());
            return root;
        }

        // 处理 GROUP BY
        boolean hasGroupBy = plainSelect.getGroupBy() != null && 
            !plainSelect.getGroupBy().getGroupByExpressions().isEmpty();
        if (hasGroupBy) {
            root = new LogicalGroupByOperator(root, 
                plainSelect.getGroupBy().getGroupByExpressions(), 
                plainSelect.getSelectItems());
        } else {
            root = new LogicalProjectOperator(root, plainSelect.getSelectItems());
            // 验证投影列是否存在（非 GROUP BY 才验证）
            validateProjectColumns(dbManager, plainSelect);
        }
        // 验证投影列是否存在
        validateProjectColumns(dbManager, plainSelect);

        // 处理 ORDER BY
        if (plainSelect.getOrderByElements() != null && !plainSelect.getOrderByElements().isEmpty()) {
            root = new LogicalOrderByOperator(root, plainSelect.getOrderByElements());
        }

        return root;
    }

    /**
     * 判断是否为聚合查询 (MAX/MIN 不含 COUNT, COUNT 已在前置检查中处理)
     */

    /**
     * 判断是否为聚合查询 (MAX/MIN 不含 COUNT, COUNT 已在前置检查中处理)
     */
    private static boolean isAggregateQuery(List<SelectItem<?>> selectItems) {
        for (SelectItem<?> item : selectItems) {
            if (item.getExpression() instanceof Function func) {
                String name = func.getName().toUpperCase();
                if (name.equals("MAX") || name.equals("MIN")) return true;
            }
        }
        return false;
    }

    /**
     * 验证 SELECT 投影列在表中是否存在。
     * 支持多表 JOIN 验证和聚合函数跳过。
     */
    private static void validateProjectColumns(DBManager dbManager, PlainSelect plainSelect) throws DBException {
        // 收集所有涉及的表名
        java.util.Set<String> tableNames = new java.util.LinkedHashSet<>();
        tableNames.add(plainSelect.getFromItem().toString());
        if (plainSelect.getJoins() != null) {
            for (Join j : plainSelect.getJoins()) {
                tableNames.add(j.getRightItem().toString());
            }
        }
        for (SelectItem<?> item : plainSelect.getSelectItems()) {
            // 跳过 * 和聚合函数
            if (item.getExpression() instanceof AllColumns) continue;
            if (item.getExpression() instanceof Function) continue;
            if (item.getExpression() instanceof Column column) {
                String colName = column.getColumnName();
                String colTable = column.getTableName();
                boolean found = false;
                for (String tn : tableNames) {
                    // 如果指定了表名前缀，只在该表中查找
                    if (colTable != null && !colTable.isEmpty() && !colTable.equals(tn)) continue;
                    try {
                        var meta = dbManager.getMetaManager().getTable(tn);
                        if (meta.getColumnMeta(colName) != null) { found = true; break; }
                    } catch (DBException ignored) { }
                }
                if (!found) {
                    throw new DBException(ExceptionTypes.ColumnDoesNotExist(colName));
                }
            }
            // 非 Column 非 Function 非 AllColumns 的表达式（如常量）跳过
        }
    }

    /**
     * 判断 SelectItems 中是否包含 COUNT 函数。
     */
    private static boolean isCountQuery(List<SelectItem<?>> selectItems) {
        if (selectItems == null || selectItems.size() != 1) {
            return false;
        }
        SelectItem<?> item = selectItems.get(0);
        if (item.getExpression() instanceof Function func) {
            return func.getName().equalsIgnoreCase("COUNT");
        }
        return false;
    }

    private static LogicalOperator handleInsert(DBManager dbManager, Insert insertStmt) {
        return new LogicalInsertOperator(insertStmt.getTable().getName(), insertStmt.getColumns(),
                insertStmt.getValues());
    }

    private static LogicalOperator handleUpdate(DBManager dbManager, Update updateStmt) throws DBException {
        LogicalOperator root = new LogicalTableScanOperator(updateStmt.getTable().getName(), dbManager);
        return new LogicalUpdateOperator(root, updateStmt.getTable().getName(), updateStmt.getUpdateSets(),
                updateStmt.getWhere());
    }

    private static LogicalOperator handleDelete(DBManager dbManager, Delete deleteStmt) throws DBException {
        LogicalOperator root = new LogicalTableScanOperator(deleteStmt.getTable().getName(), dbManager);
        return new LogicalDeleteOperator(root, deleteStmt.getTable().getName(), deleteStmt.getWhere());
    }

    private static LogicalOperator handleExists(DBManager dbManager, LogicalOperator outer,
                                                 ExistsExpression existsExpr) throws DBException {
        var right = existsExpr.getRightExpression();
        // JSqlParser 将 EXISTS (SELECT ...) 解析为 Parenthesis(PlainSelect)
        // 需要先解包 Parenthesis 层，才能取到 PlainSelect
        while (right instanceof net.sf.jsqlparser.expression.Parenthesis paren) {
            right = paren.getExpression();
        }
        if (right instanceof net.sf.jsqlparser.statement.select.Select selectExpr) {
            var subPlain = selectExpr.getPlainSelect();
            if (subPlain != null) {
                LogicalOperator subPlan = buildSubQueryPlan(dbManager, subPlain);
                return new LogicalExistsOperator(outer, subPlan, existsExpr.isNot(), subPlain.getWhere());
            }
        }
        if (right instanceof net.sf.jsqlparser.statement.select.PlainSelect subPlain) {
            LogicalOperator subPlan = buildSubQueryPlan(dbManager, subPlain);
            return new LogicalExistsOperator(outer, subPlan, existsExpr.isNot(), subPlain.getWhere());
        }
        // 其他复杂子查询（如 SetOperationList 等）暂不处理，回退为全表过滤
        return new LogicalFilterOperator(outer, existsExpr);
    }

    private static LogicalOperator buildSubQueryPlan(DBManager dbManager,
                                                      net.sf.jsqlparser.statement.select.PlainSelect subPlain)
            throws DBException {
        // 只做全表扫描，WHERE 条件在 ExistsOperator 中通过 JoinTuple 处理
        return new LogicalTableScanOperator(subPlain.getFromItem().toString(), dbManager);
    }

    private static boolean handleDescribeCommand(DBManager dbManager, String sql) throws DBException {
        Matcher m = DESCRIBE_PATTERN.matcher(sql.trim());
        if (m.matches()) {
            DescribeTableExecutor executor = new DescribeTableExecutor(m.group(1), dbManager);
            executor.execute();
            return true;
        }
        return false;
    }

    private static boolean handleShowTablesCommand(DBManager dbManager, String sql) throws DBException {
        Matcher m = SHOW_TABLES_PATTERN.matcher(sql.trim());
        if (m.matches()) {
            dbManager.showTables();
            return true;
        }
        return false;
    }

    private static net.sf.jsqlparser.expression.Expression unwrapParenthesis(
            net.sf.jsqlparser.expression.Expression expr) {
        while (expr instanceof net.sf.jsqlparser.expression.Parenthesis paren) {
            expr = paren.getExpression();
        }
        return expr;
    }

    private static String normalizeSql(String sql) {
        String normalizedSql = sql == null ? "" : sql.trim();
        while (normalizedSql.endsWith(";")) {
            normalizedSql = normalizedSql.substring(0, normalizedSql.length() - 1).trim();
        }
        return normalizedSql;
    }

    private static boolean handleManualTransactionCommand(DBManager dbManager, String sql) throws DBException {
        String normalizedSql = normalizeSql(sql);
        if (BEGIN_PATTERN.matcher(normalizedSql).matches() || START_TRANSACTION_PATTERN.matcher(normalizedSql).matches()) {
            dbManager.beginTransaction();
            return true;
        }

        Matcher sp = SAVEPOINT_PATTERN.matcher(normalizedSql);
        if (sp.matches()) {
            dbManager.savepoint(sp.group(1));
            return true;
        }

        Matcher rbTo = ROLLBACK_TO_PATTERN.matcher(normalizedSql);
        if (rbTo.matches()) {
            dbManager.rollbackToSavepoint(rbTo.group(1));
            return true;
        }

        Matcher rel = RELEASE_SAVEPOINT_PATTERN.matcher(normalizedSql);
        if (rel.matches()) {
            dbManager.releaseSavepoint(rel.group(1));
            return true;
        }

        if (ROLLBACK_PATTERN.matcher(normalizedSql).matches()) {
            dbManager.rollbackTransaction();
            return true;
        }

        return false;
    }


}
