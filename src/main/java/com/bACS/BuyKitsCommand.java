package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BuyKitsCommand implements CommandExecutor {
    private final BACS plugin;

    public BuyKitsCommand(BACS plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("❌ 只有玩家可以使用此指令！", NamedTextColor.RED));
            return true;
        }

        openBuyKitsMenu(player);
        return true;
    }

    private void openBuyKitsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
                Component.text("🛒 购买套件", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));

        // 优香套件
        ItemStack yuukaStar = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = yuukaStar.getItemMeta();
        meta.displayName(Component.text("优香（Yuuka）", NamedTextColor.BLUE).decoration(TextDecoration.BOLD, true));
        meta.lore(List.of(
                Component.text("生命值: 150", NamedTextColor.RED),
                Component.text("护甲: 50", NamedTextColor.AQUA),
                Component.text("主武器: 逻辑与理性", NamedTextColor.GOLD),
                Component.text("被动: 护甲自动恢复", NamedTextColor.GREEN),
                Component.text("EX技能: 200护甲", NamedTextColor.BLUE),
                Component.text(""),
                Component.text("价格: 8000硬币", NamedTextColor.GOLD),
                Component.text("点击购买", NamedTextColor.GREEN)
        ));
        yuukaStar.setItemMeta(meta);
        inv.setItem(13, yuukaStar);

        // 填充玻璃
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.text(" ", NamedTextColor.DARK_GRAY));
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }

        player.openInventory(inv);
    }
}