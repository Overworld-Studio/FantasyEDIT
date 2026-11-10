package com.fantasy.item.behavior;

import com.fantasy.item.FantasyItem;
import com.fantasy.item.definition.ItemDefinition;
import com.fantasy.item.network.BehaviorSync;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端行为执行器：负责「表现层」——粒子、音效、脚本钩子。
 * 若服务端已接管（Paper 插件在线），则跳过本地表现，避免双份特效。
 */
public final class ClientBehaviorRuntime {

    private static final Map<String, Long> LAST_FIRE = new HashMap<>();

    private ClientBehaviorRuntime() {
    }

    public static void fire(ItemDefinition definition, TriggerType trigger, Player player) {
        if (definition == null || definition.behaviors == null || player == null) {
            return;
        }
        List<BehaviorRule> rules = definition.behaviors;
        for (int i = 0; i < rules.size(); i++) {
            BehaviorRule rule = rules.get(i);
            if (rule == null || !rule.isValid() || rule.triggerType() != trigger) {
                continue;
            }
            if (!ready(definition.key() + "|" + player.getUUID() + "|" + i, rule.cooldownTicks)) {
                continue;
            }
            execute(rule, player, false);
        }
    }

    /** 供 GUI 的「本地预览」按钮调用：无论服务端是否在线都强制本地播放。 */
    public static void preview(BehaviorRule rule, Player player) {
        if (rule == null || player == null) {
            return;
        }
        execute(rule, player, true);
    }

    private static void execute(BehaviorRule rule, Player player, boolean force) {
        if (!force && BehaviorSync.serverActive()) {
            return;
        }
        Level level = player.level();
        switch (rule.actionType()) {
            case SPAWN_PARTICLE -> spawnParticle(rule, player, level);
            case PLAY_SOUND -> playSound(rule, player, level);
            case CUSTOM_SCRIPT_HOOK -> ScriptHooks.run(rule.value, player);
            case APPLY_POTION, EXECUTE_COMMAND -> {
                // 权威动作由服务端执行；纯客户端沙盒下仅提示一次
                if (force) {
                    FantasyItem.LOGGER.info("动作 {} 需要服务端安装 FantasyItem 插件后才会生效", rule.actionType());
                }
            }
        }
    }

    private static void spawnParticle(BehaviorRule rule, Player player, Level level) {
        Identifier id = parseId(rule.value);
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getValue(id);
        if (!(type instanceof SimpleParticleType simple)) {
            return;
        }
        int count = Math.max(1, Math.min(64, rule.amount));
        for (int i = 0; i < count; i++) {
            double dx = (level.getRandom().nextDouble() - 0.5D) * 0.8D;
            double dy = level.getRandom().nextDouble() * 0.8D + 0.3D;
            double dz = (level.getRandom().nextDouble() - 0.5D) * 0.8D;
            level.addParticle(simple,
                    player.getX() + dx, player.getY() + dy, player.getZ() + dz,
                    0.0D, 0.02D, 0.0D);
        }
    }

    private static void playSound(BehaviorRule rule, Player player, Level level) {
        Identifier id = parseId(rule.value);
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(id);
        if (event == null) {
            return;
        }
        float pitch = 0.8F + Math.min(4, Math.max(0, rule.amplifier)) * 0.2F;
        level.playLocalSound(player.getX(), player.getY(), player.getZ(), event,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, pitch, false);
    }

    private static Identifier parseId(String raw) {
        String value = raw.trim();
        if (value.indexOf(':') < 0) {
            value = "minecraft:" + value;
        }
        Identifier id = Identifier.tryParse(value);
        return id == null ? Identifier.fromNamespaceAndPath("minecraft", "flame") : id;
    }

    private static boolean ready(String key, int cooldownTicks) {
        long now = System.currentTimeMillis();
        long cooldown = Math.max(0, cooldownTicks) * 50L;
        Long last = LAST_FIRE.get(key);
        if (last != null && now - last < cooldown) {
            return false;
        }
        LAST_FIRE.put(key, now);
        return true;
    }
}
