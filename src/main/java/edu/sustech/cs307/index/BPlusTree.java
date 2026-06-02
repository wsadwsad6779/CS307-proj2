package edu.sustech.cs307.index;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.record.RID;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.value.ValueComparer;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Objects;

/**
 * 内存 B+ 树索引。
 * key = 被索引列的值(Value)，value = 该行数据的位置(RID)。
 * 设计要点：
 * - 所有真实数据(RID)只存在叶子层；内部节点只放导航用的 key。
 * - 叶子之间用 next 指针串成链表，方便范围扫描。
 * - 比较大小一律用 ValueComparer.compare(a, b)，因为 Value 没实现 Comparable。
 */
public class BPlusTree {

    /** 阶。每个节点最多 ORDER-1 个 key；超了就分裂。用小一点方便演示分裂。 */
    private static final int ORDER = 4;
    private static final int MAX_KEYS = ORDER - 1;   // 一个节点最多放几个 key
    private static final int MIN_KEYS = MAX_KEYS / 2; // 非根节点最少 key 数(=1)，少于它就下溢

    /** B+ 树的节点。用 isLeaf 区分内部节点和叶子节点。 */
    static class Node {
        boolean isLeaf;
        List<Value> keys = new ArrayList<>();

        // 仅内部节点使用：children.size() == keys.size() + 1
        List<Node> children = new ArrayList<>();

        // 仅叶子节点使用：values 与 keys 一一对应。
        // 非唯一索引：一个 key 可能对应多行，所以每个 key 挂一串 RID。
        List<List<RID>> values = new ArrayList<>();
        Node next;   // 指向右边相邻的叶子，串成链

        Node(boolean isLeaf) {
            this.isLeaf = isLeaf;
        }
    }
    static class SplitResult {
        Value upKey;
        Node rightNode;
        SplitResult(Value upKey, Node rightNode) {
            this.upKey = upKey;//上推的那个 key(父节点要把它插进自己的 keys)
            this.rightNode = rightNode;//新生的右节点(父节点要把它挂进自己的 children)
        }
    }
    private Node root;

    public BPlusTree() {
        // 初始时只有一个空叶子当根
        this.root = new Node(true);
    }

    /** 比较两个 key 的大小，封装 ValueComparer，-1/0/1。  cmp(a, b) < 0   // a 比 b 小 */
    private int cmp(Value a, Value b) throws DBException {
        return ValueComparer.compare(a, b);
    }

    // ====== 下面是要逐步实现的核心方法，先留空 ======

    /**
     * 查找 key 对应的 RID，找不到返回 null。
     */
    public RID search(Value key) throws DBException {
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
                return cur.values.get(index).get(0);   // 该 key 的第一个 RID
            }
            index++;
        }
        return null;
    }

    /** 查找 key 对应的【所有】RID（非唯一索引：一个值可能对应多行）。找不到返回空列表。 */
    public List<RID> searchAll(Value key) throws DBException {
        var cur = root;
        while (!cur.isLeaf) {
            int index = 0;
            while (index < cur.keys.size() && cmp(key, cur.keys.get(index)) >= 0) {
                index++;
            }
            cur = cur.children.get(index);
        }
        for (int j = 0; j < cur.keys.size(); j++) {
            if (cmp(key, cur.keys.get(j)) == 0) {
                return new ArrayList<>(cur.values.get(j));   // 拷贝一份返回
            }
        }
        return new ArrayList<>();
    }

    /** 插入一对 (key, rid)。叶子塞满时分裂，必要时树长高。 */
    public void insert(Value key, RID rid) throws DBException {
        SplitResult r = insertInto(root, key, rid);
        if (r != null) {                       // 根分裂了
            Node newRoot = new Node(false);     // 新根是内部节点
            newRoot.keys.add(r.upKey);
            newRoot.children.add(root);          // 老根(左半)
            newRoot.children.add(r.rightNode);   // 新右半
            root = newRoot;                      // 树长高一层
        }
    }

    /** 删除某一行：从 key 的 RID 列表里移除指定 rid；该 key 没有行后整条删除并修复下溢。 */
    public void delete(Value key, RID rid) throws DBException {
        deleteFrom(root, key, rid);
        // 根变矮：根是内部节点且删到没 key、只剩一个孩子 → 那个孩子当新根
        if (!root.isLeaf && root.keys.isEmpty() && root.children.size() == 1) {
            root = root.children.get(0);
        }
    }

    private void deleteFrom(Node node, Value key, RID rid) throws DBException {
        if (node.isLeaf) {
            for (int j = 0; j < node.keys.size(); j++) {
                if (cmp(key, node.keys.get(j)) == 0) {
                    List<RID> ridList = node.values.get(j);
                    ridList.removeIf(r -> r.pageNum == rid.pageNum && r.slotNum == rid.slotNum);
                    if (ridList.isEmpty()) {       // 这个 key 已经没有任何行了，整条删掉
                        node.keys.remove(j);
                        node.values.remove(j);
                    }
                    return;
                }
            }
            return;   // 没找到
        }
        // 内部节点：下降到对应孩子
        int idx = 0;
        while (idx < node.keys.size() && cmp(key, node.keys.get(idx)) >= 0) {
            idx++;
        }
        Node child = node.children.get(idx);
        deleteFrom(child, key, rid);
        // 递归回来后，若该孩子下溢(key 数 < MIN_KEYS)，修复它
        if (child.keys.size() < MIN_KEYS) {
            fixUnderflow(node, idx);
        }
    }

    /** 修复 parent.children[idx] 的下溢：优先借，借不到就合并。 */
    private void fixUnderflow(Node parent, int idx) {
        Node child = parent.children.get(idx);
        Node left  = idx > 0 ? parent.children.get(idx - 1) : null;
        Node right = idx < parent.children.size() - 1 ? parent.children.get(idx + 1) : null;

        if (left != null && left.keys.size() > MIN_KEYS) {
            borrowFromLeft(parent, idx, child, left);
        } else if (right != null && right.keys.size() > MIN_KEYS) {
            borrowFromRight(parent, idx, child, right);
        } else if (left != null) {
            merge(parent, idx - 1);   // child 并入 left
        } else {
            merge(parent, idx);       // right 并入 child
        }
    }

    /** child 向左兄弟借一个(左兄最大的那个挪到 child 最前)。 */
    private void borrowFromLeft(Node parent, int idx, Node child, Node left) {
        if (child.isLeaf) {
            // 把 left 的最后一个 key/value 移到 child 开头
            child.keys.add(0, left.keys.remove(left.keys.size() - 1));
            child.values.add(0, left.values.remove(left.values.size() - 1));
            // 更新父节点路标 = child 的新首 key
            parent.keys.set(idx - 1, child.keys.get(0));
        } else {
            // 内部节点：通过父路标做"旋转"
            child.keys.add(0, parent.keys.get(idx - 1));                       // 父路标下来当 child 首 key
            child.children.add(0, left.children.remove(left.children.size() - 1)); // left 末孩子挪过来
            parent.keys.set(idx - 1, left.keys.remove(left.keys.size() - 1));  // left 末 key 上去当父路标
        }
    }

    /** child 向右兄弟借一个(右兄最小的那个挪到 child 末尾)。 */
    private void borrowFromRight(Node parent, int idx, Node child, Node right) {
        if (child.isLeaf) {
            child.keys.add(right.keys.remove(0));
            child.values.add(right.values.remove(0));
            parent.keys.set(idx, right.keys.get(0));   // 路标 = 右兄新的首 key
        } else {
            child.keys.add(parent.keys.get(idx));               // 父路标下来当 child 末 key
            child.children.add(right.children.remove(0));        // 右兄首孩子挪过来
            parent.keys.set(idx, right.keys.remove(0));          // 右兄首 key 上去当父路标
        }
    }

    /** 合并 parent.children[sepIdx] 和 [sepIdx+1]：右节点并入左节点，父删一个路标。 */
    private void merge(Node parent, int sepIdx) {
        Node left  = parent.children.get(sepIdx);
        Node right = parent.children.get(sepIdx + 1);
        if (left.isLeaf) {
            left.keys.addAll(right.keys);
            left.values.addAll(right.values);
            left.next = right.next;                 // 维护叶子链表
            // 叶子的父路标只是副本，直接丢弃
        } else {
            left.keys.add(parent.keys.get(sepIdx)); // 内部节点要把父路标拉下来
            left.keys.addAll(right.keys);
            left.children.addAll(right.children);
        }
        parent.keys.remove(sepIdx);                 // 父删掉这个路标
        parent.children.remove(sepIdx + 1);         // 父删掉右孩子(已并入左)
    }

    /** 按层打印每个节点（答辩演示用）。 */
    public void printTree() {
        Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        int level=0;
        while (!queue.isEmpty()){
            int size=queue.size();// 当前层节点数
            StringBuilder sb =new StringBuilder("level" + level + ":");
            for (int i =0 ; i<size ;i++){
                Node node =queue.poll();
                List<Object> vals = new ArrayList<>();
                for (Value v : node.keys) vals.add(v.value);
                sb.append(node.isLeaf ? "L" : "I").append(vals).append(" ");
                if(!node.isLeaf){
                    queue.addAll(node.children);
                }

            }
            System.out.println(sb);
            level++;
        }
    }

    private SplitResult insertInto(Node node, Value key, RID rid) throws DBException {
        int index = 0;
        if (node.isLeaf) {
            while (index < node.keys.size() && cmp(key, node.keys.get(index)) >= 0) {
                index++;
            }
            // 重复 key：>= 0 的循环会跳过相等的，相同 key 落在 index-1，追加一个 RID（非唯一索引）
            if (index > 0 && cmp(key, node.keys.get(index - 1)) == 0) {
                node.values.get(index - 1).add(rid);
                return null;   // key 已存在，只是多挂一个 RID，不会改变树结构
            }
            node.keys.add(index, key);
            List<RID> ridList = new ArrayList<>();
            ridList.add(rid);
            node.values.add(index, ridList);
            if (node.keys.size() > MAX_KEYS) {
                return splitLeaf(node);
            }
            return null;
        } else {
            while (index < node.keys.size() && cmp(key, node.keys.get(index)) >= 0) {
                index++;
            }
            SplitResult r= insertInto(node.children.get(index), key, rid);
            if(r==null){
                return null;
            }
            node.keys.add(index,r.upKey);
            node.children.add(index+1,r.rightNode);
            if(node.keys.size() > MAX_KEYS){
                return splitInternal(node);
            }
            return null;
        }
    }

    private SplitResult splitLeaf(Node node) {
        int mid = node.keys.size() / 2;//4-->2，还是右端
        Node newNode = new Node(true);
        newNode.keys.addAll(node.keys.subList(mid, node.keys.size()));
        newNode.values.addAll(node.values.subList(mid,node.values.size()));
        node.keys.subList(mid,node.keys.size()).clear();
        node.values.subList(mid,node.values.size()).clear();
        Value upKey = newNode.keys.get(0);
        newNode.next=node.next;
        node.next=newNode;
        return new SplitResult(upKey,newNode);
    }
    private SplitResult splitInternal(Node node){
        int mid = node.keys.size() / 2;
        Value upKey = node.keys.get(mid);
        Node newNode = new Node(false);
        newNode.keys.addAll(node.keys.subList(mid+1,node.keys.size()));
        newNode.children.addAll(node.children.subList(mid+1,node.children.size()));
        node.keys.subList(mid,node.keys.size()).clear();
        node.children.subList(mid+1,node.children.size()).clear();
        return new SplitResult(upKey,newNode);
    }
}
