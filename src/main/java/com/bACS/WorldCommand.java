package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WorldCommand implements CommandExecutor {
    private final BACS plugin;
    public WorldCommand(BACS plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("只能由玩家执行！");
            return true;
        }
        World voidWorld = plugin.getVoidWorld();
        if (voidWorld == null) {
            player.sendMessage(Component.text("❌ 虚空世界不存在！", NamedTextColor.RED));
            return true;
        }
        player.teleport(new Location(voidWorld, 0, 101, 0));
        player.sendMessage(Component.text("✅ 已传送到虚空世界！", NamedTextColor.GREEN));
        Location loc = player.getLocation();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                Location blockLoc = new Location(voidWorld, loc.getBlockX() + x, loc.getBlockY() - 1, loc.getBlockZ() + z);
                if (blockLoc.getBlock().isEmpty()) blockLoc.getBlock().setType(Material.GLASS);
            }
        }
        return true;
    }
}