package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ResetCommand implements CommandExecutor {
    private final BACS plugin;
    public ResetCommand(BACS plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bacs.admin")) {
            sender.sendMessage(Component.text("❌ 你没有权限！", NamedTextColor.RED));
            return true;
        }
        plugin.getGameManager().resetGame();
        sender.sendMessage(Component.text("✅ 游戏已重置！", NamedTextColor.GREEN));
        return true;
    }
}