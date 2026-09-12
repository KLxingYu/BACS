package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryProtectListener implements Listener {

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        // ★ 主武器允许按 Q 丢弃
        if (isMainWeapon(item)) {
            return;
        }

        if (isRestrictedItem(item)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ 此物品不可丢弃！", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack item = event.getCurrentItem();
        if (item != null && isRestrictedItem(item)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ 此物品不可移动！", NamedTextColor.RED));
            return;
        }

        ItemStack cursor = event.getCursor();
        if (cursor != null && isRestrictedItem(cursor)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ 此物品不可移动！", NamedTextColor.RED));
            return;
        }

        if (event.getClick().isKeyboardClick()) {
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0 && hotbarButton < 9) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                if (hotbarItem != null && isRestrictedItem(hotbarItem)) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("❌ 此物品不可移动！", NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack cursor = event.getCursor();
        if (cursor != null && isRestrictedItem(cursor)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ 此物品不可移动！", NamedTextColor.RED));
        }
    }

    /**
     * 判断是否主武器（遍历所有 HERO_KITS）
     */
    private boolean isMainWeapon(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;

        for (HeroKit kit : GameManager.HERO_KITS) {
            if (kit.getWeaponMaterial() == Material.AIR) continue;
            if (item.getType() == kit.getWeaponMaterial()) {
                String name = item.getItemMeta().displayName().toString();
                if (name.contains(kit.getWeaponName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断是否受限物品（不可移动 / 不可丢弃，主武器例外）
     */
    private boolean isRestrictedItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;

        // 主武器不受限
        if (isMainWeapon(item)) {
            return false;
        }

        String name = item.getItemMeta().displayName().toString();

        // 近战武器
        if (name.contains("匕首") || name.contains("长刀") || name.contains("军用匕首") ||
                name.contains("合金稿") || name.contains("圣三一之剑") || name.contains("计算之刃") ||
                name.contains("治疗之杖") || name.contains("战术匕首") || name.contains("防卫匕首") ||
                name.contains("突击剑")) {
            return true;
        }

        // 小技能物品
        if (name.contains("治疗之光") || name.contains("EX.在这里的我") || name.contains("探测箭矢")) {
            return true;
        }

        // 商店物品
        if (name.contains("商店")) {
            return true;
        }

        // 炸弹相关
        if (name.contains("拆弹器") || name.contains("炸弹起爆器") ||
                name.contains("炸药追踪器") || name.contains("炸弹部署器")) {
            return true;
        }

        // EX 技能物品
        if (name.contains("EX技能")) {
            return true;
        }

        return false;
    }
}