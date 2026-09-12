package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MenuListener implements Listener {
    private final BACS plugin;

    public MenuListener(BACS plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        // 在MenuListener的onInventoryClick中添加
        if (title.contains("购买套件")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.displayName().toString().contains("优香")) {
                plugin.getCoinManager().buyYuukaKit(player);
                player.closeInventory();
            }
        }

        // 英雄选择菜单
        if (title.contains("选择英雄")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            // 处理英雄选择（下界之星）
            if (clicked.getType() == Material.NETHER_STAR) {
                ItemMeta meta = clicked.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String displayName = meta.displayName().toString();
                    for (HeroKit kit : GameManager.HERO_KITS) {
                        if (displayName.contains(kit.getName())) {
                            plugin.getGameManager().selectHero(player, kit.getId());
                            break;
                        }
                    }
                }
            }
        }

        // 阵营选择菜单
        if (title.contains("选择你的阵营")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;

            if (clicked.getType() == Material.RED_WOOL) {
                plugin.getGameManager().selectTeam(player, GameManager.TEAM_ATTACKER);
            } else if (clicked.getType() == Material.BLUE_WOOL) {
                plugin.getGameManager().selectTeam(player, GameManager.TEAM_DEFENDER);
            }
        }

        // 商店菜单
        if (title.contains("商店")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;

            String displayName = meta.displayName().toString();
            plugin.getGameManager().getShopManager().handleShopClick(player, clicked, displayName);
        }
    }

    // 处理右键打开商店
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;
        if (item.getType() != Material.GHAST_TEAR) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().displayName().equals(GameManager.SHOP_ITEM_NAME)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            plugin.getGameManager().getShopManager().openShop(player);
        }
    }
}