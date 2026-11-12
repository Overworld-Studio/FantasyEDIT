package com.fantasy.edit.client.assets;

import com.fantasy.edit.FantasyEdit;
import net.minecraft.client.Minecraft;

/**
 * 主动触发客户端资源重载（等价于自动执行一次 F3+T），实现外观热更新。
 */
public final class AssetReloader {

    private AssetReloader() {
    }

    public static void reload() {
        Minecraft minecraft = Minecraft.getInstance();
        AssetWorkspace.ensureLayout();
        minecraft.execute(() -> minecraft.reloadResourcePacks()
                .whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        FantasyEdit.LOGGER.error("Fantasy:EDIT 资源热重载失败", throwable);
                    }
                }));
    }
}
