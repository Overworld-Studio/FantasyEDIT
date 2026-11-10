package com.fantasy.item.paper.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Paper 端的定义模型，字段与 Fabric 客户端完全一致。
 * 服务端不需要注册真实物品，因此额外提供 base_item 作为原版载体。
 */
public final class ItemDefinition {

    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_.-]{1,64}");

    public String namespace = "fantasy_item";
    public String path = "";
    public String type = "ITEM";
    public String displayName = "";
    public List<String> lore = new ArrayList<>();
    public BlockProperties blockProperties = new BlockProperties();
    public String model = "";
    public String texture = "";
    public boolean glint = false;
    public String rarity = "COMMON";
    public int maxStackSize = 64;
    public String baseItem = "PAPER";

    /** 运行时行为树：与客户端 GUI 保存的结构完全一致。 */
    public List<com.fantasy.item.paper.behavior.BehaviorRule> behaviors = new ArrayList<>();

    public String key() {
        return namespace + ":" + path;
    }

    public boolean isValid() {
        return ID_PATTERN.matcher(namespace == null ? "" : namespace).matches()
                && ID_PATTERN.matcher(path == null ? "" : path).matches()
                && !"minecraft".equals(namespace);
    }

    public boolean isBlock() {
        return "BLOCK".equalsIgnoreCase(type);
    }

    public static final class BlockProperties {
        public String template = "SOLID";
        public int lightLevel = 0;
    }
}
