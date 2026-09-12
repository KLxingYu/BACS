package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ShopListener implements Listener {

    private final BACS plugin;
    private final GameManager gameManager;

    public ShopListener(BACS plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.GHAST_TEAR) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }

        // 直接比较 Component（修复：不需要再调用 Component.text()）
        if (!meta.displayName().equals(GameManager.SHOP_ITEM_NAME)) {
            return;
        }

        event.setCancelled(true);

        if (gameManager.getState() != GameState.PREPARE) {
            player.sendMessage(Component.text("❌ 只能在准备阶段打开商店！", NamedTextColor.RED));
            return;
        }

        if (!gameManager.isPlayerInShopArea(player)) {
            player.sendMessage(Component.text("❌ 你不在商店区域内！", NamedTextColor.RED));
            String team = gameManager.getPlayerTeam(player);
            if (team != null) {
                if (team.equals(GameManager.TEAM_ATTACKER)) {
                    player.sendMessage(Component.text("请前往攻方商店区域 (228~236, 81~84, 30~36)", NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text("请前往守方商店区域 (159~166, 74~76, 9~16)", NamedTextColor.YELLOW));
                }
            }
            return;
        }

        openShopMenu(player);
    }

    private void openShopMenu(Player player) {
        Inventory inv = plugin.getServer().createInventory(null, 54, Component.text("🏪 商店", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));

        // 铁剑
        ItemStack ironSword = new ItemStack(Material.IRON_SWORD);
        ItemMeta ironSwordMeta = ironSword.getItemMeta();
        ironSwordMeta.displayName(Component.text("⚔ 铁剑", NamedTextColor.WHITE));
        ironSwordMeta.lore(List.of(
                Component.text("价格: 10 金币", NamedTextColor.GOLD),
                Component.text("点击购买", NamedTextColor.GRAY)
        ));
        ironSword.setItemMeta(ironSwordMeta);
        inv.setItem(0, ironSword);

        // 钻石剑
        ItemStack diamondSword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta diamondSwordMeta = diamondSword.getItemMeta();
        diamondSwordMeta.displayName(Component.text("⚔ 钻石剑", NamedTextColor.AQUA));
        diamondSwordMeta.lore(List.of(
                Component.text("价格: 30 金币", NamedTextColor.GOLD),
                Component.text("点击购买", NamedTextColor.GRAY)
        ));
        diamondSword.setItemMeta(diamondSwordMeta);
        inv.setItem(1, diamondSword);

        // 铁头盔
        ItemStack ironHelmet = new ItemStack(Material.IRON_HELMET);
        ItemMeta ironHelmetMeta = ironHelmet.getItemMeta();
        ironHelmetMeta.displayName(Component.text("⛑ 铁头盔", NamedTextColor.WHITE));
        ironHelmetMeta.lore(List.of(
                Component.text("价格: 8 金币", NamedTextColor.GOLD),
                Component.text("点击购买", NamedTextColor.GRAY)
        ));
        ironHelmet.setItemMeta(ironHelmetMeta);
        inv.setItem(9, ironHelmet);

        // 铁胸甲
        ItemStack ironChestplate = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta ironChestplateMeta = ironChestplate.getItemMeta();
        ironChestplateMeta.displayName(Component.text("⛑ 铁胸甲", NamedTextColor.WHITE));
        ironChestplateMeta.lore(List.of(
                Component.text("价格: 12 金币", NamedTextColor.GOLD),
                Component.text("点击购买", NamedTextColor.GRAY)
        ));
        ironChestplate.setItemMeta(ironChestplateMeta);
        inv.setItem(10, ironChestplate);

        // 铁护腿
        ItemStack ironLeggings = new ItemStack(Material.IRON_LEGGINGS);
        ItemMeta ironLeggingsMeta = ironLeggings.getItemMeta();
        ironLeggingsMeta.displayName(Component.text("⛑ 铁护腿", NamedTextColor.WHITE));
        ironLeggingsMeta.lore(List.of(
                Component.text("价格: 10 金币", NamedTextColor.GOLD),
                Component.text("点击购买", NamedTextColor.GRAY)
        ));
        ironLeggings.setItemMeta(ironLeggingsMeta);
        inv.setItem(11, ironLeggings);

        // 铁靴子
        ItemStack ironBoots = new ItemStack(Material.IRON_BOOTS);
        ItemMeta ironBootsMeta = ironBoots.getItemMeta();
        ironBootsMeta.displayName(Component.text("⛑ 铁靴子", NamedTextColor.WHITE));
        ironBootsMeta.lore(List.of(
                Component.text("价格: 6 金币", NamedTextColor.GOLD),
                Component.text("点击购买", NamedTextColor.GRAY)
        ));
        ironBoots.setItemMeta(ironBootsMeta);
        inv.setItem(12, ironBoots);

        // 弓
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.displayName(Component.text("🏹 弓", NamedTextColor.WHITE));
        bowMeta.lore(List.of(
                Component.text("价格: 15 金币", NamedTextColor.GOLD),
                Component.text("点击购买", NamedTextColor.GRAY)
        ));
        bow.setItemMeta(bowMeta);
        inv.setItem(18, bow);

        // 箭
        ItemStack arrows = new ItemStack(Material.ARROW, 16);
        ItemMeta arrowsMeta = arrows.getItemMeta();
        arrowsMeta.displayName(Component.text("➹ 箭 x16", NamedTextColor.WHITE));
        arrowsMeta.lore(List.of(
                Component.text("价格: 5 金币", NamedTextColor.GOLD),
                Component.text("点击购买", NamedTextColor.GRAY)
        ));
        arrows.setItemMeta(arrowsMeta);
        inv.setItem(19, arrows);

        // 面包
        ItemStack bread = new ItemStack(Material.BREAD, 8);
        ItemMeta breadMeta = bread.getItemMeta();
        breadMeta.displayName(Component.text("🍞 面包 x8", NamedTextColor.WHITE));
        breadMeta.lore(List.of(
                Component.text("价格: 3 金币", NamedTextColor.GOLD),
                Component.text("点击购买", NamedTextColor.GRAY)
        ));
        bread.setItemMeta(breadMeta);
        inv.setItem(27, bread);

        // 金苹果
        ItemStack goldenApple = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta goldenAppleMeta = goldenApple.getItemMeta();
        goldenAppleMeta.displayName(Component.text("🍎 金苹果", NamedTextColor.GOLD));
        goldenAppleMeta.lore(List.of(
                Component.text("价格: 20 金币", NamedTextColor.GOLD),
                Component.text("点击购买", NamedTextColor.GRAY)
        ));
        goldenApple.setItemMeta(goldenAppleMeta);
        inv.setItem(28, goldenApple);

        // 治疗药水
        ItemStack healthPotion = new ItemStack(Material.POTION);
        ItemMeta healthPotionMeta = healthPotion.getItemMeta();
        healthPotionMeta.displayName(Component.text("💚 治疗药水", NamedTextColor.RED));
        healthPotionMeta.lore(List.of(
                Component.text("价格: 8 金币", NamedTextColor.GOLD),
                Component.text("点击购买", NamedTextColor.GRAY)
        ));
        healthPotion.setItemMeta(healthPotionMeta);
        inv.setItem(36, healthPotion);

        // 速度药水
        ItemStack speedPotion = new ItemStack(Material.POTION);
        ItemMeta speedPotionMeta = speedPotion.getItemMeta();
        speedPotionMeta.displayName(Component.text("💨 速度药水", NamedTextColor.AQUA));
        speedPotionMeta.lore(List.of(
                Component.text("价格: 8 金币", NamedTextColor.GOLD),
                Component.text("点击购买", NamedTextColor.GRAY)
        ));
        speedPotion.setItemMeta(speedPotionMeta);
        inv.setItem(37, speedPotion);

        // 玻璃填充
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.text(" ", NamedTextColor.DARK_GRAY));
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }

        player.openInventory(inv);
    }
}