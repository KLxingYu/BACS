package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class BombListener implements Listener {
    private final BACS plugin;
    private final GameManager gameManager;
    private final BombManager bombManager;

    public BombListener(BACS plugin, GameManager gameManager, BombManager bombManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.bombManager = bombManager;
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        if (item.getType() == Material.GHAST_TEAR &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().equals(GameManager.SHOP_ITEM_NAME)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ 此物品不可丢弃！", NamedTextColor.RED));
            return;
        }

        if (bombManager.isRestrictedItem(item)) {
            String name = item.getItemMeta().displayName().toString();

            if (name.contains("炸药") && item.getType() == Material.QUARTZ) {
                bombManager.onBombDropped(player, item);
                return;
            }

            event.setCancelled(true);
            player.sendMessage(Component.text("❌ 此物品不可丢弃！", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onItemPickup(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();

        if (item.getType() == Material.QUARTZ &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains("炸药")) {

            if (gameManager.getPlayerTeam(player) != null &&
                    gameManager.getPlayerTeam(player).equals(GameManager.TEAM_DEFENDER)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ 守方不能拾取炸药！", NamedTextColor.RED));
            }
        }

        if (item.getType() == Material.DAYLIGHT_DETECTOR &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains("炸弹部署器")) {

            if (gameManager.getPlayerTeam(player) != null &&
                    gameManager.getPlayerTeam(player).equals(GameManager.TEAM_DEFENDER)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ 守方不能拾取炸弹部署器！", NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        if (item.getType() == Material.GHAST_TEAR &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().equals(GameManager.SHOP_ITEM_NAME)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ 此物品不可移动！", NamedTextColor.RED));
            return;
        }

        if (bombManager.isRestrictedItem(item)) {
            String name = item.getItemMeta().displayName().toString();

            if (name.contains("炸药") && item.getType() == Material.QUARTZ) {
                return;
            }

            event.setCancelled(true);
            player.sendMessage(Component.text("❌ 此物品不可移动！", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        // 炸药右键 -> 获得阳光传感器
        if (item.getType() == Material.QUARTZ &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains("炸药")) {

            if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                bombManager.onBombDeploy(player);
                event.setCancelled(true);
            }
        }

        // 阳光传感器不可使用
        if (item.getType() == Material.DAYLIGHT_DETECTOR &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains("炸弹部署器")) {

            event.setCancelled(true);

            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                player.sendMessage(Component.text("💡 蹲下4秒以部署炸弹！", NamedTextColor.GOLD));
            }
        }

        // 拆弹
        if (item.getType() == Material.SHEARS &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains("拆弹器")) {

            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (event.getClickedBlock() != null &&
                        event.getClickedBlock().getType() == Material.DAYLIGHT_DETECTOR) {
                    bombManager.onDefuseStart(player);
                    event.setCancelled(true);
                }
            }
        }
    }

    // ===== 蹲下事件：触发部署 =====
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(3);

        if (item == null) return;
        if (item.getType() != Material.DAYLIGHT_DETECTOR) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().displayName().toString().contains("炸弹部署器")) return;

        Location below = player.getLocation().clone().subtract(0, 1, 0);
        if (below.getBlock().getType() != Material.QUARTZ_BLOCK) {
            if (event.isSneaking()) {
                player.sendMessage(Component.text("❌ 必须在石英块上部署！", NamedTextColor.RED));
            }
            return;
        }

        if (event.isSneaking()) {
            bombManager.startPlanting(player);
        } else {
            if (bombManager.isPlanting(player)) {
                bombManager.cancelPlant(player);
                player.sendMessage(Component.text("❌ 部署已取消！", NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        bombManager.onPlayerDeath(event.getEntity());
    }
}