package com.fantasy.item.paper;

import com.fantasy.item.paper.behavior.BehaviorPool;
import com.fantasy.item.paper.behavior.BehaviorRouter;
import com.fantasy.item.paper.behavior.PaperActionExecutor;
import com.fantasy.item.paper.build.BuildChannelListener;
import com.fantasy.item.paper.build.BuildHistoryService;
import com.fantasy.item.paper.build.BuildLimits;
import com.fantasy.item.paper.build.WorldEditService;
import com.fantasy.item.paper.command.FantasyItemCommand;
import com.fantasy.item.paper.item.ItemFactory;
import com.fantasy.item.paper.model.DefinitionStore;
import com.fantasy.item.paper.model.ItemDefinition;
import com.fantasy.item.paper.net.BehaviorChannelListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

/**
 * Fantasy:Item 服务端主类。
 *
 * 职责：
 * 1. 从 definitions/ 读取与客户端一致的 JSON；
 * 2. 提供 /fantasyitem reload|list|give；
 * 3. 用 ItemMeta / Data Components 发放不影响原版的虚拟物品；
 * 4. 通过动态事件路由与插件消息通道承载运行时行为热注入；
 * 5. 世界快速搭建：校验并执行客户端建造请求，维护可撤销历史。
 */
public final class FantasyItemPlugin extends JavaPlugin {

    private final BehaviorPool behaviorPool = new BehaviorPool();

    private DefinitionStore definitionStore;
    private ItemFactory itemFactory;
    private BehaviorRouter behaviorRouter;
    private BehaviorChannelListener channelListener;

    private BuildLimits buildLimits;
    private BuildHistoryService buildHistory;
    private WorldEditService worldEdit;
    private BuildChannelListener buildChannelListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadAll();

        PluginCommand command = getCommand("fantasyitem");
        if (command == null) {
            getLogger().severe("plugin.yml 中缺少 fantasyitem 指令，插件无法工作");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        FantasyItemCommand executor = new FantasyItemCommand(this, definitionStore, itemFactory);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        startBehaviorEngine();
        startBuildEngine();

        getLogger().info("Fantasy:Item 已启用，共加载 " + definitionStore.size() + " 条虚空造物定义，"
                + behaviorPool.size() + " 条行为树");
    }

    @Override
    public void onDisable() {
        if (buildChannelListener != null) {
            getServer().getMessenger().unregisterIncomingPluginChannel(this, BuildChannelListener.INBOUND, buildChannelListener);
            buildChannelListener = null;
        }
        if (behaviorRouter != null) {
            behaviorRouter.stopTickTask();
            behaviorRouter = null;
        }
        if (channelListener != null) {
            getServer().getMessenger().unregisterIncomingPluginChannel(this, BehaviorChannelListener.INBOUND, channelListener);
            getServer().getMessenger().unregisterOutgoingPluginChannel(this);
            channelListener = null;
        }
        behaviorPool.clear();
    }

    private void startBehaviorEngine() {
        PaperActionExecutor actionExecutor =
                new PaperActionExecutor(getConfig().getBoolean("behaviors.allow-console-commands", false));
        behaviorRouter = new BehaviorRouter(this, behaviorPool, actionExecutor, itemFactory.definitionKey());
        getServer().getPluginManager().registerEvents(behaviorRouter, this);
        behaviorRouter.startTickTask(getConfig().getInt("behaviors.tick-interval", 5));

        BehaviorChannelListener listener = new BehaviorChannelListener(this);
        this.channelListener = listener;
        getServer().getMessenger().registerIncomingPluginChannel(this, BehaviorChannelListener.INBOUND, listener);
        getServer().getMessenger().registerOutgoingPluginChannel(this, BehaviorChannelListener.OUTBOUND);
    }

    private void startBuildEngine() {
        this.buildLimits = new BuildLimits(getConfig());
        this.buildHistory = new BuildHistoryService(this.buildLimits.historyDepth);
        this.worldEdit = new WorldEditService(this.buildHistory, this.buildLimits);

        BuildChannelListener listener = new BuildChannelListener(this, this.buildLimits, this.worldEdit);
        this.buildChannelListener = listener;
        getServer().getMessenger().registerIncomingPluginChannel(this, BuildChannelListener.INBOUND, listener);
        getServer().getMessenger().registerOutgoingPluginChannel(this, BuildChannelListener.OUTBOUND);
        getLogger().info("快速搭建已就绪: " + this.buildLimits.describe());
    }

    /**
     * 重新读取配置与所有 JSON 定义。供 /fantasyitem reload 调用。
     */
    public void reloadAll() {
        reloadConfig();
        Path directory = getDataFolder().toPath()
                .resolve(getConfig().getString("definitions-directory", "definitions"));
        definitionStore = new DefinitionStore(directory, getLogger());
        definitionStore.load();
        itemFactory = new ItemFactory(this, getConfig().getString("default-base-item", "PAPER"));

        behaviorPool.clear();
        for (ItemDefinition definition : definitionStore.all()) {
            if (definition.behaviors != null && !definition.behaviors.isEmpty()) {
                behaviorPool.replace(definition.key(), definition.behaviors);
            }
        }
        getLogger().info("定义目录: " + directory.toAbsolutePath());
    }

    public DefinitionStore definitions() {
        return definitionStore;
    }

    public ItemFactory items() {
        return itemFactory;
    }

    public BehaviorPool behaviors() {
        return behaviorPool;
    }

    public BuildLimits buildLimits() {
        return buildLimits;
    }

    public WorldEditService worldEdit() {
        return worldEdit;
    }
}
