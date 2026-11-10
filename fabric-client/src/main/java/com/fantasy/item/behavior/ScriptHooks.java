package com.fantasy.item.behavior;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 内置轻量脚本钩子：无需真正加载脚本文件即可让 CUSTOM_SCRIPT_HOOK 生效。
 * 白名单机制，避免任意代码执行。
 */
public final class ScriptHooks {

    public interface Hook {
        void run(Player player);
    }

    private static final Map<String, Hook> HOOKS = new LinkedHashMap<>();

    static {
        HOOKS.put("spark_ring", player -> ring(player, ParticleTypes.END_ROD, 16, 1.1D));
        HOOKS.put("flame_burst", player -> {
            for (int i = 0; i < 24; i++) {
                double angle = Math.PI * 2.0D * i / 24.0D;
                player.level().addParticle(ParticleTypes.FLAME,
                        player.getX() + Math.cos(angle) * 0.6D,
                        player.getY() + 0.9D + (i % 4) * 0.15D,
                        player.getZ() + Math.sin(angle) * 0.6D,
                        0.0D, 0.02D, 0.0D);
            }
        });
        HOOKS.put("soul_aura", player -> ring(player, ParticleTypes.SOUL_FIRE_FLAME, 12, 0.9D));
        HOOKS.put("confetti", player -> ring(player, ParticleTypes.HAPPY_VILLAGER, 20, 1.4D));
        HOOKS.put("crit_spiral", player -> {
            Vec3 look = player.getLookAngle();
            Vec3 base = player.position().add(look.scale(1.0D)).add(0.0D, 1.0D, 0.0D);
            for (int i = 0; i < 18; i++) {
                double t = i / 18.0D * Math.PI * 2.0D;
                player.level().addParticle(ParticleTypes.CRIT,
                        base.x + Math.cos(t) * 0.5D, base.y + i * 0.05D, base.z + Math.sin(t) * 0.5D,
                        0.0D, 0.0D, 0.0D);
            }
        });
    }

    private ScriptHooks() {
    }

    public static boolean exists(String name) {
        return name != null && HOOKS.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public static String[] names() {
        return HOOKS.keySet().toArray(new String[0]);
    }

    public static void run(String name, Player player) {
        if (name == null || player == null) {
            return;
        }
        Hook hook = HOOKS.get(name.toLowerCase(Locale.ROOT));
        if (hook == null || Minecraft.getInstance().level == null) {
            return;
        }
        hook.run(player);
    }

    private static void ring(Player player, net.minecraft.core.particles.SimpleParticleType particle,
                             int points, double radius) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            player.level().addParticle(particle,
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + 0.2D,
                    player.getZ() + Math.sin(angle) * radius,
                    0.0D, 0.05D, 0.0D);
        }
    }
}
