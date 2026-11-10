package com.fantasy.item.paper.behavior;

import java.util.Locale;

/**
 * 服务端触发条件枚举，字符串值与 Fabric 客户端一致。
 */
public enum TriggerType {
    ON_RIGHT_CLICK,
    ON_BLOCK_PLACE,
    ON_ENTITY_HIT,
    ON_TICK;

    public static TriggerType parse(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
