package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CoinCommand implements CommandExecutor {
    private final CoinManager coinManager;

    public CoinCommand(CoinManager coinManager) {
        this.coinManager = coinManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // /coin - 查询自己的硬币
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("❌ 只有玩家可以使用此指令！", NamedTextColor.RED));
                return true;
            }
            int coins = coinManager.getCoins(player);
            player.sendMessage(Component.text("💰 你目前的硬币数量: " + coins, NamedTextColor.GOLD));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add":
                // /coin add <玩家> <数量> - 添加硬币
                if (!sender.hasPermission("bacs.admin")) {
                    sender.sendMessage(Component.text("❌ 你没有权限！", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("用法: /coin add <玩家> <数量>", NamedTextColor.YELLOW));
                    return true;
                }
                Player addTarget = Bukkit.getPlayer(args[1]);
                if (addTarget == null) {
                    sender.sendMessage(Component.text("❌ 玩家不在线！", NamedTextColor.RED));
                    return true;
                }
                try {
                    int addAmount = Integer.parseInt(args[2]);
                    coinManager.addCoins(addTarget, addAmount);
                    sender.sendMessage(Component.text("✅ 已添加 " + addAmount + " 硬币给 " + addTarget.getName(), NamedTextColor.GREEN));
                    addTarget.sendMessage(Component.text("💰 你获得了 " + addAmount + " 硬币！", NamedTextColor.GOLD));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("❌ 请输入有效数字！", NamedTextColor.RED));
                }
                return true;

            case "set":
                // /coin set <玩家> <数量> - 设置硬币
                if (!sender.hasPermission("bacs.admin")) {
                    sender.sendMessage(Component.text("❌ 你没有权限！", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("用法: /coin set <玩家> <数量>", NamedTextColor.YELLOW));
                    return true;
                }
                Player setTarget = Bukkit.getPlayer(args[1]);
                if (setTarget == null) {
                    sender.sendMessage(Component.text("❌ 玩家不在线！", NamedTextColor.RED));
                    return true;
                }
                try {
                    int setAmount = Integer.parseInt(args[2]);
                    coinManager.setCoins(setTarget, setAmount);
                    sender.sendMessage(Component.text("✅ 已设置 " + setTarget.getName() + " 的硬币为 " + setAmount, NamedTextColor.GREEN));
                    setTarget.sendMessage(Component.text("💰 你的硬币已被设置为 " + setAmount, NamedTextColor.GOLD));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("❌ 请输入有效数字！", NamedTextColor.RED));
                }
                return true;

            case "reset":
                // /coin reset <玩家> - 重置硬币
                if (!sender.hasPermission("bacs.admin")) {
                    sender.sendMessage(Component.text("❌ 你没有权限！", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("用法: /coin reset <玩家>", NamedTextColor.YELLOW));
                    return true;
                }
                Player resetTarget = Bukkit.getPlayer(args[1]);
                if (resetTarget == null) {
                    sender.sendMessage(Component.text("❌ 玩家不在线！", NamedTextColor.RED));
                    return true;
                }
                coinManager.setCoins(resetTarget, 0);
                sender.sendMessage(Component.text("✅ 已重置 " + resetTarget.getName() + " 的硬币为 0", NamedTextColor.GREEN));
                resetTarget.sendMessage(Component.text("💰 你的硬币已被重置", NamedTextColor.GOLD));
                return true;

            case "lookup":
                // /coin lookup <玩家> - 查询其他玩家硬币
                if (!sender.hasPermission("bacs.admin")) {
                    sender.sendMessage(Component.text("❌ 你没有权限！", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("用法: /coin lookup <玩家>", NamedTextColor.YELLOW));
                    return true;
                }
                Player lookupTarget = Bukkit.getPlayer(args[1]);
                if (lookupTarget == null) {
                    sender.sendMessage(Component.text("❌ 玩家不在线！", NamedTextColor.RED));
                    return true;
                }
                int lookupCoins = coinManager.getCoins(lookupTarget);
                sender.sendMessage(Component.text("💰 " + lookupTarget.getName() + " 的硬币数量: " + lookupCoins, NamedTextColor.GOLD));
                return true;

            default:
                sender.sendMessage(Component.text("§e用法:", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("§e/coin §7- 查询自己的硬币", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("§e/coin add <玩家> <数量> §7- 添加硬币", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("§e/coin set <玩家> <数量> §7- 设置硬币", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("§e/coin reset <玩家> §7- 重置硬币", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("§e/coin lookup <玩家> §7- 查询其他玩家硬币", NamedTextColor.YELLOW));
                return true;
        }
    }
}