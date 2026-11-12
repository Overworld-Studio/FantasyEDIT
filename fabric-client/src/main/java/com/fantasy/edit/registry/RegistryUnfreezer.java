package com.fantasy.edit.registry;

import com.fantasy.edit.FantasyEdit;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;

import java.lang.reflect.Field;

/**
 * 注册表解冻工具。
 *
 * <p>Mixin（{@code MappedRegistryMixin}）会在每次 freeze 后强制把 frozen 置回 false；
 * 这里的反射实现作为兜底，保证在 Mixin 未生效（例如其它加载器）时也能在运行时写入注册表。</p>
 */
public final class RegistryUnfreezer {

    private RegistryUnfreezer() {
    }

    public static void unfreeze(Registry<?> registry) {
        if (!(registry instanceof MappedRegistry<?> mapped)) {
            return;
        }
        try {
            Field frozen = MappedRegistry.class.getDeclaredField("frozen");
            frozen.setAccessible(true);
            frozen.setBoolean(mapped, false);
        } catch (ReflectiveOperationException e) {
            FantasyEdit.LOGGER.warn("无法解冻注册表 {}", registry.key().identifier(), e);
        }
    }
}
