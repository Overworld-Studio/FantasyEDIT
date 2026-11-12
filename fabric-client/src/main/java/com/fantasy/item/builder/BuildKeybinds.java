package com.fantasy.item.builder;

import com.fantasy.item.client.gui.BuildToolbarScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * 快速搭建快捷键。GLFW 不在编译期类路径上，因此直接使用键码。
 *
 * <p>B 开关；P 面板；X 切模式；[ ] 半径；Z / Y 撤销重做；C / V 复制粘贴；
 * G / H 选区角点；N 清空选区；R 旋转；M 镜像；L 填充；K 涂抹/抹除。</p>
 */
public final class BuildKeybinds {

    private static KeyMapping bind(String name, int key) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                name, InputConstants.Type.KEYBOARD, key, KeyMapping.Category.CREATIVE));
    }

    public static final KeyMapping TOGGLE = bind("key.fantasy_item.build.toggle", 66);
    public static final KeyMapping PANEL = bind("key.fantasy_item.build.panel", 80);
    public static final KeyMapping MODE = bind("key.fantasy_item.build.mode", 88);
    public static final KeyMapping RADIUS_DOWN = bind("key.fantasy_item.build.radius_down", 91);
    public static final KeyMapping RADIUS_UP = bind("key.fantasy_item.build.radius_up", 93);
    public static final KeyMapping UNDO = bind("key.fantasy_item.build.undo", 90);
    public static final KeyMapping REDO = bind("key.fantasy_item.build.redo", 89);
    public static final KeyMapping COPY = bind("key.fantasy_item.build.copy", 67);
    public static final KeyMapping PASTE = bind("key.fantasy_item.build.paste", 86);
    public static final KeyMapping CORNER_A = bind("key.fantasy_item.build.corner_a", 71);
    public static final KeyMapping CORNER_B = bind("key.fantasy_item.build.corner_b", 72);
    public static final KeyMapping CLEAR = bind("key.fantasy_item.build.clear", 78);
    public static final KeyMapping ROTATE = bind("key.fantasy_item.build.rotate", 82);
    public static final KeyMapping MIRROR = bind("key.fantasy_item.build.mirror", 77);
    public static final KeyMapping FILL = bind("key.fantasy_item.build.fill", 76);
    public static final KeyMapping ERASE = bind("key.fantasy_item.build.erase", 75);

    private BuildKeybinds() {
    }

    public static void register() {
        // 静态字段已经在类加载时完成注册
    }

    public static void tick(Minecraft minecraft) {
        if (consume(TOGGLE)) {
            ClientBuildEngine.toggle(minecraft);
        }
        if (!ClientBuildEngine.active()) {
            drain();
            return;
        }
        if (minecraft.gui.screen() != null) {
            drain();
            return;
        }

        if (consume(PANEL)) {
            minecraft.gui.setScreen(new BuildToolbarScreen());
        }
        if (consume(MODE)) {
            ClientBuildEngine.cycleMode();
        }
        if (consume(RADIUS_DOWN)) {
            ClientBuildEngine.changeRadius(-1);
        }
        if (consume(RADIUS_UP)) {
            ClientBuildEngine.changeRadius(1);
        }
        if (consume(ERASE)) {
            ClientBuildEngine.toggleErase();
        }
        if (consume(UNDO)) {
            ClientBuildEngine.undo(minecraft);
        }
        if (consume(REDO)) {
            ClientBuildEngine.redo(minecraft);
        }
        if (consume(COPY)) {
            ClientBuildEngine.copySelection();
        }
        if (consume(PASTE)) {
            ClientBuildEngine.pasteClipboard(minecraft);
        }
        if (consume(CORNER_A)) {
            ClientBuildEngine.markCorner(minecraft, true);
        }
        if (consume(CORNER_B)) {
            ClientBuildEngine.markCorner(minecraft, false);
        }
        if (consume(CLEAR)) {
            ClientBuildEngine.selection().clear();
            ClientBuildEngine.setStatus("§7选区已清空");
        }
        if (consume(ROTATE)) {
            ClientBuildEngine.clipboard().rotate();
            ClientBuildEngine.setStatus("§b剪贴板: " + ClientBuildEngine.clipboard().describe());
        }
        if (consume(MIRROR)) {
            ClientBuildEngine.clipboard().toggleMirror();
            ClientBuildEngine.setStatus("§b剪贴板: " + ClientBuildEngine.clipboard().describe());
        }
        if (consume(FILL)) {
            ClientBuildEngine.fillSelection(minecraft);
        }
    }

    private static boolean consume(KeyMapping mapping) {
        boolean clicked = false;
        while (mapping.consumeClick()) {
            clicked = true;
        }
        return clicked;
    }

    private static void drain() {
        consume(TOGGLE);
        consume(PANEL);
        consume(MODE);
        consume(RADIUS_DOWN);
        consume(RADIUS_UP);
        consume(UNDO);
        consume(REDO);
        consume(COPY);
        consume(PASTE);
        consume(CORNER_A);
        consume(CORNER_B);
        consume(CLEAR);
        consume(ROTATE);
        consume(MIRROR);
        consume(FILL);
        consume(ERASE);
    }
}
