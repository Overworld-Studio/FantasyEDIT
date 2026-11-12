package com.fantasy.edit.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 一条“虚空造物”定义，与 Paper 端共用同一份 JSON 结构。
 *
 * <pre>
 * {
 *   "namespace": "hyperlab",
 *   "path": "quantum_core",
 *   "type": "ITEM",
 *   "display_name": "&amp;b量子核心",
 *   "lore": ["&amp;7虚空造物", "&amp;8——第一枚"],
 *   "block_properties": { "template": "GLASS", "light_level": 7 },
 *   "model": "quantum_core.json",
 *   "texture": "quantum_core.png",
 *   "glint": true,
 *   "rarity": "RARE",
 *   "max_stack_size": 64,
 *   "base_item": "PAPER"
 * }
 * </pre>
 */
public final class ItemDefinition {

    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_.-]{1,64}");

    public String namespace = "fantasy_edit";
    public String path = "";
    public DefinitionType type = DefinitionType.ITEM;
    public String displayName = "";
    public List<String> lore = new ArrayList<>();

    public BlockSettings blockProperties = new BlockSettings();
    public String model = "";
    public String texture = "";

    public boolean glint = false;
    public String rarity = "COMMON";
    public int maxStackSize = 64;

    /** 仅服务端使用：承载外观的原版物品。客户端会生成独立物品，因此该字段在客户端被忽略。 */
    public String baseItem = "PAPER";

    /** 运行时行为编排：触发器 → 动作，保存后立即可被事件总线读取。 */
    public List<com.fantasy.edit.behavior.BehaviorRule> behaviors = new ArrayList<>();

    public String key() {
        return namespace + ":" + path;
    }

    public String fileName() {
        return namespace + "__" + path;
    }

    public boolean isValid() {
        return isValidId(namespace) && isValidId(path)
                && !"minecraft".equals(namespace)
                && type != null;
    }

    public static boolean isValidId(String value) {
        return value != null && ID_PATTERN.matcher(value).matches();
    }

    public ItemDefinition copy() {
        ItemDefinition copy = new ItemDefinition();
        copy.namespace = namespace;
        copy.path = path;
        copy.type = type;
        copy.displayName = displayName;
        copy.lore = new ArrayList<>(lore);
        copy.blockProperties = blockProperties == null ? new BlockSettings() : blockProperties.copy();
        copy.model = model;
        copy.texture = texture;
        copy.glint = glint;
        copy.rarity = rarity;
        copy.maxStackSize = maxStackSize;
        copy.baseItem = baseItem;
        copy.behaviors = new ArrayList<>();
        if (behaviors != null) {
            for (com.fantasy.edit.behavior.BehaviorRule rule : behaviors) {
                if (rule != null) {
                    copy.behaviors.add(rule.copy());
                }
            }
        }
        return copy;
    }
}
