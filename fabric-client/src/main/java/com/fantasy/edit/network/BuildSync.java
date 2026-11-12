package com.fantasy.edit.network;

import com.fantasy.edit.FantasyEdit;
import com.fantasy.edit.builder.BuildClipboard;
import com.fantasy.edit.builder.BuildSelection;
import com.fantasy.edit.builder.ClientBuildEngine;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 建造操作的双端同步通道。
 *
 * <ul>
 *     <li>C2S {@code fantasy_edit:build}：一次性的选区填充 / 替换 / 粘贴 / 笔刷 / 撤销 / 重做</li>
 *     <li>S2C {@code fantasy_edit:build_ack}：服务端校验与执行结果</li>
 * </ul>
 * 未安装插件时自动退化为纯客户端建造，不影响单人世界。
 */
public final class BuildSync {

    private static final int MAX_JSON = 60000;
    private static final long BRUSH_INTERVAL_MS = 250L;
    private static final Gson GSON = new Gson();

    private static volatile boolean serverActive;
    private static long lastBrushPush;

    private BuildSync() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(BuildPayload.ID, BuildPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BuildAckPayload.ID, BuildAckPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(BuildAckPayload.ID,
                (payload, context) -> context.client().execute(() -> handleAck(payload.json())));
    }

    public static boolean serverActive() {
        return serverActive;
    }

    public static void pushFill(BuildSelection selection, BlockState state) {
        if (selection == null || !selection.isComplete() || state == null) {
            return;
        }
        BuildMessage message = region("FILL", selection);
        message.block = state.toString();
        send(message);
    }

    public static void pushReplace(BuildSelection selection, BlockState from, BlockState to) {
        if (selection == null || !selection.isComplete() || to == null) {
            return;
        }
        BuildMessage message = region("REPLACE", selection);
        message.block = to.toString();
        message.target = from == null ? "minecraft:air" : from.toString();
        send(message);
    }

    public static void pushBrush(BlockPos center, String shape, int radius, BlockState state) {
        if (center == null || state == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBrushPush < BRUSH_INTERVAL_MS) {
            return;
        }
        lastBrushPush = now;

        BuildMessage message = new BuildMessage();
        message.action = "BRUSH";
        message.shape = shape;
        message.radius = radius;
        message.block = state.toString();
        message.origin = new int[]{center.getX(), center.getY(), center.getZ()};
        send(message);
    }

    public static void pushPaste(BlockPos origin, BuildClipboard clipboard) {
        if (origin == null || clipboard == null || clipboard.isEmpty()) {
            return;
        }
        BuildMessage message = new BuildMessage();
        message.action = "PASTE";
        message.origin = new int[]{origin.getX(), origin.getY(), origin.getZ()};
        message.blocks = clipboard.serializeTransformed();
        send(message);
    }

    public static void pushHistory(String action) {
        BuildMessage message = new BuildMessage();
        message.action = action;
        send(message);
    }

    private static BuildMessage region(String action, BuildSelection selection) {
        BuildMessage message = new BuildMessage();
        message.action = action;
        BlockPos min = selection.min();
        BlockPos max = selection.max();
        message.min = new int[]{min.getX(), min.getY(), min.getZ()};
        message.max = new int[]{max.getX(), max.getY(), max.getZ()};
        return message;
    }

    private static void send(BuildMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        message.dimension = minecraft.level.dimension().identifier().toString();
        String json = message.toJson();
        if (json.length() > MAX_JSON) {
            FantasyEdit.LOGGER.warn("建造报文过大（{} 字符），已跳过服务端同步", json.length());
            serverActive = false;
            return;
        }
        if (!ClientPlayNetworking.canSend(BuildPayload.ID)) {
            serverActive = false;
            return;
        }
        ClientPlayNetworking.send(new BuildPayload(json));
        serverActive = true;
    }

    private static void handleAck(String json) {
        Ack ack = GSON.fromJson(json, Ack.class);
        if (ack == null) {
            return;
        }
        serverActive = true;
        if (ack.ok) {
            ClientBuildEngine.setStatus("§a服务端已同步 " + ack.action + " · " + ack.changed + " 格");
        } else {
            ClientBuildEngine.setStatus("§c服务端拒绝: " + ack.message);
        }
    }

    private static final class Ack {
        boolean ok;
        String action;
        int changed;
        String message;
    }
}
