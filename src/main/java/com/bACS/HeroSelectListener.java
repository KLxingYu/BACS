package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HeroSelectListener implements Listener {

    private final BACS plugin;
    private final GameManager gameManager;

    public HeroSelectListener(BACS plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().contains("选择英雄")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        ItemStack clicked = event.getCurrentItem();
        Material type = clicked.getType();

        if (type == Material.GRAY_STAINED_GLASS_PANE) return;
        if (gameManager.getState() != GameState.HERO_SELECT) {
            player.sendMessage(Component.text("❌ 现在不是英雄选择阶段！", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        // 【修复】使用物品的持久化数据或直接匹配显示名称
        String heroId = getHeroIdFromItem(clicked);
        if (heroId == null) {
            player.sendMessage(Component.text("❌ 无法识别此英雄！", NamedTextColor.RED));
            return;
        }
        gameManager.selectHero(player, heroId);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().contains("选择英雄")) {
            event.setCancelled(true);
        }
    }

    private String getHeroIdFromItem(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return null;

        // 【修复】直接使用Component的文本内容进行匹配
        String displayName = meta.displayName().toString();

        // 遍历所有英雄，检查显示名称是否包含英雄名称
        for (HeroKit kit : GameManager.HERO_KITS) {
            String kitName = kit.getName();

            // 【修复】多种匹配方式
            // 1. 检查显示名称是否包含英雄名称
            if (displayName.contains(kitName)) {
                return kit.getId();
            }

            // 2. 检查英雄显示名称是否包含在物品显示名称中
            String kitDisplayName = kit.getDisplayName().toString();
            if (displayName.contains(kitDisplayName) || kitDisplayName.contains(displayName)) {
                return kit.getId();
            }

            // 3. 去除颜色代码后匹配
            String cleanDisplayName = displayName.replaceAll("§[0-9a-fk-or]", "")
                    .replaceAll("(?i)italic|bold|underlined|strikethrough|obfuscated", "")
                    .replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "").trim();
            String cleanKitName = kitName.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "").trim();

            if (cleanDisplayName.equalsIgnoreCase(cleanKitName) ||
                    cleanDisplayName.contains(cleanKitName)) {
                return kit.getId();
            }
        }

        return null;
    }
}