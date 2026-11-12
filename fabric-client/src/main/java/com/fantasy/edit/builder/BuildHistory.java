package com.fantasy.edit.builder;

import com.fantasy.edit.rule.RuleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 建造历史：每次批量编辑记录「改前 / 改后」快照，支持无限撤销重做的有界实现。
 */
public final class BuildHistory {

    /** 一次编辑操作的双向快照。 */
    public static final class Batch {

        public final String label;
        public final Map<BlockPos, BlockState> before = new LinkedHashMap<>();
        public final Map<BlockPos, BlockState> after = new LinkedHashMap<>();

        public Batch(String label) {
            this.label = label;
        }

        public int size() {
            return this.after.size();
        }
    }

    private final Deque<Batch> undoStack = new ArrayDeque<>();
    private final Deque<Batch> redoStack = new ArrayDeque<>();

    public void push(Batch batch) {
        if (batch == null || batch.after.isEmpty()) {
            return;
        }
        this.undoStack.push(batch);
        int depth = Math.max(4, RuleEngine.integer("build.history_depth"));
        while (this.undoStack.size() > depth) {
            this.undoStack.removeLast();
        }
        this.redoStack.clear();
    }

    public int undoSize() {
        return this.undoStack.size();
    }

    public int redoSize() {
        return this.redoStack.size();
    }

    public void clear() {
        this.undoStack.clear();
        this.redoStack.clear();
    }

    public String undoLabel() {
        return this.undoStack.isEmpty() ? "-" : this.undoStack.peek().label;
    }

    public boolean undo(Level level) {
        Batch batch = this.undoStack.poll();
        if (batch == null) {
            return false;
        }
        apply(level, batch.before);
        this.redoStack.push(batch);
        return true;
    }

    public boolean redo(Level level) {
        Batch batch = this.redoStack.poll();
        if (batch == null) {
            return false;
        }
        apply(level, batch.after);
        this.undoStack.push(batch);
        return true;
    }

    private void apply(Level level, Map<BlockPos, BlockState> snapshot) {
        for (Map.Entry<BlockPos, BlockState> entry : snapshot.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), 3);
        }
    }
}
