package com.fantasy.item.paper.behavior;

/**
 * 一条「触发器 → 动作」规则，JSON 结构与客户端完全一致。
 */
public class BehaviorRule {

    public static final int MAX_RULES = 16;

    public String trigger = "ON_RIGHT_CLICK";
    public String action = "SPAWN_PARTICLE";
    public String value = "minecraft:flame";
    public int amount = 12;
    public int duration = 100;
    public int amplifier = 0;
    public boolean asPlayer = true;
    public int cooldownTicks = 10;

    public TriggerType triggerType() {
        return TriggerType.parse(this.trigger);
    }

    public ActionType actionType() {
        return ActionType.parse(this.action);
    }

    public boolean isValid() {
        if (this.triggerType() == null || this.actionType() == null || this.value == null) {
            return false;
        }
        if (this.value.isBlank() || this.value.length() > 128) {
            return false;
        }
        if (this.value.indexOf('\n') >= 0 || this.value.indexOf('\r') >= 0) {
            return false;
        }
        return this.amount >= 1 && this.amount <= 64
                && this.duration >= 1 && this.duration <= 72000
                && this.amplifier >= 0 && this.amplifier <= 4
                && this.cooldownTicks >= 0 && this.cooldownTicks <= 1200;
    }
}
