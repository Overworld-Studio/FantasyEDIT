package com.fantasy.item.builder;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * 建造模式 HUD：模式、笔刷、选区、剪贴板与历史状态。
 */
public final class BuildHud implements HudElement {

    private static final int COLOR_TITLE = 0xFF9FE8FF;
    private static final int COLOR_TEXT = 0xFFE6E6EE;
    private static final int COLOR_HINT = 0xFF8A8A96;
    private static final int BG = 0xB0100E16;

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        if (!ClientBuildEngine.active() || minecraft.gui.screen() != null) {
            return;
        }

        int x = 8;
        int y = 8;
        graphics.fill(x - 4, y - 4, x + 250, y + 74, BG);
        graphics.fill(x - 4, y - 4, x + 250, y - 2, 0xFF6C4BD8);

        graphics.text(minecraft.font, Component.literal("快速搭建 · " + ClientBuildEngine.mode().label()
                + " §8| §f" + ClientBuildEngine.brushShape().label() + " r=" + ClientBuildEngine.radius()
                + " " + (ClientBuildEngine.erasing() ? "抹除" : "涂抹")), x, y, COLOR_TITLE);

        graphics.text(minecraft.font, Component.literal("选区: " + ClientBuildEngine.selection().describe()),
                x, y + 13, COLOR_TEXT);
        graphics.text(minecraft.font, Component.literal("剪贴板: " + ClientBuildEngine.clipboard().describe()),
                x, y + 25, COLOR_TEXT);
        graphics.text(minecraft.font, Component.literal("历史: 撤销 " + ClientBuildEngine.history().undoSize()
                + " / 重做 " + ClientBuildEngine.history().redoSize()), x, y + 37, COLOR_HINT);
        graphics.text(minecraft.font, Component.literal(ClientBuildEngine.status()),
                x, y + 49, COLOR_TEXT);
        graphics.text(minecraft.font, Component.literal("P 面板 · B 关闭 · [ ] 半径 · Z/Y 撤销重做"),
                x, y + 61, COLOR_HINT);
    }
}
