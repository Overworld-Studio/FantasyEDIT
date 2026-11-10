package com.fantasy.item.paper.item;

import com.fantasy.item.paper.model.ItemDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * 把 JSON 定义转换成服务端可发放的原版 ItemStack。
 *
 * <p>服务端无法真正注册新物品，因此使用 base_item 作为“载体”，
 * 并通过 PersistentDataContainer 写入 fantasy_item 标识，
 * 客户端模组 / 资源包可据此识别并渲染为虚空造物。</p>
 */
public final class ItemFactory {

    private final NamespacedKey definitionKey;
    private final String defaultBaseItem;

    public ItemFactory(Plugin plugin, String defaultBaseItem) {
        this.definitionKey = new NamespacedKey(plugin, "definition");
        this.defaultBaseItem = defaultBaseItem == null ? "PAPER" : defaultBaseItem;
    }

    public NamespacedKey definitionKey() {
        return definitionKey;
    }

    public ItemStack create(ItemDefinition definition, int amount) {
        Material material = resolveMaterial(definition.baseItem);
        int stackSize = Math.max(1, Math.min(Math.min(definition.maxStackSize, material.getMaxStackSize()), amount));

        ItemStack stack = new ItemStack(material, stackSize);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        String name = definition.displayName == null || definition.displayName.isBlank()
                ? definition.key()
                : definition.displayName;
        meta.itemName(legacy(name));

        if (definition.lore != null && !definition.lore.isEmpty()) {
            meta.lore(definition.lore.stream().map(this::legacy).toList());
        }
        if (definition.glint) {
            meta.setEnchantmentGlintOverride(Boolean.TRUE);
        }
        meta.getPersistentDataContainer().set(definitionKey, PersistentDataType.STRING, definition.key());

        // 让服务端物品在安装配套资源包时也能命中自定义模型
        if (definition.model != null && !definition.model.isBlank()) {
            int modelData = Math.abs(definition.key().hashCode()) % 100_000 + 1;
            meta.setCustomModelData(modelData);
        }

        stack.setItemMeta(meta);
        return stack;
    }

    private Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text == null ? "" : text);
    }

    private Material resolveMaterial(String baseItem) {
        Material material = baseItem == null ? null : Material.matchMaterial(baseItem);
        if (material == null || !material.isItem()) {
            material = Material.matchMaterial(defaultBaseItem);
        }
        if (material == null || !material.isItem()) {
            material = Material.PAPER;
        }
        return material;
    }

    public List<String> describe(ItemDefinition definition) {
        return List.of(definition.key(), definition.type, definition.baseItem == null ? defaultBaseItem : definition.baseItem);
    }
}
