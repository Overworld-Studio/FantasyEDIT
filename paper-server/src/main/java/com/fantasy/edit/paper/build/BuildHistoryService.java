package com.fantasy.edit.paper.build;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 每个玩家独立的撤销 / 重做历史。
 *
 * <p>快照只保存被真正改动过的方块，且方块坐标已归一化为整数坐标，
 * 因此大范围填充也不会撑爆内存。</p>
 */
public final class BuildHistoryService {

    /** 一次编辑操作的双向快照。 */
    public static final class Batch {

        public final String label;
        public final Map<Location, BlockData> before = new LinkedHashMap<>();
        public final Map<Location, BlockData> after = new LinkedHashMap<>();

        Batch(String label) {
            this.label = label;
        }

        public int size() {
            return this.after.size();
        }
    }

    private final Map<UUID, Deque<Batch>> undoStacks = new HashMap<>();
    private final Map<UUID, Deque<Batch>> redoStacks = new HashMap<>();
    private final int depth;

    public BuildHistoryService(int depth) {
        this.depth = depth;
    }

    public void push(Player player, Batch batch) {
        if (batch == null || batch.after.isEmpty()) {
            return;
        }
        Deque<Batch> stack = this.undoStacks.computeIfAbsent(player.getUniqueId(), key -> new ArrayDeque<>());
        stack.push(batch);
        while (stack.size() > this.depth) {
            stack.removeLast();
        }
        Deque<Batch> redo = this.redoStacks.get(player.getUniqueId());
        if (redo != null) {
            redo.clear();
        }
    }

    public int undoSize(Player player) {
        Deque<Batch> stack = this.undoStacks.get(player.getUniqueId());
        return stack == null ? 0 : stack.size();
    }

    public int redoSize(Player player) {
        Deque<Batch> stack = this.redoStacks.get(player.getUniqueId());
        return stack == null ? 0 : stack.size();
    }

    public Batch undo(Player player) {
        Deque<Batch> stack = this.undoStacks.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Batch batch = stack.pop();
        apply(batch.before);
        this.redoStacks.computeIfAbsent(player.getUniqueId(), key -> new ArrayDeque<>()).push(batch);
        return batch;
    }

    public Batch redo(Player player) {
        Deque<Batch> stack = this.redoStacks.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Batch batch = stack.pop();
        apply(batch.after);
        this.undoStacks.computeIfAbsent(player.getUniqueId(), key -> new ArrayDeque<>()).push(batch);
        return batch;
    }

    public void clear(Player player) {
        this.undoStacks.remove(player.getUniqueId());
        this.redoStacks.remove(player.getUniqueId());
    }

    private void apply(Map<Location, BlockData> snapshot) {
        for (Map.Entry<Location, BlockData> entry : snapshot.entrySet()) {
            Location location = entry.getKey();
            World world = location.getWorld();
            if (world == null) {
                continue;
            }
            world.getBlockAt(location).setBlockData(entry.getValue(), false);
        }
    }
}
