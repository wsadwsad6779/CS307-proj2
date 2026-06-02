package index;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.index.BPlusTree;
import edu.sustech.cs307.record.RID;
import edu.sustech.cs307.value.Value;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BPlusTreeTest {

    @Test
    void insertSearchAndPrint() throws DBException {
        BPlusTree tree = new BPlusTree();
        for (long i = 1; i <= 10; i++) {
            tree.insert(new Value(i), new RID((int) i, 0));
        }
        System.out.println("==== 插入 1..10 后的 B+ 树 ====");
        tree.printTree();
        for (long i = 1; i <= 10; i++) {
            assertEquals((int) i, tree.search(new Value(i)).pageNum);
        }
        assertNull(tree.search(new Value(999L)));

        tree.delete(new Value(3L), new RID(3, 0));
        tree.delete(new Value(4L), new RID(4, 0));
        tree.delete(new Value(5L), new RID(5, 0));
        System.out.println("==== 删除 3,4,5（借位/合并）后的 B+ 树 ====");
        tree.printTree();
        assertNull(tree.search(new Value(3L)));
        for (long i : new long[]{1, 2, 6, 7, 8, 9, 10}) {
            assertEquals((int) i, tree.search(new Value(i)).pageNum);
        }
    }

    @Test
    void nonUniqueKeySupportsMultipleRows() throws DBException {
        BPlusTree tree = new BPlusTree();
        // age=19 的 5 行（key 相同，RID 不同）
        tree.insert(new Value(19L), new RID(2, 0));
        tree.insert(new Value(19L), new RID(7, 0));
        tree.insert(new Value(19L), new RID(12, 0));
        tree.insert(new Value(18L), new RID(99, 0));   // 干扰：另一个 key

        List<RID> rows = tree.searchAll(new Value(19L));
        assertEquals(3, rows.size(), "age=19 应返回 3 行");

        // 删掉其中一行，其余仍在
        tree.delete(new Value(19L), new RID(7, 0));
        assertEquals(2, tree.searchAll(new Value(19L)).size());
        // 删光后该 key 消失
        tree.delete(new Value(19L), new RID(2, 0));
        tree.delete(new Value(19L), new RID(12, 0));
        assertTrue(tree.searchAll(new Value(19L)).isEmpty());
        // 别的 key 不受影响
        assertEquals(1, tree.searchAll(new Value(18L)).size());
    }

    @Test
    void randomizedAgainstMultimap() throws DBException {
        BPlusTree tree = new BPlusTree();
        Map<Long, Set<Integer>> model = new HashMap<>();   // key -> {pageNum}
        Random rnd = new Random(307);
        int pageCounter = 1;

        for (int step = 0; step < 5000; step++) {
            long key = rnd.nextInt(40);                     // key 范围小 → 大量重复
            if (rnd.nextBoolean()) {
                int page = pageCounter++;                   // 每次插入 RID 唯一
                tree.insert(new Value(key), new RID(page, 0));
                model.computeIfAbsent(key, k -> new HashSet<>()).add(page);
            } else if (model.containsKey(key)) {
                // 删掉该 key 的某一个具体 RID
                Integer page = model.get(key).iterator().next();
                tree.delete(new Value(key), new RID(page, 0));
                model.get(key).remove(page);
                if (model.get(key).isEmpty()) {
                    model.remove(key);
                }
            }
            // 抽查：tree.searchAll 的 RID 集合必须和 model 一致
            for (int q = 0; q < 4; q++) {
                long probe = rnd.nextInt(40);
                Set<Integer> got = new TreeSet<>();
                for (RID r : tree.searchAll(new Value(probe))) {
                    got.add(r.pageNum);
                }
                Set<Integer> exp = new TreeSet<>(model.getOrDefault(probe, Set.of()));
                assertEquals(exp, got, "step " + step + " key " + probe + " 的行集合不一致");
            }
        }
        System.out.println("==== 5000 步随机(含大量重复 key)，与 multimap 全程一致 ====");
    }
}
