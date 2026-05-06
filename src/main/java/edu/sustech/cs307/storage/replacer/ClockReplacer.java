package edu.sustech.cs307.storage.replacer;

import java.util.ArrayList;
import java.util.List;

public class ClockReplacer implements PageReplacer {

    /** 时钟帧的内部表示 */
    private static class FrameEntry {
        final int frameId;
        int referenceBit; // 0 或 1
        boolean isPinned;

        FrameEntry(int frameId) {
            this.frameId = frameId;
            this.referenceBit = 1;
            this.isPinned = true;
        }
    }

    private final int maxSize;
    private final List<FrameEntry> frames;
    private int hand; // 时钟指针，指向下一个待检查的帧索引

    public ClockReplacer(int numPages) {
        this.maxSize = numPages;
        this.frames = new ArrayList<>();
        this.hand = 0;
    }

    /**
     * 时钟算法寻找牺牲页。
     * 从hand位置开始环形扫描所有帧：
     * - 跳过固定的(pinned)帧
     * - 遇到引用位为1的未固定帧：将引用位设为0（给第二次机会），继续扫描
     * - 遇到引用位为0的未固定帧：该帧就是牺牲页，移除并返回其frameId
     * 如果没有可驱逐的帧（全被固定），返回-1。
     */
    @Override
    public int Victim() {
        if (frames.isEmpty()) {
            return -1;
        }
        // 最多扫描2轮（第一轮给第二次机会，第二轮找牺牲页）
        int maxScanned = 2 * frames.size();
        int scanned = 0;
        while (scanned < maxScanned) {
            // 循环处理hand
            if (hand >= frames.size()) {
                hand = 0;
            }
            FrameEntry entry = frames.get(hand);
            if (entry.isPinned) {
                // 跳过固定帧
                hand++;
                scanned++;
                continue;
            }
            if (entry.referenceBit == 1) {
                // 给第二次机会
                entry.referenceBit = 0;
                hand++;
                scanned++;
            } else {
                // referenceBit == 0，找到牺牲页
                int victimId = entry.frameId;
                frames.remove(hand);
                // hand保持在当前位置，下一个元素会移到这个位置
                if (hand >= frames.size()) {
                    hand = 0;
                }
                return victimId;
            }
        }
        // 所有帧都不可驱逐（全被固定或全被给了第二次机会但无人可驱逐）
        return -1;
    }

    /**
     * 固定一个页面。
     * - 如果该页面已在frames中且已固定，不做任何操作。
     * - 如果该页面已在frames中但未固定，标记为固定，刷新引用位为1。
     * - 如果是新页面，检查容量后加入列表末尾，标记为固定且引用位为1。
     */
    @Override
    public void Pin(int frameId) {
        // 查找是否已存在
        for (FrameEntry entry : frames) {
            if (entry.frameId == frameId) {
                if (!entry.isPinned) {
                    // 从未固定变为固定，刷新引用位
                    entry.isPinned = true;
                    entry.referenceBit = 1;
                }
                // 已固定则不做任何操作
                return;
            }
        }
        // 新页面
        if (frames.size() >= maxSize) {
            throw new RuntimeException("REPLACER IS FULL");
        }
        frames.add(new FrameEntry(frameId));
    }

    /**
     * 取消固定一个页面。
     * - 将frameId标记为未固定状态。
     * - 如果frameId不在frames中或已经处于未固定状态，抛出异常。
     */
    @Override
    public void Unpin(int frameId) {
        for (FrameEntry entry : frames) {
            if (entry.frameId == frameId) {
                if (!entry.isPinned) {
                    throw new RuntimeException("UNPIN PAGE NOT FOUND");
                }
                entry.isPinned = false;
                return;
            }
        }
        throw new RuntimeException("UNPIN PAGE NOT FOUND");
    }

    @Override
    public int size() {
        return frames.size();
    }
}
