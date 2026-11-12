package com.fantasy.edit.mixin;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 强行解除注册表冻结，使 Fantasy:EDIT 可以在运行时注册全新的 namespace:id。
 *
 * <p>Minecraft 26.x 使用官方类名（MappedRegistry 未被混淆），因此可以直接以
 * {@code net.minecraft.core.MappedRegistry} 作为注入目标。</p>
 */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin<T> {

    @Shadow
    private boolean frozen;

    /**
     * freeze() 返回后立刻把 frozen 标记改回 false：
     * 注册表仍然能正常提供 id 映射与 holder，但允许 continue 写入。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "freeze", at = @At("RETURN"))
    private void fantasyItem$keepMutable(CallbackInfoReturnable<Registry> cir) {
        this.frozen = false;
    }
}
