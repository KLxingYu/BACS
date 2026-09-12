package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class WeaponPickupListener implements Listener {
    private final BACS plugin;
    private final GameManager gameManager;

    public WeaponPickupListener(BACS plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack item = event.getItem().getItemStack();
        if (item == null || item.getType().isAir()) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String name = item.getItemMeta().displayName().toString();
        WeaponDataManager wdm = plugin.getWeaponDataManager();
        ShopManager shop = gameManager.getShopManager();

        // ===== 制式手枪 =====
        if (item.getType() == Material.WOODEN_AXE && name.contains("制式手枪")) {
            int ammo = wdm.readAmmo(item, 12);
            int reserve = wdm.readReserve(item, 12);
            shop.setPistolAmmo(player, ammo);
            shop.setPistolReserve(player, reserve);
            shop.setHasPistol(player, true);
            player.sendMessage(Component.text("🔫 拾取了制式手枪（" + ammo + "发, 备用" + reserve + "）", NamedTextColor.GRAY));
            return;
        }

        // ===== 灵异 =====
        if (item.getType() == Material.IRON_AXE && name.contains("灵异")) {
            int ammo = wdm.readAmmo(item, 15);
            int reserve = wdm.readReserve(item, 30);
            shop.setSpiritGunAmmo(player, ammo);
            shop.setSpiritGunReserve(player, reserve);
            shop.setHasSpiritGun(player, true);
            player.sendMessage(Component.text("👻 拾取了灵异（" + ammo + "发, 备用" + reserve + "）", NamedTextColor.DARK_PURPLE));
            return;
        }

        // ===== 裁决 =====
        if (item.getType() == Material.IRON_AXE && name.contains("裁决")) {
            int ammo = wdm.readAmmo(item, 6);
            int reserve = wdm.readReserve(item, 12);
            shop.setVerdictAmmo(player, ammo);
            shop.setVerdictReserve(player, reserve);
            shop.setHasVerdict(player, true);
            player.sendMessage(Component.text("⚖ 拾取了裁决（" + ammo + "发, 备用" + reserve + "）", NamedTextColor.GOLD));
            return;
        }

        // ===== 怒焰（实际材质：GOLDEN_AXE） =====
        if (item.getType() == Material.GOLDEN_AXE && name.contains("怒焰")) {
            int ammo = wdm.readAmmo(item, WeaponConfig.RAGE_MAX_AMMO);
            int reserve = wdm.readReserve(item, WeaponConfig.RAGE_INITIAL_RESERVE);
            shop.setRageAmmo(player, ammo);
            shop.setRageReserve(player, reserve);
            shop.setHasRage(player, true);
            player.sendMessage(Component.text("🔥 拾取了怒焰（" + ammo + "发, 备用" + reserve + "）", NamedTextColor.GOLD));
            return;
        }

        // ===== 手炮（实际材质：NETHERITE_AXE） =====
        if (item.getType() == Material.NETHERITE_AXE && name.contains("手炮")) {
            int ammo = wdm.readAmmo(item, WeaponConfig.HANDCANNON_MAX_AMMO);
            int reserve = wdm.readReserve(item, WeaponConfig.HANDCANNON_INITIAL_RESERVE);
            shop.setHandCannonAmmo(player, ammo);
            shop.setHandCannonReserve(player, reserve);
            shop.setHasHandCannon(player, true);
            player.sendMessage(Component.text("💥 拾取了手炮（" + ammo + "发, 备用" + reserve + "）", NamedTextColor.RED));
            return;
        }

        // ===== 主武器 =====
        for (HeroKit kit : GameManager.HERO_KITS) {
            if (kit.getWeaponMaterial() == Material.AIR) continue;
            if (item.getType() == kit.getWeaponMaterial() && name.contains(kit.getWeaponName())) {
                int ammo = wdm.readAmmo(item, kit.getMagazineSize());
                int reserve = wdm.readReserve(item, kit.getInitialReserveAmmo());
                gameManager.getAmmoManager().setMagazineAmmo(player.getUniqueId(), ammo);
                gameManager.getAmmoManager().setReserveAmmo(player.getUniqueId(), reserve);
                player.sendMessage(Component.text("⚔ 拾取了 " + kit.getWeaponName() + "（" + ammo + "/" + reserve + "）", NamedTextColor.GREEN));
                break;
            }
        }
    }
}