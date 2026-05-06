package edu.sustech.cs307.storage.replacer;

import java.util.*;

public class LRUReplacer implements PageReplacer {

    private final int maxSize;
    private final Set<Integer> pinnedFrames = new HashSet<>();
    private final LinkedList<Integer> LRUList = new LinkedList<>();

    public LRUReplacer(int numPages) {
        this.maxSize = numPages;
    }

    /**
     * 驱逐最近最少使用的未固定页面。
     * 从LRUList头部移除并返回最久未使用的frameId。
     * 如果LRUList为空（无可驱逐页面），返回-1。
     */
    public int Victim() {
        if (LRUList.isEmpty()) {
            return -1;
        }
        return LRUList.removeFirst();
    }

    /**
     * 固定一个页面。
     * - 如果该页面已在LRUList中（之前被Unpin过），将其从LRUList移除并加入pinnedFrames。
     * - 如果该页面已在pinnedFrames中（已被固定），不做任何操作。
     * - 如果是新页面，直接加入pinnedFrames。
     * - 如果总容量已满（pinnedFrames + LRUList >= maxSize），抛出异常。
     */
    public void Pin(int frameId) {
        // 如果已经在pinnedFrames中，不重复添加
        if (pinnedFrames.contains(frameId)) {
            return;
        }
        // 如果在LRUList中，移除它（它不再处于可驱逐状态）
        if (LRUList.contains(frameId)) {
            LRUList.removeFirstOccurrence(frameId);
        }
        // 检查容量
        if (pinnedFrames.size() + LRUList.size() >= maxSize) {
            throw new RuntimeException("REPLACER IS FULL");
        }
        pinnedFrames.add(frameId);
    }

    /**
     * 取消固定一个页面。
     * - 将frameId从pinnedFrames移除，加入LRUList尾部（表示最近被访问）。
     * - 如果frameId不在pinnedFrames中，抛出异常。
     */
    public void Unpin(int frameId) {
        if (!pinnedFrames.contains(frameId)) {
            throw new RuntimeException("UNPIN PAGE NOT FOUND");
        }
        pinnedFrames.remove(frameId);
        // 如果已在LRUList中（理论上不应该），先移除再添加
        LRUList.removeFirstOccurrence(frameId);
        LRUList.addLast(frameId);
    }

    public int size() {
        return LRUList.size() + pinnedFrames.size();
    }
}