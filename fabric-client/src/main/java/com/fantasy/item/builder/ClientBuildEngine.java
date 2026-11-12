package com.fantasy.item.builder;

import com.fantasy.item.network.BuildSync;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 客户端快速搭建核心（仅创造模式）。
 *
 * <p>所有编辑都在客户端即时完成，同时由 {@code BuildSync} 推送给 Paper 服务端做权威落盘；
 * 由于写方块操作是幂等的，两端重复执行不会产生偏差。</p>
 */
public final class ClientBuildEngine {

    /** 单次操作允许的最大方块数，避免一次填充直接卡死客户端。 */
    public static final int MAX_VOLUME = 250_000;

    private static boolean active;
    private static BuildMode mode = BuildMode.SELECT;
    private static BrushShape brushShape = BrushShape.SPHERE;
    private static int radius = 4;
    private static boolean erasing;

    private static final BuildSelection selection = new BuildSelection();
    private static final BuildClipboard clipboard = new BuildClipboard();
    private static final BuildHistory history = new BuildHistory();

    private static String status = "§7按 B 开启快速搭建";
    private static int previewTicks;
    private static int paintCooldown;

    private ClientBuildEngine() {
    }

    /**
     * 笔刷模式下拦截原版放置 / 破坏，避免「涂抹的同时把手里方块放出去」。
     */
    public static void registerGuards() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                active && mode == BuildMode.BRUSH && hand == InteractionHand.MAIN_HAND
                        ? InteractionResult.FAIL : InteractionResult.PASS);
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
                active && mode == BuildMode.BRUSH && hand == InteractionHand.MAIN_HAND
                        ? InteractionResult.FAIL : InteractionResult.PASS);
    }

    // ------------------------------------------------------------------ 状态

    public static boolean active() {
        return active;
    }

    public static BuildMode mode() {
        return mode;
    }

    public static BrushShape brushShape() {
        return brushShape;
    }

    public static int radius() {
        return radius;
    }

    public static boolean erasing() {
        return erasing;
    }

    public static String status() {
        return status;
    }

    public static void setStatus(String value) {
        status = value;
    }

    public static BuildSelection selection() {
        return selection;
    }

    public static BuildClipboard clipboard() {
        return clipboard;
    }

    public static BuildHistory history() {
        return history;
    }

    public static boolean isCreative(Minecraft minecraft) {
        return minecraft != null && minecraft.player != null && minecraft.player.isCreative();
    }

    public static void toggle(Minecraft minecraft) {
        if (!isCreative(minecraft)) {
            status = "§c快速搭建仅在创造模式可用";
            return;
        }
        active = !active;
        status = active ? "§a快速搭建已开启 · B 关闭" : "§7快速搭建已关闭";
    }

    public static void setMode(BuildMode value) {
        mode = value == null ? BuildMode.SELECT : value;
    }

    public static void cycleMode() {
        mode = mode.next();
        status = "§b模式: " + mode.label() + " §7— " + mode.hint();
    }

    public static void cycleBrush() {
        brushShape = brushShape.next();
    }

    public static void setBrushShape(BrushShape value) {
        brushShape = value == null ? BrushShape.SPHERE : value;
    }

    public static void changeRadius(int delta) {
        radius = Math.max(1, Math.min(32, radius + delta));
        status = "§b笔刷半径: §f" + radius;
    }

    public static void toggleErase() {
        erasing = !erasing;
        status = erasing ? "§e笔刷: 抹除" : "§a笔刷: 涂抹";
    }

    // ------------------------------------------------------------------ 每帧

    /** 由 ClientTickEvents.END_CLIENT_TICK 调用。 */
    public static void tick(Minecraft minecraft) {
        if (!active) {
            return;
        }
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (!isCreative(minecraft)) {
            active = false;
            status = "§e已退出快速搭建（非创造模式）";
            return;
        }
        if (minecraft.gui.screen() != null) {
            return;
        }

        if (mode == BuildMode.BRUSH) {
            if (paintCooldown > 0) {
                paintCooldown--;
            } else if (minecraft.options.keyAttack.isDown()) {
                if (paintAt(minecraft, true)) {
                    paintCooldown = 1;
                }
            } else if (minecraft.options.keyUse.isDown()) {
                if (paintAt(minecraft, erasing)) {
                    paintCooldown = 1;
                }
            }
        }

        if (++previewTicks % 6 == 0) {
            preview(minecraft);
        }
    }

    /** 用粒子勾勒选区与笔刷范围，零渲染管线依赖。 */
    private static void preview(Minecraft minecraft) {
        Level level = minecraft.level;
        if (level == null) {
            return;
        }
        int drawn = 0;
        if (selection.isComplete()) {
            BlockPos min = selection.min();
            BlockPos max = selection.max();
            int stepX = Math.max(1, selection.sizeX() / 12);
            int stepY = Math.max(1, selection.sizeY() / 12);
            int stepZ = Math.max(1, selection.sizeZ() / 12);
            for (int x = min.getX(); x <= max.getX(); x += stepX) {
                for (int y = min.getY(); y <= max.getY(); y += stepY) {
                    drawn += corner(level, x, y, min.getZ(), drawn);
                    drawn += corner(level, x, y, max.getZ(), drawn);
                    drawn += corner(level, x, min.getY(), y, drawn);
                    drawn += corner(level, x, max.getY(), y, drawn);
                }
            }
            for (int z = min.getZ(); z <= max.getZ(); z += stepZ) {
                drawn += corner(level, min.getX(), min.getY(), z, drawn);
                drawn += corner(level, max.getX(), min.getY(), z, drawn);
                drawn += corner(level, min.getX(), max.getY(), z, drawn);
                drawn += corner(level, max.getX(), max.getY(), z, drawn);
            }
        }
        if (active && mode == BuildMode.BRUSH && minecraft.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos center = hit.getBlockPos();
            int step = Math.max(1, radius / 4);
            for (int dx = -radius; dx <= radius; dx += step) {
                for (int dy = -radius; dy <= radius; dy += step) {
                    for (int dz = -radius; dz <= radius; dz += step) {
                        if (dx * dx + dy * dy + dz * dz > radius * radius) {
                            continue;
                        }
                        if (drawn > 220) {
                            return;
                        }
                        level.addParticle(ParticleTypes.END_ROD,
                                center.getX() + dx + 0.5, center.getY() + dy + 0.5, center.getZ() + dz + 0.5,
                                0.0, 0.0, 0.0);
                        drawn++;
                    }
                }
            }
        }
    }

    private static int corner(Level level, int x, int y, int z, int drawn) {
        if (drawn > 220) {
            return 0;
        }
        level.addParticle(ParticleTypes.END_ROD, x + 0.5, y + 0.5, z + 0.5, 0.0, 0.0, 0.0);
        return 1;
    }

    // ------------------------------------------------------------------ 选区

    /** 用准星所在方块设置角点。 */
    public static boolean markCorner(Minecraft minecraft, boolean first) {
        if (!active || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            status = "§e请把准星对准方块";
            return false;
        }
        if (first) {
            selection.setFirst(hit.getBlockPos());
        } else {
            selection.setSecond(hit.getBlockPos());
        }
        status = "§b选区: " + selection.describe();
        return true;
    }

    public static int fillSelection(Minecraft minecraft) {
        if (minecraft.level == null) {
            return 0;
        }
        BlockState state = heldBlock(minecraft);
        if (state == null) {
            status = "§e手持一个方块才能填充";
            return 0;
        }
        int changed = fill(minecraft.level, selection, state);
        status = changed > 0 ? "§a填充 " + changed + " 格" : "§e没有可填充的区域";
        return changed;
    }

    public static int replaceSelection(Minecraft minecraft, boolean onlySame) {
        if (minecraft.level == null) {
            return 0;
        }
        BlockState state = heldBlock(minecraft);
        if (state == null) {
            status = "§e手持一个方块才能替换";
            return 0;
        }
        BlockPos anchor = selection.isComplete() ? selection.min() : null;
        BlockState from = onlySame && anchor != null ? minecraft.level.getBlockState(anchor) : null;
        int changed = replace(minecraft.level, selection, from, state);
        status = changed > 0 ? "§a替换 " + changed + " 格" : "§e没有匹配的方块";
        return changed;
    }

    public static void copySelection() {
        int copied = clipboard.copyFrom(Minecraft.getInstance().level, selection);
        status = copied > 0 ? "§a已复制 " + copied + " 格（" + clipboard.describe() + "）" : "§e选区为空";
    }

    public static int pasteClipboard(Minecraft minecraft) {
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            status = "§e请把准星对准方块";
            return 0;
        }
        int changed = clipboard.pasteInto(minecraft.level, hit.getBlockPos().relative(hit.getDirection()), history);
        if (changed > 0) {
            BuildSync.pushPaste(hit.getBlockPos().relative(hit.getDirection()), clipboard);
        }
        status = changed > 0 ? "§a已粘贴 " + changed + " 格" : "§e剪贴板为空";
        return changed;
    }

    public static void undo(Minecraft minecraft) {
        if (minecraft.level == null) {
            return;
        }
        status = history.undo(minecraft.level) ? "§a已撤销" : "§e没有可撤销的操作";
        BuildSync.pushHistory("UNDO");
    }

    public static void redo(Minecraft minecraft) {
        if (minecraft.level == null) {
            return;
        }
        status = history.redo(minecraft.level) ? "§a已重做" : "§e没有可重做的操作";
        BuildSync.pushHistory("REDO");
    }

    // ------------------------------------------------------------------ 编辑原语

    public static BlockState heldBlock(Minecraft minecraft) {
        if (minecraft.player == null) {
            return null;
        }
        ItemStack stack = minecraft.player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }
        return blockItem.getBlock().defaultBlockState();
    }

    public static int fill(Level level, BuildSelection selection, BlockState state) {
        if (level == null || !selection.isComplete()) {
            return 0;
        }
        if (selection.volume() > MAX_VOLUME) {
            status = "§c选区超过 " + MAX_VOLUME + " 格上限";
            return 0;
        }
        BuildHistory.Batch batch = new BuildHistory.Batch("填充 " + selection.describe());
        int changed = 0;
        for (BlockPos pos : selection.blocks()) {
            BlockState previous = level.getBlockState(pos);
            if (previous == state) {
                continue;
            }
            BlockPos key = pos.immutable();
            batch.before.put(key, previous);
            level.setBlock(key, state, 3);
            batch.after.put(key, state);
            changed++;
        }
        history.push(batch);
        if (changed > 0) {
            BuildSync.pushFill(selection, state);
        }
        return changed;
    }

    public static int replace(Level level, BuildSelection selection, BlockState from, BlockState to) {
        if (level == null || !selection.isComplete() || to == null) {
            return 0;
        }
        if (selection.volume() > MAX_VOLUME) {
            status = "§c选区超过 " + MAX_VOLUME + " 格上限";
            return 0;
        }
        BuildHistory.Batch batch = new BuildHistory.Batch("替换 " + selection.describe());
        int changed = 0;
        for (BlockPos pos : selection.blocks()) {
            BlockState previous = level.getBlockState(pos);
            if (previous == to || (from != null && previous != from) || (from == null && previous.isAir())) {
                continue;
            }
            BlockPos key = pos.immutable();
            batch.before.put(key, previous);
            level.setBlock(key, to, 3);
            batch.after.put(key, to);
            changed++;
        }
        history.push(batch);
        if (changed > 0) {
            BuildSync.pushReplace(selection, from, to);
        }
        return changed;
    }

    private static boolean paintAt(Minecraft minecraft, boolean erase) {
        if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        BlockState state = erase ? Blocks.AIR.defaultBlockState() : heldBlock(minecraft);
        if (state == null) {
            status = "§e手持一个方块才能涂抹";
            return false;
        }
        int changed = applyBrush(minecraft.level, hit.getBlockPos(), brushShape, radius, state);
        if (changed > 0) {
            BuildSync.pushBrush(hit.getBlockPos(), brushShape.name(), radius, state);
            status = "§b" + brushShape.label() + " r=" + radius + " → §f" + changed + " 格";
        }
        return changed > 0;
    }

    public static int applyBrush(Level level, BlockPos center, BrushShape shape, int radius, BlockState state) {
        if (level == null || center == null || state == null) {
            return 0;
        }
        int r = Math.max(1, Math.min(32, radius));
        BuildHistory.Batch batch = new BuildHistory.Batch(shape.label() + " r=" + r);
        int changed = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (!matchesShape(level, shape, center, dx, dy, dz, r)) {
                        continue;
                    }
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (pos.getY() < level.getMinY() || pos.getY() > level.getMaxY()) {
                        continue;
                    }
                    BlockState previous = level.getBlockState(pos);
                    if (previous == state) {
                        continue;
                    }
                    if (shape == BrushShape.ERODE && previous.isAir()) {
                        continue;
                    }
                    BlockPos key = pos.immutable();
                    batch.before.put(key, previous);
                    level.setBlock(key, state, 3);
                    batch.after.put(key, state);
                    changed++;
                }
            }
        }
        history.push(batch);
        return changed;
    }

    private static boolean matchesShape(Level level, BrushShape shape, BlockPos center,
                                        int dx, int dy, int dz, int r) {
        int horizontal = dx * dx + dz * dz;
        return switch (shape) {
            case SPHERE, SMOOTH, ERODE -> dx * dx + dy * dy + dz * dz <= r * r;
            case CUBE -> true;
            case CYLINDER -> horizontal <= r * r && Math.abs(dy) <= r;
            case NOISE -> dx * dx + dy * dy + dz * dz <= r * r
                    && level.getRandom().nextFloat() > 0.45F;
        };
    }
}
