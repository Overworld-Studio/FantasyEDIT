package com.fantasy.edit.behavior;

/**
 * 单条「触发器 → 动作」规则，随定义一起持久化到 definitions/*.json。
 */
public class BehaviorRule {

    public static final int MAX_RULES = 16;

    public String trigger = TriggerType.ON_RIGHT_CLICK.name();
    public String action = ActionType.SPAWN_PARTICLE.name();
    public String value = ActionType.SPAWN_PARTICLE.defaultValue();

    /** 粒子数量 / 音效音量（0.1~2.0）复用该字段。 */
    public int amount = 12;

    /** 状态效果持续时间（tick），默认 5 秒。 */
    public int duration = 100;

    /** 状态效果等级，0 表示 I 级。 */
    public int amplifier = 0;

    /** 指令动作是否以玩家身份执行。 */
    public boolean asPlayer = true;

    /** 两次触发之间的最小间隔（tick）。 */
    public int cooldownTicks = 10;

    public TriggerType triggerType() {
        return TriggerType.parse(this.trigger);
    }

    public ActionType actionType() {
        return ActionType.parse(this.action);
    }

    public BehaviorRule copy() {
        BehaviorRule copy = new BehaviorRule();
        copy.trigger = this.trigger;
        copy.action = this.action;
        copy.value = this.value;
        copy.amount = this.amount;
        copy.duration = this.duration;
        copy.amplifier = this.amplifier;
        copy.asPlayer = this.asPlayer;
        copy.cooldownTicks = this.cooldownTicks;
        return copy;
    }

    /**
     * 客户端与 AUTO 校验共用：非法规则不会进入事件总线。
     */
    public boolean isValid() {
        if (this.trigger == null || this.action == null || this.value == null) {
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

    public String describe() {
        return this.triggerType().label() + " → " + this.actionType().label() + " : " + this.value;
    }
}
