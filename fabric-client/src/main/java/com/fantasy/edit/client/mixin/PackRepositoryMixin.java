package com.fantasy.edit.client.mixin;

import com.fantasy.edit.client.assets.DynamicPackSource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 把 config/fantasy_edit/resourcepack 注入为必选资源包。
 * 每次资源 reload 都会重新扫描该目录，实现 Blockbench 资产热加载。
 */
@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {

    @Shadow
    @Final
    @Mutable
    private Set<RepositorySource> sources;

    /**
     * 原版的 sources 可能是不可变集合，因此这里整体替换成可写副本，
     * 保证任何版本都不会因为 UnsupportedOperationException 崩溃。
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void fantasyItem$addDynamicSource(CallbackInfo ci) {
        Set<RepositorySource> copy = new LinkedHashSet<>(this.sources);
        copy.add(new DynamicPackSource());
        this.sources = copy;
    }
}
