package edu.sustech.cs307.system;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.exception.ExceptionTypes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;


public class TransactionManager {

    private final DBManager dbManager;
    public boolean inTransaction = false;
    List<Savepoint> savepoints = new ArrayList<>();

    public TransactionManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    private void pushSavepoint(String name) throws DBException {
        Path snapshotDir = createSnapshot();
        Map<String,Integer> filePagesCopy=
                new HashMap<>(dbManager.getDiskManager().filePages);
        savepoints.add(new Savepoint(name,snapshotDir,filePagesCopy));
    }



    public void begin() throws DBException {
        if (inTransaction) {//报错,别再开一个
            throw new
                    DBException(ExceptionTypes.TransactionAlreadyActive());
        }
        pushSavepoint(null);
        inTransaction=true;
    }


    public void commit() throws DBException {
        if (!inTransaction) {//报错,别再开一个
          return;
        }
        dbManager.persistRuntimeState();
        savepoints.clear();
        inTransaction=false;
    }


    public void rollback() throws DBException {
        if (!inTransaction) {
          return;
        }
        restoreFromSnapshot(savepoints.get(0));
        savepoints.clear();
        inTransaction=false;
    }


    public void savepoint(String savepointName) throws DBException {
        if (!inTransaction) {
            throw new DBException(ExceptionTypes.TransactionRequired());
        }
        pushSavepoint(savepointName);
    }

    public void rollbackToSavepoint(String savepointName) throws DBException {
        if (!inTransaction) {
            throw new DBException(ExceptionTypes.TransactionRequired());
        }
        var temp =findSavepoint(savepointName);
        if(temp==-1){
            throw new DBException(ExceptionTypes.SavepointDoesNotExist(savepointName));
        }
        restoreFromSnapshot(savepoints.get(temp));
        savepoints.subList(temp + 1, savepoints.size()).clear();
    }
    public void releaseSavepoint(String savepointName) throws DBException {
        if (!inTransaction) {
            throw new
                    DBException(ExceptionTypes.TransactionRequired());
        }
        int idx = findSavepoint(savepointName);
        if (idx < 0) {
            throw new DBException(ExceptionTypes.SavepointDoesNotExist(savepointName));
        }
        savepoints.subList(idx, savepoints.size()).clear();
    }

    private Path createSnapshot() throws DBException {
        dbManager.persistRuntimeState();
        Path snapshotDir;
        try {
            snapshotDir = Files.createTempDirectory("cs307-txn-");
            copyDirectoryContents(getDbRoot(), snapshotDir);
        } catch (IOException e) {
            throw new DBException(ExceptionTypes.BadIOError(e.getMessage()));
        }
        return snapshotDir;
    }

    private void restoreFromSnapshot(Savepoint sp) throws DBException {
        dbManager.getBufferPool().discardAllPages();
        try {
            Path root = getDbRoot();
            deleteDirectoryContents(root);
            copyDirectoryContents(sp.snapshotDir,root);
        }catch (IOException e){
            throw new DBException(ExceptionTypes.BadIOError(e.getMessage()));
        }
        // 4. 重新加载表元数据
        dbManager.getMetaManager().reload();
        // 5. 还原 filePages（清空再灌入冻结副本）
        dbManager.getDiskManager().filePages.clear();
        dbManager.getDiskManager().filePages.putAll(sp.filePages);
    }
    private int findSavepoint(String name) {
        for (int i = savepoints.size() - 1; i >= 0; i--) {
            if (name.equals(savepoints.get(i).name)) {
                return i;
            }
        }
        return -1;   // 没找到
    }
    private void deleteDirectoryContents(Path root) throws IOException {
        if (!Files.exists(root)) return;
        List<Path> all;
        try (var paths = Files.walk(root)) {
            // 倒序：先删子文件再删父目录
            all = paths.sorted(java.util.Comparator.reverseOrder()).toList();
        }
        for (Path p : all) {
            if (!p.equals(root)) {   // 保留根目录本身，只清里面
                Files.delete(p);
            }
        }
    }
    private Path getDbRoot() {
        return Path.of(dbManager.getDiskManager().getCurrentDir());
    }

    private void copyDirectoryContents(Path sourceRoot, Path targetRoot) throws IOException {
        if (!Files.exists(sourceRoot)) {
            Files.createDirectories(targetRoot);
            return;
        }
        Files.createDirectories(targetRoot);
        try (var paths = Files.walk(sourceRoot)) {
            for (Path source : paths.toList()) {
                Path relative = sourceRoot.relativize(source);
                Path target = targetRoot.resolve(relative);
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }
    private static class Savepoint {
        String name;
        Path snapshotDir;
        Map<String, Integer> filePages;
        Savepoint(String name, Path snapshotDir,Map<String,Integer> filePages){
            this.name=name;
            this.snapshotDir=snapshotDir;
            this.filePages=filePages;
        }
    }
}
