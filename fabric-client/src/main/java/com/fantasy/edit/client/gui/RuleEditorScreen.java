package com.fantasy.edit.client.gui;

import com.fantasy.edit.rule.RuleEngine;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 运行时规则面板：列出 {@link RuleEngine} 里的全部规则，点一下就改，改完立刻生效。
 *
 * <p>面板本身不持有任何状态，所有值都实时从规则引擎读取，因此不存在「面板显示旧值」的问题。</p>
 */
public final class RuleEditorScreen extends Screen {

    private static final int PANEL_W = 470;
    private static final int PANEL_H = 292;
    private static final int ROWS_PER_PAGE = 8;
    private static final int COLOR_TEXT = 0xFFD8D8E0;
    private static final int COLOR_LABEL = 0xFF9FE8FF;
    private static final int COLOR_HINT = 0xFF8A8A96;

    private int panelX;
    private int panelY;
    private int listY;

    private String category;
    private int page;

    public RuleEditorScreen() {
        super(Component.literal("Fantasy:EDIT 运行时规则"));
    }

    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;
        this.listY = this.panelY + 54;

        buildCategoryRow();
        buildRuleRows();
        buildFooter();
    }

    private void buildCategoryRow() {
        List<String> tabs = new ArrayList<>();
        tabs.add("全部");
        tabs.addAll(RuleEngine.categories());

        int x = this.panelX + 12;
        int width = 108;
        int gap = 4;
        int rowWidth = PANEL_W - 24;
        int perRow = Math.max(1, (rowWidth + gap) / (width + gap));
        for (int i = 0; i < tabs.size(); i++) {
            String name = tabs.get(i);
            int column = i % perRow;
            int row = i / perRow;
            String caption = name.equals("全部") ? "全部" : name;
            if (name.equals(this.category)) {
                caption = "▶ " + caption;
            }
            addRenderableWidget(Button.builder(Component.literal(caption), button -> {
                        this.category = name.equals("全部") ? null : name;
                        this.page = 0;
                        rebuildWidgets();
                    })
                    .bounds(x + column * (width + gap), this.panelY + 32 + row * 18, width, 16)
                    .build());
        }
    }

    private List<RuleEngine.Rule> visibleRules() {
        List<RuleEngine.Rule> result = new ArrayList<>();
        for (RuleEngine.Rule rule : RuleEngine.rules()) {
            if (this.category == null || this.category.equals(rule.category)) {
                result.add(rule);
            }
        }
        return result;
    }

    private int pageCount(List<RuleEngine.Rule> rules) {
        return Math.max(1, (rules.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
    }

    private void buildRuleRows() {
        List<RuleEngine.Rule> rules = visibleRules();
        this.page = Math.max(0, Math.min(pageCount(rules) - 1, this.page));

        int start = this.page * ROWS_PER_PAGE;
        for (int i = 0; i < ROWS_PER_PAGE; i++) {
            int index = start + i;
            if (index >= rules.size()) {
                break;
            }
            RuleEngine.Rule rule = rules.get(index);
            int y = this.listY + i * 22;

            addRenderableWidget(Button.builder(
                            Component.literal(rule.label + " = " + RuleEngine.displayValue(rule)),
                            button -> {
                                RuleEngine.bump(rule.id, true);
                                rebuildWidgets();
                            })
                    .bounds(this.panelX + 12, y, 300, 20)
                    .build());

            if (rule.kind != RuleEngine.Kind.BOOLEAN) {
                addRenderableWidget(Button.builder(Component.literal("−"), button -> {
                            RuleEngine.bump(rule.id, false);
                            rebuildWidgets();
                        })
                        .bounds(this.panelX + 316, y, 22, 20)
                        .build());
                addRenderableWidget(Button.builder(Component.literal("+"), button -> {
                            RuleEngine.bump(rule.id, true);
                            rebuildWidgets();
                        })
                        .bounds(this.panelX + 342, y, 22, 20)
                        .build());
            }

            addRenderableWidget(Button.builder(Component.literal("复位"), button -> {
                        RuleEngine.reset(rule.id);
                        rebuildWidgets();
                    })
                    .bounds(this.panelX + 372, y, 42, 20)
                    .build());
        }
    }

    private void buildFooter() {
        int y = this.panelY + PANEL_H - 26;

        addRenderableWidget(Button.builder(Component.literal("上一页"), button -> {
                    this.page = Math.max(0, this.page - 1);
                    rebuildWidgets();
                })
                .bounds(this.panelX + 12, y, 62, 20).build());

        addRenderableWidget(Button.builder(Component.literal("下一页"), button -> {
                    this.page++;
                    rebuildWidgets();
                })
                .bounds(this.panelX + 78, y, 62, 20).build());

        addRenderableWidget(Button.builder(Component.literal("全部复位"), button -> {
                    RuleEngine.resetAll();
                    rebuildWidgets();
                })
                .bounds(this.panelX + 146, y, 76, 20).build());

        addRenderableWidget(Button.builder(Component.literal("关闭"), button -> onClose())
                .bounds(this.panelX + PANEL_W - 74, y, 62, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0xB8000000);
        graphics.fill(this.panelX, this.panelY, this.panelX + PANEL_W, this.panelY + PANEL_H, 0xF0141218);
        graphics.fill(this.panelX, this.panelY, this.panelX + PANEL_W, this.panelY + 2, 0xFFE0A82E);
        graphics.fill(this.panelX, this.panelY + PANEL_H - 2, this.panelX + PANEL_W, this.panelY + PANEL_H, 0xFFE0A82E);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        List<RuleEngine.Rule> rules = visibleRules();
        graphics.text(this.font, Component.literal("Fantasy:EDIT · 运行时规则"), this.panelX + 12, this.panelY + 10,
                0xFFFFFFFF);
        graphics.text(this.font, Component.literal("改动立即生效，无需重启游戏；左侧点击 + / −，右侧复位"),
                this.panelX + 12, this.panelY + 21, COLOR_LABEL);

        if (rules.isEmpty()) {
            graphics.text(this.font, Component.literal("当前分类下没有规则"), this.panelX + 12, this.listY + 6,
                    COLOR_HINT);
        }

        graphics.text(this.font, Component.literal("第 " + (this.page + 1) + " / " + pageCount(rules) + " 页 · 共 "
                        + rules.size() + " 条"),
                this.panelX + 232, this.panelY + PANEL_H - 21, COLOR_HINT);
        graphics.text(this.font, Component.literal(RuleEngine.status()), this.panelX + 12,
                this.panelY + PANEL_H - 40, COLOR_TEXT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
