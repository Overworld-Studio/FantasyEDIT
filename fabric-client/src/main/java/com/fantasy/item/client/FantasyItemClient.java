package com.fantasy.item.client;

import com.fantasy.item.behavior.DynamicEventRouter;
import com.fantasy.item.client.assets.AssetWorkspace;
import com.fantasy.item.client.creativetab.FantasyTabs;
import com.fantasy.item.network.BehaviorSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * 客户端入口：注册快捷键、专属创造标签页、动态事件总线与行为同步通道。
 */
public final class FantasyItemClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AssetWorkspace.ensureLayout();
        FantasyTabs.register();
        StudioKeybinds.register();
        ClientTickEvents.END_CLIENT_TICK.register(StudioKeybinds::tick);

        // 运行时行为：事件总线与双端同步（保存后零重启生效）
        BehaviorSync.register();
        DynamicEventRouter.register();
    }
}
