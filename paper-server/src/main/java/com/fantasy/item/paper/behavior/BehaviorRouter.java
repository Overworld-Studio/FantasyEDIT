package com.fantasy.item.paper.behavior;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端动态事件分发器：所有规则从 {@link BehaviorPool} 实时读取，
 * 因此 GUI 推送新行为后无需重启或重载插件即可生效。
 */
public final class BehaviorRouter implements Listener {

    private final Plugin plugin;
    private final BehaviorPool pool;
    private final PaperActionExecutor executor;
    private final NamespacedKey definitionKey;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    private BukkitTask tickTask;

    public BehaviorRouter(Plugin plugin, BehaviorPool pool, PaperActionExecutor executor, NamespacedKey definitionKey) {
        this.plugin = plugin;
        this.pool = pool;
        this.executor = executor;
        this.definitionKey = definitionKey;
    }

    public void startTickTask(int intervalTicks) {
        stopTickTask();
        long interval = Math.max(1, intervalTicks);
        this.tickTask = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            for (Player player : this.plugin.getServer().getOnlinePlayers()) {
                dispatch(player, player.getInventory().getItemInMainHand(), TriggerType.ON_TICK);
            }
            if (this.cooldowns.size() > 4096) {
                this.cooldowns.clear();
            }
        }, interval, interval);
    }

    public void stopTickTask() {
        if (this.tickTask != null) {
            this.tickTask.cancel();
            this.tickTask = null;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        dispatch(event.getPlayer(), event.getItem(), TriggerType.ON_RIGHT_CLICK);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        dispatch(event.getPlayer(), event.getItemInHand(), TriggerType.ON_BLOCK_PLACE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            dispatch(player, player.getInventory().getItemInMainHand(), TriggerType.ON_ENTITY_HIT);
        }
    }

    private void dispatch(Player player, ItemStack stack, TriggerType trigger) {
        String key = itemKey(stack);
        if (key == null) {
            return;
        }
        List<BehaviorRule> rules = this.pool.get(key);
        if (rules == null) {
            return;
        }
        for (int i = 0; i < rules.size(); i++) {
            BehaviorRule rule = rules.get(i);
            if (rule == null || !rule.isValid() || rule.triggerType() != trigger) {
                continue;
            }
            if (!ready(player.getUniqueId() + "|" + key + "|" + i, rule.cooldownTicks)) {
                continue;
            }
            try {
                this.executor.execute(player, rule);
            } catch (RuntimeException e) {
                this.plugin.getLogger().warning("执行行为失败 " + key + " #" + i + ": " + e.getMessage());
            }
        }
    }

    private boolean ready(String key, int cooldownTicks) {
        long now = System.currentTimeMillis();
        long cooldown = Math.max(0, cooldownTicks) * 50L;
        Long last = this.cooldowns.get(key);
        if (last != null && now - last < cooldown) {
            return false;
        }
        this.cooldowns.put(key, now);
        return true;
    }

    private String itemKey(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(this.definitionKey, PersistentDataType.STRING);
    }
}
