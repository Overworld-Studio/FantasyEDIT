package com.fantasy.item.paper.net;

import com.fantasy.item.paper.FantasyItemPlugin;
import com.fantasy.item.paper.behavior.ActionType;
import com.fantasy.item.paper.behavior.BehaviorRule;
import com.fantasy.item.paper.behavior.PaperActionExecutor;
import com.fantasy.item.paper.model.ItemDefinition;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 插件消息通道：
 * <ul>
 *     <li>{@code fantasy_item:behavior}：客户端推送行为树（需 fantasyitem.admin）</li>
 *     <li>{@code fantasy_item:behavior_sync}：校验通过后广播给所有在线客户端</li>
 * </ul>
 */
public final class BehaviorChannelListener implements PluginMessageListener {

    public static final String INBOUND = "fantasy_item:behavior";
    public static final String OUTBOUND = "fantasy_item:behavior_sync";

    private static final Pattern KEY_PATTERN =
            Pattern.compile("[a-z0-9_.-]{1,64}:[a-z0-9_.-]{1,64}");
    private static final int MAX_BYTES = 30000;

    private final FantasyItemPlugin plugin;

    public BehaviorChannelListener(FantasyItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!INBOUND.equals(channel)) {
            return;
        }
        if (!player.hasPermission("fantasyitem.admin")) {
            return;
        }
        if (message == null || message.length == 0 || message.length > MAX_BYTES) {
            this.plugin.getLogger().warning("拒绝过大的行为报文: " + (message == null ? 0 : message.length) + "B");
            return;
        }

        String json = new String(message, StandardCharsets.UTF_8);
        BehaviorMessage incoming = BehaviorMessage.fromJson(json);
        if (incoming == null || incoming.key == null || !KEY_PATTERN.matcher(incoming.key).matches()) {
            this.plugin.getLogger().warning("拒绝非法行为报文（key 不合法）来自 " + player.getName());
            return;
        }

        List<BehaviorRule> accepted = new ArrayList<>();
        if (incoming.behaviors != null) {
            for (BehaviorRule rule : incoming.behaviors) {
                if (rule == null || !rule.isValid()) {
                    continue;
                }
                if (rule.actionType() == ActionType.CUSTOM_SCRIPT_HOOK
                        && !PaperActionExecutor.hookNames().contains(rule.value.toLowerCase(Locale.ROOT))) {
                    this.plugin.getLogger().warning("忽略未知脚本钩子: " + rule.value);
                    continue;
                }
                if (accepted.size() >= BehaviorRule.MAX_RULES) {
                    break;
                }
                accepted.add(rule);
            }
        }

        // 1) 注册进服务端动态行为池（立即生效）
        this.plugin.behaviors().replace(incoming.key, accepted);

        // 2) 回写 JSON，保证重启后依然存在
        ItemDefinition definition = this.plugin.definitions().get(incoming.key);
        if (definition != null) {
            definition.behaviors = new ArrayList<>(accepted);
            this.plugin.definitions().save(definition);
        }

        // 3) 广播权威行为树给所有在线客户端（含发起者，用于标记 serverActive）
        BehaviorMessage outbound = new BehaviorMessage();
        outbound.key = incoming.key;
        outbound.behaviors = accepted;
        byte[] payload = outbound.toJson().getBytes(StandardCharsets.UTF_8);
        for (Player online : this.plugin.getServer().getOnlinePlayers()) {
            online.sendPluginMessage(this.plugin, OUTBOUND, payload);
        }
        this.plugin.getLogger().info("已注入行为树 " + incoming.key + "（" + accepted.size() + " 条，来自 "
                + player.getName() + "）");
    }
}
