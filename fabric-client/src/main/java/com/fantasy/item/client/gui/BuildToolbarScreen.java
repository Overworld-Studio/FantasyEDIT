package com.fantasy.item.client.gui;

import com.fantasy.item.builder.ClientBuildEngine;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 快速搭建面板：把全部建造操作收敛到一个可视化面板里，避免记忆大量快捷键。
 */
public final class BuildToolbarScreen extends Screen {

    private static final int PANEL_W = 452;
    private static final int PANEL_H = 238;
    private static final int COLOR_TEXT = 0xFFD8D8E0;
    private static final int COLOR_LABEL = 0xFF9FE8FF;
    private static final int COLOR_HINT = 0xFF8A8A96;

    private int panelX;
    private int panelY;
    private int contentY;

    public BuildToolbarScreen() {
        super(Component.literal("Fantasy:Item 快速搭建"));
    }

    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;
        this.contentY = this.panelY + 40;

        int colW = 104;
        int gap = 8;
        int[] xs = new int[4];
        for (int i = 0; i < 4; i++) {
            xs[i] = this.panelX + 12 + i * (colW + gap);
        }
        int rowH = 24;

        // 第 1 行：模式 / 笔刷 / 半径
        addRenderableWidget(Button.builder(
                        Component.literal("模式: " + ClientBuildEngine.mode().label()),
                        button -> {
                            ClientBuildEngine.cycleMode();
                            rebuildWidgets();
                        })
                .bounds(xs[0], this.contentY, colW, 20).build());

        addRenderableWidget(Button.builder(
                        Component.literal("笔刷: " + ClientBuildEngine.brushShape().label()),
                        button -> {
                            ClientBuildEngine.cycleBrush();
                            rebuildWidgets();
                        })
                .bounds(xs[1], this.contentY, colW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("半径 -"),
                        button -> {
                            ClientBuildEngine.changeRadius(-1);
                            rebuildWidgets();
                        })
                .bounds(xs[2], this.contentY, colW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("半径 +"),
                        button -> {
                            ClientBuildEngine.changeRadius(1);
                            rebuildWidgets();
                        })
                .bounds(xs[3], this.contentY, colW, 20).build());

        // 第 2 行：选区角点
        addRenderableWidget(Button.builder(Component.literal("选区点 A"),
                        button -> {
                            ClientBuildEngine.markCorner(this.minecraft, true);
                            rebuildWidgets();
                        })
                .bounds(xs[0], this.contentY + rowH, colW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("选区点 B"),
                        button -> {
                            ClientBuildEngine.markCorner(this.minecraft, false);
                            rebuildWidgets();
                        })
                .bounds(xs[1], this.contentY + rowH, colW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("清空选区"),
                        button -> {
                            ClientBuildEngine.selection().clear();
                            ClientBuildEngine.setStatus("§7选区已清空");
                            rebuildWidgets();
                        })
                .bounds(xs[2], this.contentY + rowH, colW, 20).build());

        addRenderableWidget(Button.builder(
                        Component.literal(ClientBuildEngine.erasing() ? "笔刷: 抹除" : "笔刷: 涂抹"),
                        button -> {
                            ClientBuildEngine.toggleErase();
                            rebuildWidgets();
                        })
                .bounds(xs[3], this.contentY + rowH, colW, 20).build());

        // 第 3 行：编辑
        addRenderableWidget(Button.builder(Component.literal("填充选区"),
                        button -> {
                            ClientBuildEngine.fillSelection(this.minecraft);
                            rebuildWidgets();
                        })
                .bounds(xs[0], this.contentY + rowH * 2, colW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("替换同块"),
                        button -> {
                            ClientBuildEngine.replaceSelection(this.minecraft, true);
                            rebuildWidgets();
                        })
                .bounds(xs[1], this.contentY + rowH * 2, colW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("替换全部"),
                        button -> {
                            ClientBuildEngine.replaceSelection(this.minecraft, false);
                            rebuildWidgets();
                        })
                .bounds(xs[2], this.contentY + rowH * 2, colW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("复制选区"),
                        button -> {
                            ClientBuildEngine.copySelection();
                            rebuildWidgets();
                        })
                .bounds(xs[3], this.contentY + rowH * 2, colW, 20).build());

        // 第 4 行：剪贴板与历史
        addRenderableWidget(Button.builder(Component.literal("粘贴到准星"),
                        button -> {
                            ClientBuildEngine.pasteClipboard(this.minecraft);
                            rebuildWidgets();
                        })
                .bounds(xs[0], this.contentY + rowH * 3, colW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("旋转 90°"),
                        button -> {
                            ClientBuildEngine.clipboard().rotate();
                            ClientBuildEngine.setStatus("§b剪贴板: " + ClientBuildEngine.clipboard().describe());
                            rebuildWidgets();
                        })
                .bounds(xs[1], this.contentY + rowH * 3, colW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("镜像"),
                        button -> {
                            ClientBuildEngine.clipboard().toggleMirror();
                            ClientBuildEngine.setStatus("§b剪贴板: " + ClientBuildEngine.clipboard().describe());
                            rebuildWidgets();
                        })
                .bounds(xs[2], this.contentY + rowH * 3, colW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("撤销"),
                        button -> {
                            ClientBuildEngine.undo(this.minecraft);
                            rebuildWidgets();
                        })
                .bounds(xs[3], this.contentY + rowH * 3, colW, 20).build());

        // 第 5 行：重做 / 关闭
        addRenderableWidget(Button.builder(Component.literal("重做"),
                        button -> {
                            ClientBuildEngine.redo(this.minecraft);
                            rebuildWidgets();
                        })
                .bounds(xs[0], this.contentY + rowH * 4, colW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("关闭"),
                        button -> onClose())
                .bounds(xs[3], this.contentY + rowH * 4, colW, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0xB8000000);
        graphics.fill(this.panelX, this.panelY, this.panelX + PANEL_W, this.panelY + PANEL_H, 0xF0141218);
        graphics.fill(this.panelX, this.panelY, this.panelX + PANEL_W, this.panelY + 2, 0xFF2EC4B6);
        graphics.fill(this.panelX, this.panelY + PANEL_H - 2, this.panelX + PANEL_W, this.panelY + PANEL_H, 0xFF2EC4B6);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.text(this.font, Component.literal("Fantasy:Item · 世界快速搭建（仅创造模式）"),
                this.panelX + 12, this.panelY + 12, 0xFFFFFFFF);
        graphics.text(this.font, Component.literal("模式提示: " + ClientBuildEngine.mode().hint()),
                this.panelX + 12, this.panelY + 24, COLOR_LABEL);

        graphics.text(this.font, Component.literal("选区: " + ClientBuildEngine.selection().describe()),
                this.panelX + 12, this.contentY + 96, COLOR_TEXT);
        graphics.text(this.font, Component.literal("剪贴板: " + ClientBuildEngine.clipboard().describe()),
                this.panelX + 12, this.contentY + 110, COLOR_TEXT);
        graphics.text(this.font, Component.literal("手持方块: " + heldName()),
                this.panelX + 12, this.contentY + 124, COLOR_TEXT);
        graphics.text(this.font, Component.literal("历史: 撤销 " + ClientBuildEngine.history().undoSize()
                        + " · 重做 " + ClientBuildEngine.history().redoSize()),
                this.panelX + 12, this.contentY + 138, COLOR_HINT);
        graphics.text(this.font, Component.literal(ClientBuildEngine.status()),
                this.panelX + 12, this.contentY + 156, COLOR_TEXT);
        graphics.text(this.font, Component.literal("提示：笔刷模式下按住左键抹除、右键涂抹"),
                this.panelX + 12, this.panelY + PANEL_H - 16, COLOR_HINT);
    }

    private String heldName() {
        BlockState state = ClientBuildEngine.heldBlock(this.minecraft);
        if (state == null) {
            return "无（请手持方块）";
        }
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
