package edu.sustech.cs307.index;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.record.RID;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.value.ValueComparer;

import java.util.ArrayList;
import java.util.List;

/**
 * 内存 B+ 树索引。
 * key = 被索引列的值(Value)，value = 该行数据的位置(RID)。
 *
 * 设计要点：
 * - 所有真实数据(RID)只存在叶子层；内部节点只放导航用的 key。
 * - 叶子之间用 next 指针串成链表，方便范围扫描。
 * - 比较大小一律用 ValueComparer.compare(a, b)，因为 Value 没实现 Comparable。
 */
public class BPlusTree {

    /** 阶。每个节点最多 ORDER-1 个 key；超了就分裂。用小一点方便演示分裂。 */
    private static final int ORDER = 4;
    private static final int MAX_KEYS = ORDER - 1;   // 一个节点最多放几个 key

    /** B+ 树的节点。用 isLeaf 区分内部节点和叶子节点。 */
    static class Node {
        boolean isLeaf;
        List<Value> keys = new ArrayList<>();

        // 仅内部节点使用：children.size() == keys.size() + 1
        List<Node> children = new ArrayList<>();

        // 仅叶子节点使用：values 与 keys 一一对应
        List<RID> values = new ArrayList<>();
        Node next;   // 指向右边相邻的叶子，串成链

        Node(boolean isLeaf) {
            this.isLeaf = isLeaf;
        }
    }

    private Node root;

    public BPlusTree() {
        // 初始时只有一个空叶子当根
        this.root = new Node(true);
    }

    /** 比较两个 key 的大小，封装 ValueComparer，-1/0/1。 */
    private int cmp(Value a, Value b) throws DBException {
        return ValueComparer.compare(a, b);
    }

    // ====== 下面是要逐步实现的核心方法，先留空 ======

    /** 查找 key 对应的 RID，找不到返回 null。 */
    public RID search(Value key) throws DBException {
        // TODO: 你来写
        // 1. cur 从 root 出发，while(!cur.isLeaf) 时用 cmp 选 child 下降
        // 2. 到叶子后扫 keys 找 cmp==0 的，返回对应 values；没有返回 null
        var cur = root;
        while (!cur.isLeaf) {
            int index = 0;
            while (index < cur.keys.size() && cmp(key, cur.keys.get(index)) >= 0) {//叶子分裂时中间key复制到右边那片叶子
                index++;
            }
            cur = cur.children.get(index);
        }
        int index = 0;
        for (var i : cur.keys) {
            if (cmp(key, i) == 0) {
                return cur.values.get(index);
            }
            index++;
        }
        return null;
    }

    /** 插入一对 (key, rid)。叶子塞满时分裂，必要时树长高。 */
    public void insert(Value key, RID rid) throws DBException {
        // TODO
    }

    /** 删除 key。叶子太空时借/合并。 */
    public void delete(Value key) throws DBException {
        // TODO
    }

    /** 按层打印每个节点（答辩演示用）。 */
    public void printTree() {
        // TODO
    }
}
