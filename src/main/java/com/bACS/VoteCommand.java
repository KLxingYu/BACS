package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VoteCommand implements CommandExecutor {
    private final BACS plugin;

    public VoteCommand(BACS plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("❌ 只有玩家可以使用此指令！", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("用法: /vote <agree|disagree>", NamedTextColor.YELLOW));
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("agree")) {
            plugin.getVoteManager().castVote(player, true);
        } else if (sub.equals("disagree")) {
            plugin.getVoteManager().castVote(player, false);
        } else {
            player.sendMessage(Component.text("用法: /vote <agree|disagree>", NamedTextColor.YELLOW));
        }
        return true;
    }
}