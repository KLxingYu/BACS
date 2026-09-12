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

import java.util.HashMap;
import java.util.Map;

public class ShopClickListener implements Listener {

    private final BACS plugin;
    private final GameManager gameManager;
    private final Map<String, Integer> itemPrices = new HashMap<>();

    public ShopClickListener(BACS plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();

        // 初始化价格
        itemPrices.put("铁剑", 10);
        itemPrices.put("钻石剑", 30);
        itemPrices.put("铁头盔", 8);
        itemPrices.put("铁胸甲", 12);
        itemPrices.put("铁护腿", 10);
        itemPrices.put("铁靴子", 6);
        itemPrices.put("弓", 15);
        itemPrices.put("箭 x16", 5);
        itemPrices.put("面包 x8", 3);
        itemPrices.put("金苹果", 20);
        itemPrices.put("治疗药水", 8);
        itemPrices.put("速度药水", 8);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().contains("商店")) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        if (gameManager.getState() != GameState.PREPARE) {
            player.sendMessage(Component.text("❌ 只能在准备阶段购买物品！", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        if (!gameManager.isPlayerInShopArea(player)) {
            player.sendMessage(Component.text("❌ 你不在商店区域内！", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        String itemName = getItemDisplayName(clicked);
        if (itemName == null) {
            player.sendMessage(Component.text("❌ 无法识别此物品！", NamedTextColor.RED));
            return;
        }

        Integer price = itemPrices.get(itemName);
        if (price == null) {
            player.sendMessage(Component.text("❌ 无法识别此物品！", NamedTextColor.RED));
            return;
        }

        int level = player.getLevel();
        if (level < price) {
            player.sendMessage(Component.text("❌ 金币不足！需要 " + price + " 金币，你有 " + level + " 金币", NamedTextColor.RED));
            return;
        }

        player.setLevel(level - price);

        ItemStack giveItem = clicked.clone();
        giveItem.setAmount(1);
        ItemMeta meta = giveItem.getItemMeta();
        meta.lore(null);
        giveItem.setItemMeta(meta);

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(Component.text("❌ 背包已满！", NamedTextColor.RED));
            player.setLevel(level);
            return;
        }

        player.getInventory().addItem(giveItem);
        player.sendMessage(Component.text("✅ 你购买了 " + itemName + "！花费 " + price + " 金币", NamedTextColor.GREEN));
        player.sendMessage(Component.text("剩余金币: " + player.getLevel(), NamedTextColor.GOLD));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().contains("商店")) {
            event.setCancelled(true);
        }
    }

    private String getItemDisplayName(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return null;
        // 提取纯文本名称
        String name = meta.displayName().toString();
        // 移除 Minecraft 颜色代码
        name = name.replaceAll("§[0-9a-fk-or]", "");
        // 移除 Adventure 格式
        name = name.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s\\-x×➹⚔⛑🏹🍞🍎💚💨]", "");
        name = name.trim();
        return name;
    }
}