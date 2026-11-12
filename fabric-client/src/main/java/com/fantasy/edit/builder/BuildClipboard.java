package com.fantasy.edit.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 结构剪贴板：保存相对坐标 → 方块状态，支持绕 Y 轴旋转与镜像。
 */
public final class BuildClipboard {

    private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    private int sizeX;
    private int sizeY;
    private int sizeZ;
    private int rotationSteps;
    private boolean mirrored;
    private String source = "";

    public boolean isEmpty() {
        return this.blocks.isEmpty();
    }

    public int volume() {
        return this.blocks.size();
    }

    public String describe() {
        if (isEmpty()) {
            return "空";
        }
        return this.sizeX + "×" + this.sizeY + "×" + this.sizeZ + " (" + this.blocks.size() + ")"
                + (this.rotationSteps == 0 ? "" : " ↗" + (this.rotationSteps * 90) + "°")
                + (this.mirrored ? " ⇋" : "");
    }

    public void clear() {
        this.blocks.clear();
        this.sizeX = 0;
        this.sizeY = 0;
        this.sizeZ = 0;
        this.rotationSteps = 0;
        this.mirrored = false;
        this.source = "";
    }

    public void rotate() {
        this.rotationSteps = (this.rotationSteps + 1) & 3;
    }

    public void toggleMirror() {
        this.mirrored = !this.mirrored;
    }

    public String source() {
        return this.source;
    }

    /** 把选区内的方块复制到剪贴板，空气会被跳过。 */
    public int copyFrom(Level level, BuildSelection selection) {
        clear();
        if (level == null || !selection.isComplete()) {
            return 0;
        }
        BlockPos min = selection.min();
        for (BlockPos pos : selection.blocks()) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            this.blocks.put(new BlockPos(
                    pos.getX() - min.getX(),
                    pos.getY() - min.getY(),
                    pos.getZ() - min.getZ()), state);
        }
        this.sizeX = selection.sizeX();
        this.sizeY = selection.sizeY();
        this.sizeZ = selection.sizeZ();
        this.source = selection.describe();
        return this.blocks.size();
    }

    /** 把剪贴板写入世界，返回实际改动的方块数。 */
    public int pasteInto(Level level, BlockPos origin, BuildHistory history) {
        if (level == null || origin == null || isEmpty()) {
            return 0;
        }
        BuildHistory.Batch batch = new BuildHistory.Batch("粘贴 " + describe());
        int changed = 0;
        for (Map.Entry<BlockPos, BlockState> entry : this.blocks.entrySet()) {
            BlockPos target = origin.offset(transform(entry.getKey()));
            BlockState state = transformState(entry.getValue());
            BlockState previous = level.getBlockState(target);
            if (previous == state) {
                continue;
            }
            batch.before.put(target.immutable(), previous);
            level.setBlock(target, state, 3);
            batch.after.put(target.immutable(), state);
            changed++;
        }
        if (history != null) {
            history.push(batch);
        }
        return changed;
    }

    /** 供服务端同步使用的扁平结构列表（已应用旋转与镜像）。 */
    public List<String> serializeTransformed() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> entry : this.blocks.entrySet()) {
            BlockPos pos = transform(entry.getKey());
            BlockState state = transformState(entry.getValue());
            out.add(pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + state);
        }
        return out;
    }

    private BlockPos transform(BlockPos pos) {
        int x = this.mirrored ? this.sizeX - 1 - pos.getX() : pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return switch (this.rotationSteps & 3) {
            case 1 -> new BlockPos(this.sizeZ - 1 - z, y, x);
            case 2 -> new BlockPos(this.sizeX - 1 - x, y, this.sizeZ - 1 - z);
            case 3 -> new BlockPos(z, y, this.sizeX - 1 - x);
            default -> new BlockPos(x, y, z);
        };
    }

    private BlockState transformState(BlockState state) {
        BlockState result = state;
        if (this.mirrored) {
            result = result.mirror(Mirror.LEFT_RIGHT);
        }
        return switch (this.rotationSteps & 3) {
            case 1 -> result.rotate(Rotation.CLOCKWISE_90);
            case 2 -> result.rotate(Rotation.CLOCKWISE_180);
            case 3 -> result.rotate(Rotation.COUNTERCLOCKWISE_90);
            default -> result;
        };
    }
}
