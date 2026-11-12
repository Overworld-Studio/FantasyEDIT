package com.fantasy.edit.network;

import com.fantasy.edit.FantasyEdit;
import com.fantasy.edit.behavior.BehaviorRule;
import com.fantasy.edit.definition.DefinitionStore;
import com.fantasy.edit.definition.ItemDefinition;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * 双端行为同步通道。
 * <ul>
 *     <li>C2S：玩家在 GUI 中保存时推送 {@code fantasy_edit:behavior}</li>
 *     <li>S2C：服务端校验并广播 {@code fantasy_edit:behavior_sync}</li>
 * </ul>
 * 若服务器未安装 Paper 插件（通道未注册），则自动退化为纯客户端沙盒模式。
 */
public final class BehaviorSync {

    private static volatile boolean serverActive;

    private BehaviorSync() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(BehaviorPayload.ID, BehaviorPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BehaviorSyncPayload.ID, BehaviorSyncPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(BehaviorSyncPayload.ID,
                (payload, context) -> context.client().execute(() -> applyAuthoritative(payload.json())));
    }

    /** 服务端是否已接管行为执行（安装插件且通道已注册）。 */
    public static boolean serverActive() {
        return serverActive;
    }

    /**
     * 把本地定义的行为树推送到服务端。返回是否成功送达。
     */
    public static boolean push(ItemDefinition definition) {
        if (definition == null) {
            return false;
        }
        String json = BehaviorMessage.of(definition.key(), definition.behaviors).toJson();
        if (json.length() > 30000) {
            FantasyEdit.LOGGER.warn("行为树过大（{} 字符），已跳过服务端同步", json.length());
            serverActive = false;
            return false;
        }
        if (!ClientPlayNetworking.canSend(BehaviorPayload.ID)) {
            serverActive = false;
            return false;
        }
        ClientPlayNetworking.send(new BehaviorPayload(json));
        serverActive = true;
        return true;
    }

    /**
     * 收到服务端权威行为树后回写本地定义，下一次 tick 立即生效。
     */
    private static void applyAuthoritative(String json) {
        BehaviorMessage message = BehaviorMessage.fromJson(json);
        if (message == null || message.key == null || message.behaviors == null) {
            return;
        }
        DefinitionStore store = FantasyEdit.store();
        if (store == null) {
            return;
        }
        ItemDefinition definition = store.get(message.key);
        if (definition == null) {
            return;
        }
        List<BehaviorRule> accepted = new ArrayList<>();
        for (BehaviorRule rule : message.behaviors) {
            if (rule != null && rule.isValid() && accepted.size() < BehaviorRule.MAX_RULES) {
                accepted.add(rule);
            }
        }
        definition.behaviors = accepted;
        store.save(definition);
        serverActive = true;
        FantasyEdit.LOGGER.info("已同步服务端权威行为树：{}（{} 条）", message.key, accepted.size());
    }
}
