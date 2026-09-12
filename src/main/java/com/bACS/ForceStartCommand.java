package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ForceStartCommand implements CommandExecutor {
    private final BACS plugin;
    public ForceStartCommand(BACS plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bacs.admin")) {
            sender.sendMessage(Component.text("❌ 你没有权限！", NamedTextColor.RED));
            return true;
        }
        GameManager manager = plugin.getGameManager();
        if (manager.getState() == GameState.ROUND_ACTIVE || manager.getState() == GameState.ROUND_END) {
            sender.sendMessage(Component.text("⚠ 游戏已开始！", NamedTextColor.YELLOW));
            return true;
        }

        // 使用新的强开方法
        manager.forceStartGame();
        sender.sendMessage(Component.text("✅ 已强制开始游戏！", NamedTextColor.GREEN));
        return true;
    }
}