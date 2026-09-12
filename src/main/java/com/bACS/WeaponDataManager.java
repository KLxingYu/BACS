package com.bACS;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 将副武器的弹药数据写入物品本身（PDC），
 * 丢弃/掉落后数据跟随物品一起保存。
 */
public class WeaponDataManager {
    private final NamespacedKey ammoKey;
    private final NamespacedKey reserveKey;

    public WeaponDataManager(JavaPlugin plugin) {
        this.ammoKey = new NamespacedKey(plugin, "weapon_ammo");
        this.reserveKey = new NamespacedKey(plugin, "weapon_reserve");
    }

    /** 写入弹药数据 */
    public void writeAmmo(ItemStack item, int ammo, int reserve) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ammoKey, PersistentDataType.INTEGER, ammo);
        pdc.set(reserveKey, PersistentDataType.INTEGER, reserve);
        item.setItemMeta(meta);
    }

    /** 读取弹药数据（默认 -1 表示无数据） */
    public int readAmmo(ItemStack item, int defaultValue) {
        if (item == null || item.getType().isAir()) return defaultValue;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return defaultValue;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer v = pdc.get(ammoKey, PersistentDataType.INTEGER);
        return v != null ? v : defaultValue;
    }

    public int readReserve(ItemStack item, int defaultValue) {
        if (item == null || item.getType().isAir()) return defaultValue;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return defaultValue;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer v = pdc.get(reserveKey, PersistentDataType.INTEGER);
        return v != null ? v : defaultValue;
    }

    /** 判断物品是否带有弹药数据 */
    public boolean hasData(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(ammoKey, PersistentDataType.INTEGER);
    }
}