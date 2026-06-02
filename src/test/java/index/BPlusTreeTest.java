package index;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.index.BPlusTree;
import edu.sustech.cs307.record.RID;
import edu.sustech.cs307.value.Value;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

        tree.delete(new Value(3L));
        tree.delete(new Value(4L));
        tree.delete(new Value(5L));
        System.out.println("==== 删除 3,4,5（借位/合并）后的 B+ 树 ====");
        tree.printTree();
        assertNull(tree.search(new Value(3L)));
        for (long i : new long[]{1, 2, 6, 7, 8, 9, 10}) {
            assertEquals((int) i, tree.search(new Value(i)).pageNum);
        }
    }

    @Test
    void deleteAllThenReusable() throws DBException {
        BPlusTree tree = new BPlusTree();
        for (long i = 1; i <= 50; i++) {
            tree.insert(new Value(i), new RID((int) i, 0));
        }
        for (long i = 1; i <= 50; i++) {
            tree.delete(new Value(i));
            assertNull(tree.search(new Value(i)));
        }
        // 全删空后仍能用
        tree.insert(new Value(42L), new RID(7, 0));
        assertEquals(7, tree.search(new Value(42L)).pageNum);
    }

    @Test
    void randomizedAgainstHashMap() throws DBException {
        BPlusTree tree = new BPlusTree();
        HashMap<Long, Integer> expected = new HashMap<>();   // key -> pageNum
        Random rnd = new Random(307);

        for (int step = 0; step < 5000; step++) {
            long key = rnd.nextInt(200);
            if (rnd.nextBoolean()) {
                int page = step + 1;
                tree.insert(new Value(key), new RID(page, 0));
                expected.put(key, page);
            } else {
                tree.delete(new Value(key));
                expected.remove(key);
            }
            // 每步随机抽查若干 key，B+ 树结果必须和 HashMap 一致
            for (int q = 0; q < 5; q++) {
                long probe = rnd.nextInt(200);
                RID rid = tree.search(new Value(probe));
                if (expected.containsKey(probe)) {
                    assertNotNull(rid, "step " + step + " key " + probe + " 应存在");
                    assertEquals((int) expected.get(probe), rid.pageNum);
                } else {
                    assertNull(rid, "step " + step + " key " + probe + " 应已删除");
                }
            }
        }
        System.out.println("==== 5000 步随机插入/删除，与 HashMap 全程一致 ====");
    }
}
