package com.fantasy.edit.client;

import com.fantasy.edit.client.gui.StudioScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * 快捷键 U：在无界面状态下呼出工作室。
 * 已经打开任意界面时不再抢占按键，避免和背包/聊天冲突。
 */
public final class StudioKeybinds {

    /** GLFW_KEY_U。GLFW 不在编译期类路径上，直接使用键码常量。 */
    private static final int KEY_U = 85;

    public static final KeyMapping OPEN_STUDIO = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.fantasy_edit.studio",
                    InputConstants.Type.KEYBOARD,
                    KEY_U,
                    KeyMapping.Category.MISC));

    private StudioKeybinds() {
    }

    public static void register() {
        // 静态字段已经在类加载时完成注册
    }

    public static void tick(Minecraft minecraft) {
        boolean opened = false;
        while (OPEN_STUDIO.consumeClick()) {
            opened = true;
        }
        if (opened && minecraft.gui.screen() == null) {
            minecraft.gui.setScreen(new StudioScreen());
        }
    }
}
