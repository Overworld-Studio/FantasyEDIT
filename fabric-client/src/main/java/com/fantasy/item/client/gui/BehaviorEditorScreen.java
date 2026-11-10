package com.fantasy.item.client.gui;

import com.fantasy.item.behavior.ActionType;
import com.fantasy.item.behavior.BehaviorRule;
import com.fantasy.item.behavior.ClientBehaviorRuntime;
import com.fantasy.item.behavior.ScriptHooks;
import com.fantasy.item.behavior.TriggerType;
import com.fantasy.item.text.LegacyText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 行为编排面板：可视化组合「触发器 → 动作」，保存后立即被动态事件总线读取。
 */
public final class BehaviorEditorScreen extends Screen {

    private static final int PANEL_W = 560;
    private static final int PANEL_H = 300;
    private static final int ROW_HEIGHT = 20;
    private static final int PAGE_SIZE = 9;
    private static final int COLOR_TEXT = 0xFFD8D8E0;
    private static final int COLOR_LABEL = 0xFFB79CFF;
    private static final int COLOR_HINT = 0xFF8A8A96;

    private final Screen parent;
    private final Consumer<List<BehaviorRule>> onDone;
    private final List<BehaviorRule> rules = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int contentY;

    private int editingIndex = -1;
    private TriggerType trigger = TriggerType.ON_RIGHT_CLICK;
    private ActionType action = ActionType.SPAWN_PARTICLE;
    private String value = ActionType.SPAWN_PARTICLE.defaultValue();
    private int amount = 12;
    private int duration = 100;
    private int amplifier = 0;
    private boolean asPlayer = true;
    private int cooldown = 10;
    private String status = "";

    private EditBox valueBox;

    public BehaviorEditorScreen(Screen parent, List<BehaviorRule> initial, Consumer<List<BehaviorRule>> onDone) {
        super(Component.literal("Fantasy:Item 行为编排"));
        this.parent = parent;
        this.onDone = onDone;
        if (initial != null) {
            for (BehaviorRule rule : initial) {
                if (rule != null) {
                    this.rules.add(rule.copy());
                }
            }
        }
    }

    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;
        this.contentY = this.panelY + 48;

        int listX = this.panelX + 10;
        int listW = 200;
        int formX = this.panelX + 222;
        int formW = 150;

        for (int i = 0; i < PAGE_SIZE && i < this.rules.size(); i++) {
            int index = i;
            BehaviorRule rule = this.rules.get(i);
            String prefix = index == this.editingIndex ? "▶ " : "";
            addRenderableWidget(Button.builder(Component.literal(prefix + shorten(rule.describe(), 28)), button -> selectRule(index))
                    .bounds(listX, this.contentY + i * ROW_HEIGHT, listW, 18).build());
        }
        if (this.rules.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("＋ 新建规则"), button -> resetForm())
                    .bounds(listX, this.contentY, listW, 18).build());
        }

        addRenderableWidget(Button.builder(Component.literal("触发器: " + this.trigger.label()), button -> {
            syncForm();
            this.trigger = this.trigger.next();
            rebuildWidgets();
        }).bounds(formX, this.contentY, formW + 30, 18).build());

        addRenderableWidget(Button.builder(Component.literal("动作: " + this.action.label()), button -> {
            syncForm();
            this.action = this.action.next();
            if (this.value == null || this.value.isBlank()) {
                this.value = this.action.defaultValue();
            }
            rebuildWidgets();
        }).bounds(formX, this.contentY + 22, formW + 30, 18).build());

        this.valueBox = new EditBox(this.font, formX + 74, this.contentY + 46, formW - 44, 18, Component.literal("参数"));
        this.valueBox.setMaxLength(128);
        this.valueBox.setValue(this.value == null ? "" : this.value);
        this.valueBox.setResponder(text -> this.value = text);
        addRenderableWidget(this.valueBox);

        addRenderableWidget(Button.builder(Component.literal("参数 -"), button -> {
            syncForm();
            this.amount = Math.max(1, this.amount - 1);
            rebuildWidgets();
        }).bounds(formX, this.contentY + 70, 62, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            syncForm();
            this.amount = Math.min(64, this.amount + 1);
            rebuildWidgets();
        }).bounds(formX + 66, this.contentY + 70, 24, 18).build());

        addRenderableWidget(Button.builder(Component.literal("时长 -"), button -> {
            syncForm();
            this.duration = Math.max(1, this.duration - 20);
            rebuildWidgets();
        }).bounds(formX, this.contentY + 92, 62, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            syncForm();
            this.duration = Math.min(72000, this.duration + 20);
            rebuildWidgets();
        }).bounds(formX + 66, this.contentY + 92, 24, 18).build());

        addRenderableWidget(Button.builder(Component.literal("等级 -"), button -> {
            syncForm();
            this.amplifier = Math.max(0, this.amplifier - 1);
            rebuildWidgets();
        }).bounds(formX, this.contentY + 114, 62, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            syncForm();
            this.amplifier = Math.min(4, this.amplifier + 1);
            rebuildWidgets();
        }).bounds(formX + 66, this.contentY + 114, 24, 18).build());

        addRenderableWidget(Button.builder(Component.literal("身份: " + (this.asPlayer ? "玩家" : "控制台")), button -> {
            syncForm();
            this.asPlayer = !this.asPlayer;
            rebuildWidgets();
        }).bounds(formX, this.contentY + 136, 90, 18).build());

        addRenderableWidget(Button.builder(Component.literal("冷却 -"), button -> {
            syncForm();
            this.cooldown = Math.max(0, this.cooldown - 5);
            rebuildWidgets();
        }).bounds(formX, this.contentY + 158, 62, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            syncForm();
            this.cooldown = Math.min(1200, this.cooldown + 5);
            rebuildWidgets();
        }).bounds(formX + 66, this.contentY + 158, 24, 18).build());

        addRenderableWidget(Button.builder(Component.literal(this.editingIndex >= 0 ? "更新规则" : "写入规则"), button -> commitRule())
                .bounds(listX, this.contentY + 202, listW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("本地预览"), button -> previewRule())
                .bounds(listX, this.contentY + 226, listW, 18).build());
        addRenderableWidget(Button.builder(Component.literal("清空全部"), button -> {
            this.rules.clear();
            this.editingIndex = -1;
            this.status = "§e已清空，点击「完成」写回工作室";
            rebuildWidgets();
        }).bounds(listX, this.contentY + 148, listW, 18).build());

        addRenderableWidget(Button.builder(Component.literal("完成"), button -> onClose())
                .bounds(this.panelX + PANEL_W - 70, this.panelY + PANEL_H - 26, 60, 20).build());
    }

    private void selectRule(int index) {
        BehaviorRule rule = this.rules.get(index);
        this.editingIndex = index;
        this.trigger = rule.triggerType();
        this.action = rule.actionType();
        this.value = rule.value;
        this.amount = rule.amount;
        this.duration = rule.duration;
        this.amplifier = rule.amplifier;
        this.asPlayer = rule.asPlayer;
        this.cooldown = rule.cooldownTicks;
        this.status = "§b正在编辑第 " + (index + 1) + " 条";
        rebuildWidgets();
    }

    private void resetForm() {
        this.editingIndex = -1;
        this.trigger = TriggerType.ON_RIGHT_CLICK;
        this.action = ActionType.SPAWN_PARTICLE;
        this.value = ActionType.SPAWN_PARTICLE.defaultValue();
        this.amount = 12;
        this.duration = 100;
        this.amplifier = 0;
        this.asPlayer = true;
        this.cooldown = 10;
        rebuildWidgets();
    }

    private void syncForm() {
        if (this.valueBox != null) {
            this.value = this.valueBox.getValue();
        }
    }

    private BehaviorRule currentRule() {
        syncForm();
        BehaviorRule rule = new BehaviorRule();
        rule.trigger = this.trigger.name();
        rule.action = this.action.name();
        rule.value = this.value == null ? "" : this.value.trim();
        rule.amount = this.amount;
        rule.duration = this.duration;
        rule.amplifier = this.amplifier;
        rule.asPlayer = this.asPlayer;
        rule.cooldownTicks = this.cooldown;
        return rule;
    }

    private void commitRule() {
        BehaviorRule rule = currentRule();
        if (!rule.isValid()) {
            this.status = "§c参数非法：长度 ≤128、数量 1~64、时长 ≤72000、等级 ≤4";
            rebuildWidgets();
            return;
        }
        if (this.action == ActionType.CUSTOM_SCRIPT_HOOK && !ScriptHooks.exists(rule.value)) {
            this.status = "§e未知脚本钩子，可用: " + String.join(", ", ScriptHooks.names());
            rebuildWidgets();
            return;
        }
        if (this.editingIndex >= 0 && this.editingIndex < this.rules.size()) {
            this.rules.set(this.editingIndex, rule);
        } else if (this.rules.size() < BehaviorRule.MAX_RULES) {
            this.rules.add(rule);
            this.editingIndex = this.rules.size() - 1;
        } else {
            this.status = "§c已达到 " + BehaviorRule.MAX_RULES + " 条上限";
            rebuildWidgets();
            return;
        }
        this.status = "§a已写入内存规则，点击「完成」返回并保存";
        rebuildWidgets();
    }

    private void previewRule() {
        BehaviorRule rule = currentRule();
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        if (rule.actionType() == ActionType.CUSTOM_SCRIPT_HOOK && !ScriptHooks.exists(rule.value)) {
            this.status = "§e未知脚本钩子: " + rule.value;
            rebuildWidgets();
            return;
        }
        ClientBehaviorRuntime.preview(rule, this.minecraft.player);
        this.status = "§b已本地预览（仅表现层）";
        rebuildWidgets();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0xB8000000);
        graphics.fill(this.panelX, this.panelY, this.panelX + PANEL_W, this.panelY + PANEL_H, 0xF0141218);
        graphics.fill(this.panelX, this.panelY, this.panelX + PANEL_W, this.panelY + 2, 0xFF6C4BD8);
        graphics.fill(this.panelX + 212, this.panelY + 32, this.panelX + 213, this.panelY + PANEL_H - 34, 0xFF2A2338);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int listX = this.panelX + 10;
        int formX = this.panelX + 222;

        graphics.text(this.font, Component.literal("Fantasy:Item · 行为编排（" + this.rules.size() + "/"
                + BehaviorRule.MAX_RULES + "）"), this.panelX + 12, this.panelY + 12, 0xFFFFFFFF);
        graphics.text(this.font, Component.literal("规则列表"), listX, this.contentY - 11, COLOR_LABEL);
        graphics.text(this.font, Component.literal("触发 → 动作"), formX, this.contentY - 11, COLOR_LABEL);

        graphics.text(this.font, Component.literal("参数"), formX, this.contentY + 50, COLOR_TEXT);

        String amountLabel = switch (this.action) {
            case SPAWN_PARTICLE -> "粒子数量: " + this.amount;
            case PLAY_SOUND -> "音量: " + this.amount;
            default -> "数量: " + this.amount;
        };
        graphics.text(this.font, Component.literal(amountLabel), formX + 94, this.contentY + 75, COLOR_TEXT);
        graphics.text(this.font, Component.literal("时长: " + (this.duration / 20.0F) + "s"),
                formX + 94, this.contentY + 97, COLOR_TEXT);
        graphics.text(this.font, Component.literal("等级: " + (this.amplifier + 1)),
                formX + 94, this.contentY + 119, COLOR_TEXT);
        graphics.text(this.font, Component.literal("冷却: " + this.cooldown + "t"),
                formX + 94, this.contentY + 163, COLOR_TEXT);

        if (this.action == ActionType.CUSTOM_SCRIPT_HOOK) {
            graphics.text(this.font, Component.literal("钩子: " + String.join(" / ", ScriptHooks.names())),
                    formX, this.contentY + 182, COLOR_HINT);
        } else {
            graphics.text(this.font, Component.literal(this.action == ActionType.EXECUTE_COMMAND
                            ? "指令无需 / 前缀" : "ID 可省略 minecraft:"),
                    formX, this.contentY + 182, COLOR_HINT);
        }

        if (this.status != null && !this.status.isEmpty()) {
            graphics.text(this.font, LegacyText.parse(this.status),
                    listX, this.panelY + PANEL_H - 40, 0xFFFFFFFF);
        }
        graphics.text(this.font, Component.literal("客户端负责表现层，服务端负责状态效果与指令"),
                listX, this.panelY + PANEL_H - 52, COLOR_HINT);
    }

    @Override
    public void onClose() {
        if (this.onDone != null) {
            this.onDone.accept(new ArrayList<>(this.rules));
        }
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, Math.max(0, max - 1)) + "…";
    }
}
