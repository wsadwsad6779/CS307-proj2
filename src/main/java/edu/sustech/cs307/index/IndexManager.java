package edu.sustech.cs307.index;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IndexManager {

    private final Map<String, Map<String, BPlusTree>> tableIndexes = new HashMap<>();
    //tableIndexes: 表名 -> (列名 -> 该列的 B+ 树)，支持一张表多列索引、多张表多棵树。
    private final Map<String, String[]> nameToCol = new HashMap<>();
    //nameToCol:    索引名 -> [表名, 列名]，给 DROP INDEX / PRINT INDEX 按名字定位。
    /** 索引名是否已存在。 */
    public boolean hasIndex(String indexName) {
        return nameToCol.containsKey(indexName);
    }

    /** 登记一棵新建好的树。 */
    public void addIndex(String indexName, String table, String column, BPlusTree tree) {
        tableIndexes.computeIfAbsent(table, k -> new HashMap<>()).put(column, tree);//记录：这张表的这个列对应这棵树
        nameToCol.put(indexName, new String[]{table, column});//记录：这个索引名对应哪张表、哪一列。
    }

    /** 按索引名移除，返回 [表名, 列名]（不存在返回 null）。 */
    public String[] removeIndex(String indexName) {
        String[] tc = nameToCol.remove(indexName);
        if (tc != null) {
            Map<String, BPlusTree> m = tableIndexes.get(tc[0]);
            if (m != null) {
                m.remove(tc[1]);
            }
        }
        return tc;
    }

    /** 取某张表的所有索引（列名 -> 树），给 INSERT/DELETE 联动用。 */
    public Map<String, BPlusTree> getIndexesForTable(String table) {
        return tableIndexes.getOrDefault(table, Collections.emptyMap());
    }

    /** 按 表+列 反查索引名（给 EXPLAIN 显示用）。没有返回 null。 */
    public String getIndexNameByColumn(String table, String column) {
        for (Map.Entry<String, String[]> e : nameToCol.entrySet()) {
            if (e.getValue()[0].equals(table) && e.getValue()[1].equals(column)) {
                return e.getKey();
            }
        }
        return null;
    }

    /** 按索引名取树（给 PRINT INDEX 用）。 */
    public BPlusTree getIndexByName(String indexName) {
        String[] tc = nameToCol.get(indexName);
        if (tc == null) {
            return null;
        }
        Map<String, BPlusTree> m = tableIndexes.get(tc[0]);
        return m == null ? null : m.get(tc[1]);
    }
}
