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
            String opType = exp.getOperation().name().toUpperCase();
            switch (opType) {
                case "ADD" -> {
                    String colName = exp.getColumnName();
                    // 从 AlterExpression 的 SQL 文本中解析列类型
                    String colType = "char";
                    var dtList = exp.getColDataTypeList();
                    if (dtList != null && !dtList.isEmpty()) {
                        colType = dtList.get(0).toString();
                    }
                    ValueType vt = switch (colType.toLowerCase()) {
                        case "int" -> ValueType.INTEGER;
                        case "char", "varchar" -> ValueType.CHAR;
                        case "float", "double" -> ValueType.FLOAT;
                        default -> throw new DBException(ExceptionTypes.UnsupportedCommand("ALTER TABLE ADD " + colType));
                    };
                    int newColSize = vt == ValueType.CHAR ? Value.CHAR_SIZE :
                                     vt == ValueType.INTEGER ? Value.INT_SIZE : Value.FLOAT_SIZE;
                    var tableMeta = dbManager.getMetaManager().getTable(tableName);
                    int maxOffset = 0;
                    for (var cm : tableMeta.columns_list) {
                        int end = cm.getOffset() + cm.getLen();
                        if (end > maxOffset) maxOffset = end;
                    }
                    ColumnMeta newCol = new ColumnMeta(tableName, colName, vt, newColSize, maxOffset);
                    dbManager.addColumn(tableName, newCol);
                    Logger.info("Column '{}' added to table '{}'.", colName, tableName);
                }
                case "DROP" -> {
                    String colName = exp.getColumnName();
                    dbManager.dropColumn(tableName, colName);
                    Logger.info("Column '{}' dropped from table '{}'.", colName, tableName);
                }
                default -> throw new DBException(
                    ExceptionTypes.UnsupportedCommand("ALTER TABLE " + opType));
            }
        }
    }
}
