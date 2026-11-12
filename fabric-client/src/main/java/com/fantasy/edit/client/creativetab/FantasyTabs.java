package com.fantasy.edit.client.creativetab;

import com.fantasy.edit.FantasyEdit;
import com.fantasy.edit.registry.DynamicRegistry;
import com.fantasy.edit.registry.RegistryUnfreezer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

/**
 * 专属创造标签页：实时展示所有自创物品。
 *
 * <p>每次重新打开创造模式物品栏，标签内容都会按当前注册表重新生成，
 * 因此新造的物品无需重启即可出现。</p>
 */
public final class FantasyTabs {

    public static final ResourceKey<CreativeModeTab> STUDIO = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(FantasyEdit.MOD_ID, "studio"));

    private FantasyTabs() {
    }

    public static void register() {
        RegistryUnfreezer.unfreeze(BuiltInRegistries.CREATIVE_MODE_TAB);

        CreativeModeTab tab = FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.fantasy_edit.studio"))
                .icon(DynamicRegistry::iconStack)
                .displayItems((parameters, output) -> DynamicRegistry.allDefinitions()
                        .forEach(definition -> output.accept(DynamicRegistry.createStack(definition))))
                .build();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, STUDIO, tab);
    }
}
