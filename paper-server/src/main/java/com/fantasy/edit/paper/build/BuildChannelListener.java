package com.fantasy.edit.paper.build;

import com.fantasy.edit.paper.FantasyEditPlugin;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * {@code fantasy_edit:build} 通道：校验客户端建造请求并执行，回执写入 {@code fantasy_edit:build_ack}。
 *
 * <p>安全边界：仅创造模式、需要 {@code fantasyedit.build} 权限、受体积与半径限额约束。</p>
 */
public final class BuildChannelListener implements PluginMessageListener {

    public static final String INBOUND = "fantasy_edit:build";
    public static final String OUTBOUND = "fantasy_edit:build_ack";
    private static final int MAX_MESSAGE = 60000;

    private final FantasyEditPlugin plugin;
    private final BuildLimits limits;
    private final WorldEditService editor;
    private final Gson gson = new Gson();

    public BuildChannelListener(FantasyEditPlugin plugin, BuildLimits limits, WorldEditService editor) {
        this.plugin = plugin;
        this.limits = limits;
        this.editor = editor;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!INBOUND.equals(channel) || message == null) {
            return;
        }
        BuildMessage parsed = BuildMessage.fromJson(new String(message, StandardCharsets.UTF_8));
        if (parsed == null || parsed.action == null) {
            return;
        }
        // 方块编辑必须在主线程执行
        Bukkit.getScheduler().runTask(this.plugin, () -> handle(player, parsed));
    }

    private void handle(Player player, BuildMessage message) {
        String action = message.action.toUpperCase(Locale.ROOT);
        if (!this.limits.enabled) {
            ack(player, false, action, 0, "服务端已关闭快速搭建");
            return;
        }
        if (!player.hasPermission("fantasyedit.build")) {
            ack(player, false, action, 0, "缺少 fantasyedit.build 权限");
            return;
        }
        if (this.limits.creativeOnly && player.getGameMode() != GameMode.CREATIVE) {
            ack(player, false, action, 0, "快速搭建仅在创造模式可用");
            return;
        }
        World world = player.getWorld();
        if (message.dimension != null && !message.dimension.isEmpty()
                && !message.dimension.equals(world.getKey().toString())) {
            ack(player, false, action, 0, "维度不匹配");
            return;
        }

        switch (action) {
            case "FILL" -> {
                BlockData data = parse(message.block);
                if (data == null) {
                    ack(player, false, action, 0, "方块 ID 非法");
                    return;
                }
                finish(player, action, this.editor.fill(player, world, message.min, message.max, data));
            }
            case "REPLACE" -> {
                BlockData data = parse(message.block);
                BlockData target = parse(message.target);
                if (data == null) {
                    ack(player, false, action, 0, "方块 ID 非法");
                    return;
                }
                finish(player, action, this.editor.replace(player, world, message.min, message.max, target, data));
            }
            case "BRUSH" -> {
                BlockData data = parse(message.block);
                if (data == null || message.origin == null) {
                    ack(player, false, action, 0, "笔刷参数非法");
                    return;
                }
                finish(player, action,
                        this.editor.brush(player, world, message.origin, message.shape, message.radius, data));
            }
            case "PASTE" -> {
                if (message.origin == null) {
                    ack(player, false, action, 0, "粘贴原点缺失");
                    return;
                }
                finish(player, action, this.editor.paste(player, world, message.origin, message.blocks));
            }
            case "UNDO" -> ack(player, this.editor.undo(player), action, 0, "已撤销");
            case "REDO" -> ack(player, this.editor.redo(player), action, 0, "已重做");
            default -> ack(player, false, action, 0, "未知操作");
        }
    }

    private void finish(Player player, String action, int changed) {
        if (changed == WorldEditService.REJECTED) {
            ack(player, false, action, 0, "超出体积/半径限额");
            return;
        }
        ack(player, true, action, changed, "完成");
    }

    private void ack(Player player, boolean ok, String action, int changed, String message) {
        if (!player.isOnline()) {
            return;
        }
        Ack payload = new Ack();
        payload.ok = ok;
        payload.action = action;
        payload.changed = changed;
        payload.message = message;
        player.sendPluginMessage(this.plugin, OUTBOUND,
                this.gson.toJson(payload).getBytes(StandardCharsets.UTF_8));
    }

    private static BlockData parse(String raw) {
        if (raw == null || raw.isEmpty() || raw.length() > 256) {
            return null;
        }
        try {
            return Bukkit.createBlockData(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static final class Ack {
        boolean ok;
        String action;
        int changed;
        String message;
    }
}
