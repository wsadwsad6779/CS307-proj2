package edu.sustech.cs307.logicalOperator.ddl;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.system.DBManager;

public class DescribeTableExecutor implements DMLExecutor {

    private final String tableName;
    private final DBManager dbManager;

    public DescribeTableExecutor(String tableName, DBManager dbManager) {
        this.tableName = tableName;
        this.dbManager = dbManager;
    }

    @Override
    public void execute() throws DBException {
        dbManager.descTable(tableName);
    }
}
