package com.fantasy.item.client.gui;

import com.fantasy.item.FantasyItem;
import com.fantasy.item.behavior.BehaviorRule;
import com.fantasy.item.client.assets.AssetReloader;
import com.fantasy.item.client.assets.AssetWorkspace;
import com.fantasy.item.client.assets.ModelWriter;
import com.fantasy.item.definition.BlockSettings;
import com.fantasy.item.definition.BlockTemplate;
import com.fantasy.item.definition.DefinitionType;
import com.fantasy.item.definition.ItemDefinition;
import com.fantasy.item.network.BehaviorSync;
import com.fantasy.item.registry.DynamicRegistry;
import com.fantasy.item.text.LegacyText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 零宿主可视化工作室。
 *
 * 布局：左侧定义列表 / 中间属性表单 / 右侧 Blockbench 资产与生成按钮。
 * 全部使用原版控件，因此在不同 GUI 缩放与手柄导航下都能正常工作。
 */
public final class StudioScreen extends Screen {

    private static final int PANEL_W = 680;
    private static final int PANEL_H = 340;
    private static final int ROW_HEIGHT = 24;
    private static final int PAGE_SIZE = 7;
    private static final int COLOR_TEXT = 0xFFD8D8E0;
    private static final int COLOR_LABEL = 0xFF9FE8FF;
    private static final int COLOR_HINT = 0xFF8A8A96;

    private final List<ItemDefinition> rows = new ArrayList<>();
    private final List<Path> models = new ArrayList<>();
    private final List<Path> textures = new ArrayList<>();
    private final List<BehaviorRule> draftBehaviors = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int contentY;
    private int page;
    private int modelIndex = -1;
    private int textureIndex = -1;

    private DefinitionType mode = DefinitionType.ITEM;
    private String draftNamespace = "hyperlab";
    private String draftPath = "";
    private String draftName = "";
    private String draftLore = "";
    private String draftModel = "";
    private String draftTexture = "";
    private String draftRarity = "COMMON";
    private int draftStack = 64;
    private BlockTemplate draftTemplate = BlockTemplate.SOLID;
    private int draftLight = 0;
    private boolean draftGlint = false;
    private String status = "";
    private String selectedKey = null;
    private String originalKey = null;

    private EditBox namespaceBox;
    private EditBox pathBox;
    private EditBox nameBox;
    private MultiLineEditBox loreBox;

    public StudioScreen() {
        super(Component.literal("Fantasy:Item 虚空造物工作室"));
    }

    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;
        this.contentY = this.panelY + 52;

        refreshRows();
        refreshAssets();

        int listX = this.panelX + 10;
        int listW = 190;
        int midX = this.panelX + 212;
        int fieldW = 210;
        int rightX = this.panelX + 434;
        int rightW = 236;

        // 顶部：模式切换
        addRenderableWidget(Button.builder(Component.literal("物品"), button -> switchMode(DefinitionType.ITEM))
                .bounds(this.panelX + PANEL_W - 152, this.panelY + 8, 70, 18).build());
        addRenderableWidget(Button.builder(Component.literal("方块"), button -> switchMode(DefinitionType.BLOCK))
                .bounds(this.panelX + PANEL_W - 78, this.panelY + 8, 70, 18).build());

        // 左列：定义列表（分页）
        int start = this.page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = start + i;
            if (index >= this.rows.size()) {
                break;
            }
            ItemDefinition definition = this.rows.get(index);
            String prefix = definition.key().equals(this.selectedKey) ? "▶ " : "";
            addRenderableWidget(Button.builder(Component.literal(prefix + definition.key()), button -> loadDefinition(definition))
                    .bounds(listX, this.contentY + i * ROW_HEIGHT, listW, 20).build());
        }
        int pageCount = Math.max(1, (this.rows.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        addRenderableWidget(Button.builder(Component.literal("‹ 上一页"), button -> {
            this.page = Math.max(0, this.page - 1);
            rebuildWidgets();
        }).bounds(listX, this.contentY + PAGE_SIZE * ROW_HEIGHT + 2, 60, 18).build());
        addRenderableWidget(Button.builder(Component.literal("下一页 ›"), button -> {
            this.page = Math.min(pageCount - 1, this.page + 1);
            rebuildWidgets();
        }).bounds(listX + listW - 60, this.contentY + PAGE_SIZE * ROW_HEIGHT + 2, 60, 18).build());

        addRenderableWidget(Button.builder(Component.literal("行为编排 (" + this.draftBehaviors.size() + ")…"), button -> openBehaviorEditor())
                .bounds(listX, this.contentY + PAGE_SIZE * ROW_HEIGHT + 26, listW, 20).build());

        // 中列：核心属性
        this.namespaceBox = addField(midX + 74, this.contentY, fieldW - 74, this.draftNamespace, 64, value -> this.draftNamespace = value);
        this.pathBox = addField(midX + 74, this.contentY + 24, fieldW - 74, this.draftPath, 64, value -> this.draftPath = value);
        this.nameBox = addField(midX + 74, this.contentY + 48, fieldW - 74, this.draftName, 128, value -> this.draftName = value);

        this.loreBox = MultiLineEditBox.builder()
                .setX(midX)
                .setY(this.contentY + 78)
                .setPlaceholder(Component.literal("多行 Lore，支持 & 颜色代码"))
                .setShowBackground(true)
                .build(this.font, fieldW, 78, Component.literal("Lore"));
        this.loreBox.setCharacterLimit(2048);
        this.loreBox.setValue(this.draftLore);
        this.loreBox.setValueListener(value -> this.draftLore = value);
        addRenderableWidget(this.loreBox);

        addRenderableWidget(Button.builder(Component.literal("稀有度: " + this.draftRarity), button -> {
            syncDraft();
            this.draftRarity = nextRarity(this.draftRarity);
            rebuildWidgets();
        }).bounds(midX, this.contentY + 164, 100, 18).build());

        addRenderableWidget(Button.builder(Component.literal("流光: " + (this.draftGlint ? "开" : "关")), button -> {
            syncDraft();
            this.draftGlint = !this.draftGlint;
            rebuildWidgets();
        }).bounds(midX + 104, this.contentY + 164, 106, 18).build());

        addRenderableWidget(Button.builder(Component.literal("-"), button -> {
            syncDraft();
            this.draftStack = Math.max(1, this.draftStack - 1);
            rebuildWidgets();
        }).bounds(midX, this.contentY + 188, 24, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            syncDraft();
            this.draftStack = Math.min(99, this.draftStack + 1);
            rebuildWidgets();
        }).bounds(midX + 70, this.contentY + 188, 24, 18).build());

        // 右列：资产与操作
        addRenderableWidget(Button.builder(Component.literal("模型: " + displayModel()), button -> {
            syncDraft();
            cycleModel();
            rebuildWidgets();
        }).bounds(rightX, this.contentY, rightW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("纹理: " + displayTexture()), button -> {
            syncDraft();
            cycleTexture();
            rebuildWidgets();
        }).bounds(rightX, this.contentY + 24, rightW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("物理模板: " + this.draftTemplate), button -> {
            syncDraft();
            this.draftTemplate = nextTemplate(this.draftTemplate);
            rebuildWidgets();
        }).bounds(rightX, this.contentY + 48, rightW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("-"), button -> {
            syncDraft();
            this.draftLight = Math.max(0, this.draftLight - 1);
            rebuildWidgets();
        }).bounds(rightX, this.contentY + 72, 24, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            syncDraft();
            this.draftLight = Math.min(15, this.draftLight + 1);
            rebuildWidgets();
        }).bounds(rightX + rightW - 24, this.contentY + 72, 24, 18).build());

        addRenderableWidget(Button.builder(Component.literal("保存并凭空生成"), button -> saveAndSpawn())
                .bounds(rightX, this.contentY + 100, rightW, 24).build());
        addRenderableWidget(Button.builder(Component.literal("给自己一份"), button -> giveCurrent())
                .bounds(rightX, this.contentY + 130, rightW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("删除定义"), button -> deleteCurrent())
                .bounds(rightX, this.contentY + 154, rightW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("重新扫描资产 + 热重载"), button -> {
            refreshAssets();
            AssetReloader.reload();
            this.status = "§b已请求资源热重载";
            rebuildWidgets();
        }).bounds(rightX, this.contentY + 178, rightW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("关闭"), button -> onClose())
                .bounds(rightX, this.contentY + 202, rightW, 20).build());
    }

    private EditBox addField(int x, int y, int width, String value, int maxLength, java.util.function.Consumer<String> responder) {
        EditBox box = new EditBox(this.font, x, y, width, 18, Component.empty());
        box.setMaxLength(maxLength);
        box.setValue(value == null ? "" : value);
        box.setResponder(responder);
        addRenderableWidget(box);
        return box;
    }

    private void refreshRows() {
        this.rows.clear();
        this.rows.addAll(FantasyItem.store().all());
        this.rows.sort(Comparator.comparing(ItemDefinition::key));
        int pageCount = Math.max(1, (this.rows.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        this.page = Math.min(this.page, pageCount - 1);
    }

    private void refreshAssets() {
        this.models.clear();
        this.models.addAll(AssetWorkspace.listModels());
        this.textures.clear();
        this.textures.addAll(AssetWorkspace.listTextures());

        this.modelIndex = -1;
        for (int i = 0; i < this.models.size(); i++) {
            if (this.models.get(i).getFileName().toString().equalsIgnoreCase(this.draftModel)) {
                this.modelIndex = i;
                break;
            }
        }
        this.textureIndex = -1;
        for (int i = 0; i < this.textures.size(); i++) {
            if (this.textures.get(i).getFileName().toString().equalsIgnoreCase(this.draftTexture)) {
                this.textureIndex = i;
                break;
            }
        }
    }

    private void cycleModel() {
        if (this.models.isEmpty()) {
            this.draftModel = "";
            this.status = "§e未找到模型，请把 Blockbench JSON 放进 config/fantasy_item/models";
            return;
        }
        this.modelIndex = (this.modelIndex + 1) % (this.models.size() + 1);
        this.draftModel = this.modelIndex >= this.models.size() ? "" : this.models.get(this.modelIndex).getFileName().toString();
    }

    private void cycleTexture() {
        if (this.textures.isEmpty()) {
            this.draftTexture = "";
            this.status = "§e未找到纹理，将自动生成占位贴图";
            return;
        }
        this.textureIndex = (this.textureIndex + 1) % (this.textures.size() + 1);
        this.draftTexture = this.textureIndex >= this.textures.size() ? "" : this.textures.get(this.textureIndex).getFileName().toString();
    }

    private String displayModel() {
        return this.draftModel == null || this.draftModel.isBlank() ? "自动 / 无" : this.draftModel;
    }

    private String displayTexture() {
        return this.draftTexture == null || this.draftTexture.isBlank() ? "同名自动匹配" : this.draftTexture;
    }

    private void switchMode(DefinitionType type) {
        syncDraft();
        this.mode = type;
        rebuildWidgets();
    }

    private void loadDefinition(ItemDefinition definition) {
        this.mode = definition.type;
        this.draftNamespace = definition.namespace;
        this.draftPath = definition.path;
        this.draftName = definition.displayName == null ? "" : definition.displayName;
        this.draftLore = definition.lore == null ? "" : String.join("\n", definition.lore);
        this.draftModel = definition.model == null ? "" : definition.model;
        this.draftTexture = definition.texture == null ? "" : definition.texture;
        this.draftGlint = definition.glint;
        this.draftRarity = definition.rarity == null ? "COMMON" : definition.rarity;
        this.draftStack = definition.maxStackSize;
        this.draftTemplate = definition.blockProperties == null ? BlockTemplate.SOLID : definition.blockProperties.template;
        this.draftLight = definition.blockProperties == null ? 0 : definition.blockProperties.lightLevel;
        this.draftBehaviors.clear();
        if (definition.behaviors != null) {
            for (BehaviorRule rule : definition.behaviors) {
                if (rule != null) {
                    this.draftBehaviors.add(rule.copy());
                }
            }
        }
        this.selectedKey = definition.key();
        this.originalKey = definition.key();
        this.status = "§a已载入 " + definition.key();
        rebuildWidgets();
    }

    private void syncDraft() {
        if (this.namespaceBox != null) {
            this.draftNamespace = this.namespaceBox.getValue();
        }
        if (this.pathBox != null) {
            this.draftPath = this.pathBox.getValue();
        }
        if (this.nameBox != null) {
            this.draftName = this.nameBox.getValue();
        }
        if (this.loreBox != null) {
            this.draftLore = this.loreBox.getValue();
        }
    }

    private ItemDefinition buildDefinition() {
        syncDraft();
        ItemDefinition definition = new ItemDefinition();
        definition.namespace = this.draftNamespace.trim().toLowerCase(Locale.ROOT);
        definition.path = this.draftPath.trim().toLowerCase(Locale.ROOT);
        definition.type = this.mode;
        definition.displayName = this.draftName;
        definition.lore = this.draftLore.lines().filter(line -> !line.isBlank()).toList();
        definition.model = this.draftModel;
        definition.texture = this.draftTexture;
        definition.glint = this.draftGlint;
        definition.rarity = this.draftRarity;
        definition.maxStackSize = this.draftStack;
        definition.blockProperties = new BlockSettings(this.draftTemplate, this.draftLight);
        definition.behaviors = new ArrayList<>();
        for (BehaviorRule rule : this.draftBehaviors) {
            if (rule != null) {
                definition.behaviors.add(rule.copy());
            }
        }
        return definition;
    }

    private void openBehaviorEditor() {
        syncDraft();
        if (this.minecraft == null) {
            return;
        }
        this.minecraft.gui.setScreen(new BehaviorEditorScreen(this, this.draftBehaviors, updated -> {
            this.draftBehaviors.clear();
            this.draftBehaviors.addAll(updated);
            this.status = "§a行为树已更新（" + updated.size() + " 条），保存后立即注入";
            rebuildWidgets();
        }));
    }

    private void saveAndSpawn() {
        ItemDefinition definition = buildDefinition();
        if (!definition.isValid()) {
            this.status = "§c非法 ID：只允许小写字母/数字/_-.，且不能使用 minecraft";
            rebuildWidgets();
            return;
        }
        try {
            if (this.originalKey != null && !this.originalKey.equals(definition.key())) {
                ItemDefinition old = FantasyItem.store().get(this.originalKey);
                if (old != null) {
                    FantasyItem.store().delete(old);
                    ModelWriter.delete(old);
                }
            }
            DynamicRegistry.ensureRegistered(definition);
            ModelWriter.write(definition);
            FantasyItem.store().save(definition);
            this.originalKey = definition.key();
            this.selectedKey = definition.key();
            AssetReloader.reload();
            give(definition);
            boolean synced = BehaviorSync.push(definition);
            this.status = "§a已凭空生成 " + definition.key()
                    + (synced ? "，行为已注入服务端" : "，行为本地即时生效");
        } catch (Exception e) {
            this.status = "§c生成失败: " + e.getMessage();
            FantasyItem.LOGGER.error("生成虚空造物失败", e);
        }
        refreshRows();
        rebuildWidgets();
    }

    private void giveCurrent() {
        ItemDefinition definition = buildDefinition();
        if (!definition.isValid()) {
            this.status = "§c当前表单不是合法定义";
            rebuildWidgets();
            return;
        }
        try {
            DynamicRegistry.ensureRegistered(definition);
            give(definition);
            this.status = "§a已放入物品栏: " + definition.key();
        } catch (RuntimeException e) {
            this.status = "§c" + e.getMessage();
        }
        rebuildWidgets();
    }

    private void give(ItemDefinition definition) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.getInventory().add(DynamicRegistry.createStack(definition));
        }
    }

    private void deleteCurrent() {
        ItemDefinition definition = buildDefinition();
        ItemDefinition stored = FantasyItem.store().get(definition.key());
        if (stored == null) {
            this.status = "§e该定义尚未保存";
        } else {
            FantasyItem.store().delete(stored);
            ModelWriter.delete(stored);
            this.status = "§e已删除 " + stored.key() + " 的 JSON 与资源（运行时对象需重启后消失）";
            this.selectedKey = null;
            this.originalKey = null;
            AssetReloader.reload();
        }
        refreshRows();
        rebuildWidgets();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0xB8000000);
        graphics.fill(this.panelX, this.panelY, this.panelX + PANEL_W, this.panelY + PANEL_H, 0xF0141218);
        graphics.fill(this.panelX, this.panelY, this.panelX + PANEL_W, this.panelY + 2, 0xFF6C4BD8);
        graphics.fill(this.panelX, this.panelY + PANEL_H - 2, this.panelX + PANEL_W, this.panelY + PANEL_H, 0xFF6C4BD8);
        graphics.fill(this.panelX + 204, this.panelY + 34, this.panelX + 205, this.panelY + PANEL_H - 12, 0xFF2A2338);
        graphics.fill(this.panelX + 426, this.panelY + 34, this.panelX + 427, this.panelY + PANEL_H - 12, 0xFF2A2338);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.text(this.font, Component.literal("Fantasy:Item · 虚空造物工作室"),
                this.panelX + 12, this.panelY + 12, 0xFFFFFFFF);

        int listX = this.panelX + 10;
        int midX = this.panelX + 212;
        int rightX = this.panelX + 434;

        graphics.text(this.font, Component.literal("定义列表 (" + this.rows.size() + ")"),
                listX, this.contentY - 12, COLOR_LABEL);
        graphics.text(this.font, Component.literal("属性表单"),
                midX, this.contentY - 12, COLOR_LABEL);
        graphics.text(this.font, Component.literal("Blockbench 资产"),
                rightX, this.contentY - 12, COLOR_LABEL);

        graphics.text(this.font, Component.literal("命名空间"), midX, this.contentY + 5, COLOR_TEXT);
        graphics.text(this.font, Component.literal("路径 ID"), midX, this.contentY + 29, COLOR_TEXT);
        graphics.text(this.font, Component.literal("显示名称"), midX, this.contentY + 53, COLOR_TEXT);
        graphics.text(this.font, Component.literal("Lore"), midX, this.contentY + 68, COLOR_TEXT);

        graphics.text(this.font, Component.literal("最大堆叠: " + this.draftStack),
                midX + 30, this.contentY + 193, COLOR_TEXT);
        graphics.text(this.font, Component.literal("发光等级: " + this.draftLight),
                rightX + 30, this.contentY + 77, COLOR_TEXT);
        graphics.text(this.font, Component.literal("类型: " + this.mode),
                rightX, this.contentY + 226, COLOR_HINT);
        graphics.text(this.font, Component.literal("行为: " + this.draftBehaviors.size() + " 条"
                        + (BehaviorSync.serverActive() ? "（服务端权威）" : "（本地）")),
                rightX, this.contentY + 240, COLOR_HINT);

        int pageCount = Math.max(1, (this.rows.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        graphics.text(this.font, Component.literal("第 " + (this.page + 1) + " / " + pageCount + " 页"),
                listX + 66, this.contentY + PAGE_SIZE * ROW_HEIGHT + 7, COLOR_HINT);

        if (this.rows.isEmpty()) {
            graphics.text(this.font, Component.literal("还没有定义"), listX, this.contentY + 4, COLOR_HINT);
            graphics.text(this.font, Component.literal("填写右侧表单开始造物"), listX, this.contentY + 18, COLOR_HINT);
        }

        if (this.status != null && !this.status.isEmpty()) {
            graphics.text(this.font, LegacyText.parse(this.status),
                    this.panelX + 12, this.panelY + PANEL_H - 18, 0xFFFFFFFF);
        }
        graphics.text(this.font, Component.literal("U 关闭 · 资产目录: config/fantasy_item"),
                this.panelX + 12, this.panelY + PANEL_H - 30, COLOR_HINT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String nextRarity(String current) {
        return switch (current == null ? "COMMON" : current.toUpperCase(Locale.ROOT)) {
            case "COMMON" -> "UNCOMMON";
            case "UNCOMMON" -> "RARE";
            case "RARE" -> "EPIC";
            default -> "COMMON";
        };
    }

    private static BlockTemplate nextTemplate(BlockTemplate current) {
        BlockTemplate[] values = BlockTemplate.values();
        return values[(current.ordinal() + 1) % values.length];
    }
}
