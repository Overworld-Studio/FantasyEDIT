package com.fantasy.item.registry;

import com.fantasy.item.FantasyItem;
import com.fantasy.item.definition.BlockTemplate;
import com.fantasy.item.definition.DefinitionType;
import com.fantasy.item.definition.ItemDefinition;
import com.fantasy.item.text.LegacyText;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运行时动态物品/方块工厂。
 *
 * <p>注册前会调用 {@link RegistryUnfreezer#unfreeze}，保证即使注册表已经 freeze，
 * 也能把新的 namespace:id 写入 BuiltInRegistries。</p>
 */
public final class DynamicRegistry {

    private static final Map<String, ItemDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();

    private DynamicRegistry() {
    }

    /**
     * 幂等注册：已存在的 id 直接复用，仅刷新定义缓存（用于名称/Lore 热更新）。
     */
    public static Item ensureRegistered(ItemDefinition definition) {
        DEFINITIONS.put(definition.key(), definition);
        Item existing = ITEMS.get(definition.key());
        if (existing != null) {
            return existing;
        }
        if (!definition.isValid()) {
            throw new IllegalArgumentException("非法定义: " + definition.key());
        }

        Identifier id = Identifier.fromNamespaceAndPath(definition.namespace, definition.path);
        RegistryUnfreezer.unfreeze(BuiltInRegistries.ITEM);
        RegistryUnfreezer.unfreeze(BuiltInRegistries.BLOCK);

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Item item;
        if (definition.type == DefinitionType.BLOCK) {
            ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
            Block block = createBlock(definition, blockKey);
            Registry.register(BuiltInRegistries.BLOCK, id, block);

            BlockItem blockItem = new BlockItem(block, baseProperties(definition, itemKey).useBlockDescriptionPrefix());
            item = Registry.register(BuiltInRegistries.ITEM, id, blockItem);

            Item.BY_BLOCK.put(block, blockItem);
            registerStates(block);
            BLOCKS.put(definition.key(), block);
        } else {
            item = Registry.register(BuiltInRegistries.ITEM, id, new Item(baseProperties(definition, itemKey)));
        }

        ITEMS.put(definition.key(), item);
        FantasyItem.LOGGER.info("已凭空注册 {} ({})", definition.key(), definition.type);
        return item;
    }

    private static Item.Properties baseProperties(ItemDefinition definition, ResourceKey<Item> key) {
        int stackSize = Math.max(1, Math.min(99, definition.maxStackSize));
        return new Item.Properties()
                .setId(key)
                .stacksTo(stackSize)
                .rarity(parseRarity(definition.rarity));
    }

    private static Block createBlock(ItemDefinition definition, ResourceKey<Block> key) {
        BlockTemplate template = definition.blockProperties == null
                ? BlockTemplate.SOLID
                : definition.blockProperties.template;
        int light = definition.blockProperties == null ? 0 : definition.blockProperties.lightLevel;

        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .setId(key)
                .strength(template == BlockTemplate.PASSABLE ? 0.0F : 1.5F)
                .lightLevel(state -> Math.max(0, Math.min(15, light)));

        switch (template) {
            case GLASS -> properties.sound(SoundType.GLASS).noOcclusion();
            case PASSABLE -> properties.noCollision().noOcclusion();
            case DIRECTIONAL -> properties.noOcclusion().sound(SoundType.STONE);
            case SOLID -> properties.sound(SoundType.STONE);
        }

        return template == BlockTemplate.DIRECTIONAL ? new FantasyBlock(properties) : new Block(properties);
    }

    /**
     * 把方块的所有状态补进 Block.BLOCK_STATE_REGISTRY。
     * 26.x 的 Block 构造器通常已经完成注册，这里做一次幂等补齐，
     * 避免某些环境（自定义 StateDefinition）漏注册导致 F3 调试信息异常。
     */
    private static void registerStates(Block block) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            if (Block.BLOCK_STATE_REGISTRY.getId(state) == -1) {
                Block.BLOCK_STATE_REGISTRY.add(state);
            }
        }
    }

    /**
     * 按“最新定义”构造 ItemStack，因此修改显示名/Lore 后新发放的物品立即生效。
     */
    public static ItemStack createStack(ItemDefinition definition) {
        Item item = ITEMS.get(definition.key());
        if (item == null) {
            item = ensureRegistered(definition);
        }
        ItemStack stack = new ItemStack(item);
        String name = definition.displayName == null || definition.displayName.isBlank()
                ? definition.key()
                : definition.displayName;
        stack.set(DataComponents.CUSTOM_NAME, LegacyText.parse(name));
        if (definition.lore != null && !definition.lore.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(definition.lore.stream().map(LegacyText::parse).toList()));
        }
        if (definition.glint) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);
        }
        return stack;
    }

    public static Item itemOf(ItemDefinition definition) {
        return ITEMS.get(definition.key());
    }

    public static Collection<Item> allItems() {
        return ITEMS.values();
    }

    public static Collection<ItemDefinition> allDefinitions() {
        return DEFINITIONS.values();
    }

    public static ItemStack iconStack() {
        return DEFINITIONS.values().stream()
                .findFirst()
                .map(DynamicRegistry::createStack)
                .orElseGet(() -> new ItemStack(net.minecraft.world.item.Items.PAPER));
    }

    private static Rarity parseRarity(String value) {
        if (value == null) {
            return Rarity.COMMON;
        }
        try {
            return Rarity.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Rarity.COMMON;
        }
    }
}
