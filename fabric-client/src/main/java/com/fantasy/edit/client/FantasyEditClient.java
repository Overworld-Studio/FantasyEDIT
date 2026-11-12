package com.fantasy.edit.client;

import com.fantasy.edit.behavior.DynamicEventRouter;
import com.fantasy.edit.builder.BuildHud;
import com.fantasy.edit.builder.BuildKeybinds;
import com.fantasy.edit.builder.ClientBuildEngine;
import com.fantasy.edit.client.assets.AssetWorkspace;
import com.fantasy.edit.client.creativetab.FantasyTabs;
import com.fantasy.edit.network.BehaviorSync;
import com.fantasy.edit.network.BuildSync;
import com.fantasy.edit.rule.RuleEngine;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

/**
 * 客户端入口：注册快捷键、专属创造标签页、动态事件总线、行为同步通道与快速搭建工具链。
 */
public final class FantasyEditClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AssetWorkspace.ensureLayout();
        // 运行时规则：把硬编码常量与原版 GameRule 统一搬进可热改注册表
        RuleEngine.bootstrap();
        ClientTickEvents.END_CLIENT_TICK.register(RuleEngine::tick);
        FantasyTabs.register();
        StudioKeybinds.register();
        ClientTickEvents.END_CLIENT_TICK.register(StudioKeybinds::tick);

        // 运行时行为：事件总线与双端同步（保存后零重启生效）
        BehaviorSync.register();
        DynamicEventRouter.register();

        // 世界快速搭建（仅创造模式）
        BuildKeybinds.register();
        ClientBuildEngine.registerGuards();
        BuildSync.register();
        ClientTickEvents.END_CLIENT_TICK.register(BuildKeybinds::tick);
        ClientTickEvents.END_CLIENT_TICK.register(ClientBuildEngine::tick);
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath("fantasy_edit", "build_hud"), new BuildHud());
    }
}
