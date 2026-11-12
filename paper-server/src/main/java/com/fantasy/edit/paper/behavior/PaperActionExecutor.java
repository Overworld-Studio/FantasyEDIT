package com.fantasy.edit.paper.behavior;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 服务端权威动作执行器：负责状态效果、指令与需要广播给全场的表现。
 * 所有参数均已通过 {@link BehaviorRule#isValid()} 与白名单校验。
 */
public final class PaperActionExecutor {

    private static final Set<String> BLOCKED_COMMANDS = Set.of(
            "op", "deop", "stop", "restart", "reload", "ban", "ban-ip", "pardon",
            "whitelist", "kick", "minecraft:op", "minecraft:deop", "minecraft:stop"
    );

    private final boolean allowConsoleCommands;

    public PaperActionExecutor(boolean allowConsoleCommands) {
        this.allowConsoleCommands = allowConsoleCommands;
    }

    public void execute(Player player, BehaviorRule rule) {
        switch (rule.actionType()) {
            case SPAWN_PARTICLE -> spawnParticle(player, rule);
            case PLAY_SOUND -> playSound(player, rule);
            case APPLY_POTION -> applyPotion(player, rule);
            case EXECUTE_COMMAND -> executeCommand(player, rule);
            case CUSTOM_SCRIPT_HOOK -> runHook(player, rule.value);
        }
    }

    private void spawnParticle(Player player, BehaviorRule rule) {
        Particle particle;
        try {
            particle = Particle.valueOf(rule.value.replace("minecraft:", "").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return;
        }
        Location base = player.getLocation().add(0.0D, 1.0D, 0.0D);
        player.getWorld().spawnParticle(particle, base, Math.max(1, Math.min(64, rule.amount)),
                0.4D, 0.5D, 0.4D, 0.02D);
    }

    private void playSound(Player player, BehaviorRule rule) {
        float pitch = 0.8F + Math.min(4, Math.max(0, rule.amplifier)) * 0.2F;
        player.getWorld().playSound(player.getLocation(), rule.value, 1.0F, pitch);
    }

    private void applyPotion(Player player, BehaviorRule rule) {
        NamespacedKey key = NamespacedKey.fromString(rule.value);
        if (key == null) {
            return;
        }
        PotionEffectType type = PotionEffectType.getByKey(key);
        if (type == null) {
            return;
        }
        player.addPotionEffect(new PotionEffect(type, rule.duration, rule.amplifier, true, true, true));
    }

    private void executeCommand(Player player, BehaviorRule rule) {
        String command = rule.value.startsWith("/") ? rule.value.substring(1) : rule.value;
        if (command.isBlank()) {
            return;
        }
        String root = command.split(" ", 2)[0].toLowerCase(Locale.ROOT);
        if (BLOCKED_COMMANDS.contains(root)) {
            return;
        }
        if (rule.asPlayer) {
            player.performCommand(command);
            return;
        }
        if (!this.allowConsoleCommands) {
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private void runHook(Player player, String name) {
        Hook hook = Hooks.ALL.get(name == null ? "" : name.toLowerCase(Locale.ROOT));
        if (hook != null) {
            hook.run(player);
        }
    }

    private interface Hook {
        void run(Player player);
    }

    /** 内置服务端脚本钩子白名单：客户端同名的钩子只负责表现，这里负责真实效果。 */
    private static final class Hooks {
        private static final Map<String, Hook> ALL = new LinkedHashMap<>();

        static {
            ALL.put("spark_ring", player -> ring(player, Particle.END_ROD, 16, 1.1D));
            ALL.put("flame_burst", player -> ring(player, Particle.FLAME, 24, 0.6D));
            ALL.put("soul_aura", player -> ring(player, Particle.SOUL_FIRE_FLAME, 12, 0.9D));
            ALL.put("confetti", player -> ring(player, Particle.HAPPY_VILLAGER, 20, 1.4D));
            ALL.put("crit_spiral", player -> ring(player, Particle.CRIT, 18, 0.5D));
            ALL.put("heal_1", player -> player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 2.0D)));
            ALL.put("glow", player -> player.addPotionEffect(
                    new PotionEffect(PotionEffectType.GLOWING, 100, 0, true, false, true)));
        }

        private static void ring(Player player, Particle particle, int points, double radius) {
            Location center = player.getLocation().add(0.0D, 0.2D, 0.0D);
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0D * i / points;
                Location point = center.clone().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
                player.getWorld().spawnParticle(particle, point, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        static List<String> names() {
            return List.copyOf(ALL.keySet());
        }
    }

    public static List<String> hookNames() {
        return Hooks.names();
    }
}
