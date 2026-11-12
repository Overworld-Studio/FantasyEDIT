package com.fantasy.edit.behavior;

/**
 * 可执行动作。客户端只负责表现类动作（粒子/音效/脚本钩子），
 * 状态效果与指令交给服务端权威执行。
 */
public enum ActionType {
    SPAWN_PARTICLE("粒子特效", "minecraft:flame"),
    PLAY_SOUND("播放音效", "minecraft:block.amethyst_block.chime"),
    APPLY_POTION("状态效果", "minecraft:speed"),
    EXECUTE_COMMAND("执行指令", "say hello from fantasy_edit"),
    CUSTOM_SCRIPT_HOOK("脚本钩子", "spark_ring");

    private final String label;
    private final String defaultValue;

    ActionType(String label, String defaultValue) {
        this.label = label;
        this.defaultValue = defaultValue;
    }

    public String label() {
        return this.label;
    }

    public String defaultValue() {
        return this.defaultValue;
    }

    public ActionType next() {
        ActionType[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static ActionType parse(String raw) {
        if (raw == null) {
            return SPAWN_PARTICLE;
        }
        for (ActionType type : values()) {
            if (type.name().equalsIgnoreCase(raw)) {
                return type;
            }
        }
        return SPAWN_PARTICLE;
    }
}
