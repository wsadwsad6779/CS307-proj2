package edu.sustech.cs307.system;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.exception.ExceptionTypes;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.meta.MetaManager;
import edu.sustech.cs307.meta.TableMeta;
import edu.sustech.cs307.storage.BufferPool;
import edu.sustech.cs307.storage.DiskManager;
import edu.sustech.cs307.storage.replacer.ClockReplacer;
import edu.sustech.cs307.storage.replacer.PageReplacer;
import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.function.IntFunction;

public class DBManager {
    private final MetaManager metaManager;
    /* --- --- --- */
    private final DiskManager diskManager;
    private final BufferPool bufferPool;
    private final RecordManager recordManager;
    private TransactionManager transactionManager;
    private final IntFunction<PageReplacer> replacerFactory;

    public DBManager(DiskManager diskManager, BufferPool bufferPool, RecordManager recordManager,
                     MetaManager metaManager) {
        this(diskManager, bufferPool, recordManager, metaManager, null, ClockReplacer::new);
    }

    public DBManager(DiskManager diskManager, BufferPool bufferPool, RecordManager recordManager,
                     MetaManager metaManager, TransactionManager transactionManager,
                     IntFunction<PageReplacer> replacerFactory) {
        this.diskManager = diskManager;
        this.bufferPool = bufferPool;
        this.recordManager = recordManager;
        this.metaManager = metaManager;
        this.replacerFactory = replacerFactory;
        this.transactionManager = transactionManager == null ? new TransactionManager(this) : transactionManager;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public BufferPool getBufferPool() {
        return bufferPool;
    }

    public RecordManager getRecordManager() {
        return recordManager;
    }

    public DiskManager getDiskManager() {
        return diskManager;
    }

    public MetaManager getMetaManager() {
        return metaManager;
    }

    public boolean isDirExists(String dir) {
        File file = new File(dir);
        return file.exists() && file.isDirectory();
    }

    /**
     * Displays a formatted table listing all available tables in the database.
     * The output is presented in a bordered ASCII table format with centered table
     * names.
     * Each table name is displayed in a separate row within the ASCII borders.
     */
    public void showTables() {
        var tableNames = metaManager.getTableNames();
        Logger.info("|-----------|");
        Logger.info("|  Tables   |");
        Logger.info("|-----------|");
        for (String name : tableNames) {
            Logger.info("| " + StringUtils.center(name, 9) + " |");
        }
        Logger.info("|-----------|");
    }

    public void descTable(String table_name) throws DBException {
        TableMeta meta = metaManager.getTable(table_name);
        Logger.info("|-------------------------|");
        Logger.info("|    Field   |   Type     |");
        Logger.info("|-------------------------|");
        for (ColumnMeta col : meta.columns_list) {
            String typeStr = col.type.toString().toLowerCase();
            Logger.info("| " + StringUtils.center(col.name, 10) + " | " + StringUtils.center(typeStr, 10) + " |");
        }
        Logger.info("|-------------------------|");
    }

    /**
     * Creates a new table in the database with specified name and column metadata.
     * This method sets up both the table metadata and the physical storage
     * structure.
     *
     * @param table_name The name of the table to be created
     * @param columns    List of column metadata defining the table structure
     * @throws DBException If there is an error during table creation
     */
    public void createTable(String table_name, ArrayList<ColumnMeta> columns) throws DBException {
        TableMeta tableMeta = new TableMeta(
                table_name, columns);
        metaManager.createTable(tableMeta);
        String table_folder = String.format("%s/%s", diskManager.getCurrentDir(), table_name);
        File file_folder = new File(table_folder);
        if (!file_folder.exists()) {
            file_folder.mkdirs();
        }
        int record_size = 0;
        for (var col : columns) {
            record_size += col.len;
        }
        String data_file = String.format("%s/%s", table_name, "data");
        recordManager.CreateFile(data_file, record_size);
    }

    /**
     * Drops a table from the database by removing its metadata and associated
     * physical files.
     *
     * @param table_name The name of the table to be dropped
     * @throws DBException If the table does not exist or encounters IO
     *                     errors during deletion
     */
    public void dropTable(String table_name) throws DBException {
        // 检查表是否存在，不存在则记录日志后返回
        if (!metaManager.getTableNames().contains(table_name)) {
            Logger.warn("Table '{}' does not exist, nothing to drop.", table_name);
            return;
        }

        String data_file = String.format("%s/%s", table_name, "data");

        // 1. 刷新缓冲池中该表的所有页面到磁盘
        bufferPool.FlushAllPages(data_file);

        // 2. 从缓冲池中彻底移除该文件的所有页面缓存（防止残留）
        bufferPool.removePagesByFilename(data_file);

        // 3. 从磁盘管理器中删除数据文件（同时从 filePages 移除并持久化）
        diskManager.DeleteFile(data_file);

        // 4. 删除表目录（包括可能残留的任何文件）
        String table_folder = String.format("%s/%s", diskManager.getCurrentDir(), table_name);
        File file_folder = new File(table_folder);
        if (file_folder.exists()) {
            deleteDirectory(file_folder);
        }

        // 5. 删除元数据并持久化
        metaManager.dropTable(table_name);

        Logger.info("Table '{}' dropped successfully.", table_name);
    }

    /**
     * Recursively deletes a directory and all its contents.
     * If the given file is a directory, it first deletes all its entries
     * recursively.
     * Finally deletes the file/directory itself.
     *
     * @param file The file or directory to be deleted
     * @throws IOException If deletion of any file or directory fails
     */
    private void deleteDirectory(File file) throws DBException {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteDirectory(entry);
                }
            }
        }
        if (!file.delete()) {
            throw new DBException(ExceptionTypes.BadIOError("File deletion failed: " + file.getAbsolutePath()));
        }
    }

    /**
     * Checks if a table exists in the database.
     *
     * @param table the name of the table to check
     * @return true if the table exists, false otherwise
     */
    public boolean isTableExists(String table) {
        return metaManager.getTableNames().contains(table);
    }

    /**
     * Adds a column to an existing table. Only metadata is updated.
     */
    public void addColumn(String tableName, ColumnMeta column) throws DBException {
        metaManager.addColumnInTable(tableName, column);
        Logger.info("Column '{}' added to table '{}'.", column.name, tableName);
    }

    /**
     * Drops a column from an existing table. Only metadata is updated.
     */
    public void dropColumn(String tableName, String columnName) throws DBException {
        metaManager.dropColumnInTable(tableName, columnName);
        Logger.info("Column '{}' dropped from table '{}'.", columnName, tableName);
    }

    /**
     * Closes the database manager and performs cleanup operations.
     * This method flushes all pages in the buffer pool, dumps disk manager
     * metadata,
     * and saves meta manager state to JSON format.
     *
     * @throws DBException if an error occurs during the closing process
     */
    public void closeDBManager() throws DBException {
        this.bufferPool.FlushAllPages(null);
        DiskManager.dump_disk_manager_meta(this.diskManager);
        this.metaManager.saveToJson();
    }

    public void beginTransaction() throws DBException {
        transactionManager.begin();
    }

    public void commitTransaction() throws DBException{
        transactionManager.commit();
    }

    public void persistRuntimeState() throws DBException {
        this.bufferPool.FlushAllPages("");
        DiskManager.dump_disk_manager_meta(this.diskManager);
        this.metaManager.saveToJson();
    }
}
