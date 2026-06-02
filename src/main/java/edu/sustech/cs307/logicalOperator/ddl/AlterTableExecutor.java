package edu.sustech.cs307.logicalOperator.ddl;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.exception.ExceptionTypes;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.system.DBManager;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.value.ValueType;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import org.pmw.tinylog.Logger;

import java.util.List;

public class AlterTableExecutor implements DMLExecutor {
    private final Alter alterStmt;
    private final DBManager dbManager;

    public AlterTableExecutor(Alter alterStmt, DBManager dbManager) {
        this.alterStmt = alterStmt;
        this.dbManager = dbManager;
    }

    @Override
    public void execute() throws DBException {
        String tableName = alterStmt.getTable().getName();
        List<AlterExpression> alterExps = alterStmt.getAlterExpressions();
        if (alterExps == null || alterExps.isEmpty()) {
            throw new DBException(ExceptionTypes.InvalidSQL("ALTER TABLE", "No operations specified"));
        }

        for (AlterExpression exp : alterExps) {
            if (exp.getOperation() == null) {
                throw new DBException(ExceptionTypes.UnsupportedCommand("ALTER TABLE with null operation"));
            }
            String opType = exp.getOperation().name().toUpperCase();
            switch (opType) {
                case "ADD" -> {
                    String colName = exp.getColumnName();
                    // 从 AlterExpression 中解析列类型
                    // JSqlParser 有时会把 "列名 类型" 一起放进 colDataTypeList
                    String colType = null;
                    var dtList = exp.getColDataTypeList();
                    if (dtList != null && !dtList.isEmpty()) {
                        colType = dtList.get(0).toString();
                    }
                    // 如果 colType 包含空格（如 "email varchar"），取最后一个空格后的词作为类型
                    if (colType != null && colType.contains(" ")) {
                        String[] parts = colType.split("\\s+");
                        colType = parts[parts.length - 1];
                        // 如果 colName 为空或与类型混在一起，从第一部分提取列名
                        if (colName == null || colName.isEmpty() || parts.length > 1) {
                            colName = parts[0];
                        }
                    }
                    if (colType == null || colType.isEmpty()) {
                        colType = "char";
                    }
                    // 处理带参数的类型，如 varchar(20), char(100) 等
                    String colTypeLower = colType.toLowerCase().replaceAll("\\(.*\\)", "").trim();
                    ValueType vt = switch (colTypeLower) {
                        case "int" -> ValueType.INTEGER;
                        case "char", "varchar" -> ValueType.CHAR;
                        case "float", "double" -> ValueType.FLOAT;
                        default -> throw new DBException(ExceptionTypes.UnsupportedCommand("ALTER TABLE ADD " + colType));
                    };
                    if (colName == null || colName.isEmpty()) {
                        throw new DBException(ExceptionTypes.InvalidSQL("ALTER TABLE ADD", "No column name specified"));
                    }
                    int newColSize = vt == ValueType.CHAR ? Value.CHAR_SIZE :
                                     vt == ValueType.INTEGER ? Value.INT_SIZE : Value.FLOAT_SIZE;
                    var tableMeta = dbManager.getMetaManager().getTable(tableName);
                    int maxOffset = 0;
                    for (var cm : tableMeta.columns_list) {
                        int end = cm.getOffset() + cm.getLen();
                        if (end > maxOffset) maxOffset = end;
                    }
                    ColumnMeta newCol = new ColumnMeta(tableName, colName, vt, newColSize, maxOffset);
                    newCol.displayType = colTypeLower;
                    dbManager.addColumn(tableName, newCol);   // addColumn 内部已打日志
                }
                case "DROP" -> {
                    String colName = exp.getColumnName();
                    dbManager.dropColumn(tableName, colName);  // dropColumn 内部已打日志
                }
                default -> throw new DBException(
                    ExceptionTypes.UnsupportedCommand("ALTER TABLE " + opType));
            }
        }
    }
}
