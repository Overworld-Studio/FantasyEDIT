package com.fantasy.item.paper.command;

import com.fantasy.item.paper.FantasyItemPlugin;
import com.fantasy.item.paper.item.ItemFactory;
import com.fantasy.item.paper.model.DefinitionStore;
import com.fantasy.item.paper.model.ItemDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /fantasyitem reload | list | give &lt;namespace:path&gt; [player] [amount]
 */
public final class FantasyItemCommand implements CommandExecutor, TabCompleter {

    private final FantasyItemPlugin plugin;
    private final DefinitionStore store;
    private final ItemFactory itemFactory;

    public FantasyItemCommand(FantasyItemPlugin plugin, DefinitionStore store, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.store = store;
        this.itemFactory = itemFactory;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            usage(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadAll();
                sender.sendMessage(Component.text("Fantasy:Item 已重载，共 " + plugin.definitions().size() + " 条定义", NamedTextColor.GREEN));
            }
            case "list" -> list(sender);
            case "behaviors" -> behaviors(sender);
            case "give" -> give(sender, args);
            default -> usage(sender);
        }
        return true;
    }

    private void list(CommandSender sender) {
        if (store.size() == 0) {
            sender.sendMessage(Component.text("当前没有任何定义，请把客户端生成的 JSON 放入 " + store.directory(), NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("虚空造物列表 (" + store.size() + "):", NamedTextColor.AQUA));
        for (ItemDefinition definition : store.all()) {
            sender.sendMessage(Component.text(" - " + definition.key() + " [" + definition.type + "]", NamedTextColor.GRAY));
        }
    }

    private void behaviors(CommandSender sender) {
        var snapshot = plugin.behaviors().snapshot();
        if (snapshot.isEmpty()) {
            sender.sendMessage(Component.text("当前没有任何已注入的行为树", NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("已注入行为树 (" + snapshot.size() + "):", NamedTextColor.AQUA));
        snapshot.forEach((key, rules) ->
                sender.sendMessage(Component.text(" - " + key + " × " + rules.size(), NamedTextColor.GRAY)));
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /" + "fantasyitem give <namespace:path> [player] [amount]", NamedTextColor.RED));
            return;
        }

        ItemDefinition definition = store.get(args[1]);
        if (definition == null) {
            sender.sendMessage(Component.text("找不到定义: " + args[1], NamedTextColor.RED));
            return;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(Component.text("玩家不在线: " + args[2], NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Component.text("控制台发放时必须指定玩家", NamedTextColor.RED));
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("数量必须是数字", NamedTextColor.RED));
                return;
            }
        }

        ItemStack stack = itemFactory.create(definition, amount);
        target.getInventory().addItem(stack);
        sender.sendMessage(Component.text("已给予 " + target.getName() + " " + definition.key() + " x" + amount, NamedTextColor.GREEN));
        if (sender != target) {
            target.sendMessage(Component.text("你收到了虚空造物: " + definition.key(), NamedTextColor.AQUA));
        }
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(Component.text("Fantasy:Item 指令:", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/fantasyitem reload — 重载 JSON 定义", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/fantasyitem list — 列出全部定义", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/fantasyitem behaviors — 查看已注入的行为树", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/fantasyitem give <namespace:path> [player] [amount]", NamedTextColor.GRAY));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("reload", "list", "behaviors", "give"), args[0]);
        }
        if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            List<String> keys = new ArrayList<>();
            store.all().forEach(definition -> keys.add(definition.key()));
            return filter(keys, args[1]);
        }
        if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> names.add(player.getName()));
            return filter(names, args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> source, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return source.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
