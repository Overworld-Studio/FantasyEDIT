package com.fantasy.item.paper.build;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 服务端权威方块编辑：填充、替换、笔刷与结构粘贴。
 *
 * <p>所有写入都经过体积与半径限额校验，并记录到 {@link BuildHistoryService}，
 * 因此客户端即使伪造报文也无法执行超范围操作。</p>
 */
public final class WorldEditService {

    /** 表示「超出限额」。 */
    public static final int REJECTED = -1;

    private final BuildHistoryService history;
    private final BuildLimits limits;

    public WorldEditService(BuildHistoryService history, BuildLimits limits) {
        this.history = history;
        this.limits = limits;
    }

    public BuildHistoryService history() {
        return this.history;
    }

    public int fill(Player player, World world, int[] min, int[] max, BlockData data) {
        long volume = volume(min, max);
        if (volume <= 0 || volume > this.limits.maxVolume) {
            return REJECTED;
        }
        BuildHistoryService.Batch batch = new BuildHistoryService.Batch("填充 " + volume);
        int changed = 0;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        for (int x = min[0]; x <= max[0]; x++) {
            for (int y = Math.max(min[1], minY); y <= Math.min(max[1], maxY); y++) {
                for (int z = min[2]; z <= max[2]; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    BlockData previous = block.getBlockData();
                    if (previous.equals(data)) {
                        continue;
                    }
                    batch.before.put(key(world, x, y, z), previous);
                    block.setBlockData(data, false);
                    batch.after.put(key(world, x, y, z), data);
                    changed++;
                }
            }
        }
        this.history.push(player, batch);
        return changed;
    }

    public int replace(Player player, World world, int[] min, int[] max, BlockData target, BlockData data) {
        long volume = volume(min, max);
        if (volume <= 0 || volume > this.limits.maxVolume) {
            return REJECTED;
        }
        BuildHistoryService.Batch batch = new BuildHistoryService.Batch("替换 " + volume);
        int changed = 0;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        for (int x = min[0]; x <= max[0]; x++) {
            for (int y = Math.max(min[1], minY); y <= Math.min(max[1], maxY); y++) {
                for (int z = min[2]; z <= max[2]; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    BlockData previous = block.getBlockData();
                    if (previous.equals(data)) {
                        continue;
                    }
                    if (target != null && !previous.matches(target)) {
                        continue;
                    }
                    if (target == null && previous.getMaterial().isAir()) {
                        continue;
                    }
                    batch.before.put(key(world, x, y, z), previous);
                    block.setBlockData(data, false);
                    batch.after.put(key(world, x, y, z), data);
                    changed++;
                }
            }
        }
        this.history.push(player, batch);
        return changed;
    }

    public int brush(Player player, World world, int[] origin, String shape, int radius, BlockData data) {
        int r = Math.max(1, Math.min(this.limits.maxRadius, radius));
        long volume = (long) (2 * r + 1) * (2 * r + 1) * (2 * r + 1);
        if (volume > this.limits.maxVolume) {
            return REJECTED;
        }
        BuildHistoryService.Batch batch = new BuildHistoryService.Batch("笔刷 " + shape + " r=" + r);
        int changed = 0;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (!matches(shape, dx, dy, dz, r)) {
                        continue;
                    }
                    int x = origin[0] + dx;
                    int y = origin[1] + dy;
                    int z = origin[2] + dz;
                    if (y < minY || y > maxY) {
                        continue;
                    }
                    Block block = world.getBlockAt(x, y, z);
                    BlockData previous = block.getBlockData();
                    if (previous.equals(data)) {
                        continue;
                    }
                    if ("ERODE".equalsIgnoreCase(shape) && previous.getMaterial().isAir()) {
                        continue;
                    }
                    batch.before.put(key(world, x, y, z), previous);
                    block.setBlockData(data, false);
                    batch.after.put(key(world, x, y, z), data);
                    changed++;
                }
            }
        }
        this.history.push(player, batch);
        return changed;
    }

    public int paste(Player player, World world, int[] origin, List<String> blocks) {
        if (blocks == null || blocks.isEmpty() || blocks.size() > this.limits.maxVolume) {
            return REJECTED;
        }
        BuildHistoryService.Batch batch = new BuildHistoryService.Batch("粘贴 " + blocks.size());
        int changed = 0;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        for (String raw : blocks) {
            String[] parts = raw.split(",", 4);
            if (parts.length < 4) {
                continue;
            }
            int dx;
            int dy;
            int dz;
            try {
                dx = Integer.parseInt(parts[0]);
                dy = Integer.parseInt(parts[1]);
                dz = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                continue;
            }
            BlockData data;
            try {
                data = Bukkit.createBlockData(parts[3]);
            } catch (IllegalArgumentException e) {
                continue;
            }
            int x = origin[0] + dx;
            int y = origin[1] + dy;
            int z = origin[2] + dz;
            if (y < minY || y > maxY) {
                continue;
            }
            Block block = world.getBlockAt(x, y, z);
            BlockData previous = block.getBlockData();
            if (previous.equals(data)) {
                continue;
            }
            batch.before.put(key(world, x, y, z), previous);
            block.setBlockData(data, false);
            batch.after.put(key(world, x, y, z), data);
            changed++;
        }
        this.history.push(player, batch);
        return changed;
    }

    public boolean undo(Player player) {
        return this.history.undo(player) != null;
    }

    public boolean redo(Player player) {
        return this.history.redo(player) != null;
    }

    private static boolean matches(String shape, int dx, int dy, int dz, int r) {
        int horizontal = dx * dx + dz * dz;
        String normalized = shape == null ? "SPHERE" : shape.toUpperCase();
        return switch (normalized) {
            case "CUBE" -> true;
            case "CYLINDER" -> horizontal <= r * r && Math.abs(dy) <= r;
            case "NOISE" -> dx * dx + dy * dy + dz * dz <= r * r
                    && ThreadLocalRandom.current().nextFloat() > 0.45F;
            default -> dx * dx + dy * dy + dz * dz <= r * r;
        };
    }

    private static Location key(World world, int x, int y, int z) {
        return new Location(world, x, y, z);
    }

    private static long volume(int[] min, int[] max) {
        if (min == null || max == null || min.length < 3 || max.length < 3) {
            return 0;
        }
        long sizeX = (long) max[0] - min[0] + 1;
        long sizeY = (long) max[1] - min[1] + 1;
        long sizeZ = (long) max[2] - min[2] + 1;
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            return 0;
        }
        return sizeX * sizeY * sizeZ;
    }
}
