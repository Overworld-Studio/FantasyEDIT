package com.fantasy.edit;

import com.fantasy.edit.definition.DefinitionStore;
import com.fantasy.edit.definition.ItemDefinition;
import com.fantasy.edit.registry.DynamicRegistry;
import com.fantasy.edit.registry.RegistryUnfreezer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Fantasy:EDIT 公共入口。
 *
 * 启动流程：
 * 1. 解冻 ITEM / BLOCK 注册表；
 * 2. 读取 config/fantasy_edit/definitions/*.json；
 * 3. 把磁盘上已有的定义重新注册进运行时注册表。
 */
public final class FantasyEdit implements ModInitializer {

    public static final String MOD_ID = "fantasy_edit";
    public static final Logger LOGGER = LoggerFactory.getLogger("Fantasy:EDIT");

    private static DefinitionStore store;

    @Override
    public void onInitialize() {
        Path root = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        store = new DefinitionStore(root.resolve("definitions"));
        store.load();

        RegistryUnfreezer.unfreeze(BuiltInRegistries.ITEM);
        RegistryUnfreezer.unfreeze(BuiltInRegistries.BLOCK);

        for (ItemDefinition definition : store.all()) {
            try {
                DynamicRegistry.ensureRegistered(definition);
            } catch (RuntimeException e) {
                LOGGER.error("注册虚空造物失败: {}", definition.key(), e);
            }
        }
    }

    public static DefinitionStore store() {
        return store;
    }
}
