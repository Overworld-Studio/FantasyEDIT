package com.fantasy.item.client.assets;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackMetadataResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.world.flag.FeatureFlagSet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 把一个真实磁盘目录挂载为“必选”客户端资源包。
 *
 * <p>与内置资源包不同，这里每次资源重载都会重新读取
 * {@code config/fantasy_item/resourcepack}，所以 GUI 保存后立刻 reload 就能看到新模型。</p>
 */
public final class DynamicPackSource implements net.minecraft.server.packs.repository.RepositorySource {

    public static final String PACK_ID = "fantasy_item_dynamic";

    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        Path root = AssetWorkspace.packRoot();
        if (!Files.isDirectory(root.resolve("assets"))) {
            return;
        }

        PackLocationInfo location = new PackLocationInfo(
                PACK_ID,
                Component.literal("Fantasy:Item 动态资产"),
                PackSource.BUILT_IN,
                Optional.empty());

        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override
            public PackMetadataResources openMetadata(PackLocationInfo info) {
                return new PathPackResources(info, root);
            }

            @Override
            public Stream<PackResources> openResources(PackLocationInfo info, Pack.Metadata metadata) {
                return Stream.of(new PathPackResources(info, root));
            }
        };

        Pack.Metadata metadata = new Pack.Metadata(
                Component.literal("运行时生成的虚空造物模型与纹理"),
                PackCompatibility.COMPATIBLE,
                FeatureFlagSet.of(),
                List.of());

        consumer.accept(new Pack(location, supplier, metadata,
                new PackSelectionConfig(true, Pack.Position.TOP, true)));
    }
}
