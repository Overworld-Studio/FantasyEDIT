package com.fantasy.item.paper.behavior;

import java.util.Locale;

/**
 * 服务端动作枚举，字符串值与 Fabric 客户端一致。
 */
public enum ActionType {
    SPAWN_PARTICLE,
    PLAY_SOUND,
    APPLY_POTION,
    EXECUTE_COMMAND,
    CUSTOM_SCRIPT_HOOK;

    public static ActionType parse(String raw) {
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
