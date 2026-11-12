package com.fantasy.edit.behavior;

/**
 * 行为触发条件。字符串形式写入 JSON，避免反序列化时因未知枚举值整体失败。
 */
public enum TriggerType {
    ON_RIGHT_CLICK("右键使用"),
    ON_BLOCK_PLACE("放置方块"),
    ON_ENTITY_HIT("攻击实体"),
    ON_TICK("持续 Tick");

    private final String label;

    TriggerType(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public TriggerType next() {
        TriggerType[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static TriggerType parse(String raw) {
        if (raw == null) {
            return ON_RIGHT_CLICK;
        }
        for (TriggerType type : values()) {
            if (type.name().equalsIgnoreCase(raw)) {
                return type;
            }
        }
        return ON_RIGHT_CLICK;
    }
}
