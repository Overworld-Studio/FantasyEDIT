package com.fantasy.item.builder;

import net.minecraft.core.BlockPos;

/**
 * 立方体选区：由两个角点定义，坐标在读取时自动归一化。
 */
public final class BuildSelection {

    private BlockPos first;
    private BlockPos second;

    public boolean isComplete() {
        return this.first != null && this.second != null;
    }

    public BlockPos first() {
        return this.first;
    }

    public BlockPos second() {
        return this.second;
    }

    public void setFirst(BlockPos pos) {
        this.first = pos == null ? null : pos.immutable();
    }

    public void setSecond(BlockPos pos) {
        this.second = pos == null ? null : pos.immutable();
    }

    public void clear() {
        this.first = null;
        this.second = null;
    }

    public BlockPos min() {
        if (!isComplete()) {
            return BlockPos.ZERO;
        }
        return new BlockPos(
                Math.min(this.first.getX(), this.second.getX()),
                Math.min(this.first.getY(), this.second.getY()),
                Math.min(this.first.getZ(), this.second.getZ()));
    }

    public BlockPos max() {
        if (!isComplete()) {
            return BlockPos.ZERO;
        }
        return new BlockPos(
                Math.max(this.first.getX(), this.second.getX()),
                Math.max(this.first.getY(), this.second.getY()),
                Math.max(this.first.getZ(), this.second.getZ()));
    }

    public int sizeX() {
        return isComplete() ? max().getX() - min().getX() + 1 : 0;
    }

    public int sizeY() {
        return isComplete() ? max().getY() - min().getY() + 1 : 0;
    }

    public int sizeZ() {
        return isComplete() ? max().getZ() - min().getZ() + 1 : 0;
    }

    public long volume() {
        if (!isComplete()) {
            return 0L;
        }
        return (long) sizeX() * (long) sizeY() * (long) sizeZ();
    }

    public Iterable<BlockPos> blocks() {
        return BlockPos.betweenClosed(min(), max());
    }

    public String describe() {
        if (!isComplete()) {
            return "未选定";
        }
        return sizeX() + "×" + sizeY() + "×" + sizeZ() + " (" + volume() + ")";
    }
}
