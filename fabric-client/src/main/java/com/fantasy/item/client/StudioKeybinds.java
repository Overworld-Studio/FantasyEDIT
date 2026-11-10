package com.fantasy.item.client;

import com.fantasy.item.client.gui.StudioScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * 快捷键 U：在无界面状态下呼出工作室。
 * 已经打开任意界面时不再抢占按键，避免和背包/聊天冲突。
 */
public final class StudioKeybinds {

    public static final KeyMapping OPEN_STUDIO = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.fantasy_item.studio",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_U,
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
