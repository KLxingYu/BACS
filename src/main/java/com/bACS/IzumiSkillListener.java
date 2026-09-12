package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class IzumiSkillListener implements Listener {
    private final BACS plugin;
    private final GameManager gameManager;
    private final Map<UUID, Integer> energyBalls = new HashMap<>();
    private final Map<UUID, Boolean> isInvincible = new HashMap<>();
    private final Map<UUID, Integer> tempShield = new HashMap<>();
    private final Map<UUID, Boolean> exActive = new HashMap<>();
    private final Map<UUID, Double> healingPlayers = new HashMap<>();

    public IzumiSkillListener(BACS plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    public void onKill(Player killer, Player victim) {
        if (killer == null || victim == null) return;
        HeroKit kit = gameManager.getPlayerHero(killer);
        if (kit == null || !kit.getId().equals("izumi")) return;
        energyBalls.put(killer.getUniqueId(), energyBalls.getOrDefault(killer.getUniqueId(), 0) + 1);
    }

    public void onRoundStart(Player player) {
        energyBalls.put(player.getUniqueId(), 0);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("izumi")) return;

        UUID uuid = player.getUniqueId();
        int balls = energyBalls.getOrDefault(uuid, 0);
        if (balls <= 0) return;

        boolean isMelee = item.getType() == kit.getMeleeWeaponMaterial() &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains(kit.getMeleeWeaponName());
        boolean isMainOrSidearm = (item.getType() == kit.getWeaponMaterial() || item.getType() == Material.WOODEN_AXE || item.getType() == Material.IRON_AXE);

        if (isMelee) {
            event.setCancelled(true);
            energyBalls.put(uuid, balls - 1);
            activateSkill1(player);
        } else if (isMainOrSidearm) {
            event.setCancelled(true);
            energyBalls.put(uuid, balls - 1);
            activateSkill2(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("izumi")) return;
        if (item.getType() != kit.getMeleeWeaponMaterial()) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().displayName().toString().contains(kit.getMeleeWeaponName())) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!player.isSneaking()) return;

        event.setCancelled(true);
        activateUltimate(player);
    }

    private void activateUltimate(Player player) {
        gameManager.getExSkillManager().clearCharge(player);
        UUID uuid = player.getUniqueId();
        exActive.put(uuid, true);
        player.sendMessage(Component.text("⚡ 泉EX技能已激活！", NamedTextColor.GREEN));
    }

    private void activateSkill1(Player player) {
        UUID uuid = player.getUniqueId();
        int duration = 47; // 2.35s
        isInvincible.put(uuid, true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 1, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 4, true, false)); // 速度5
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 50, 0, true, false)); // 2.5s发光
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,duration,4,true,false));//无敌
        new BukkitRunnable() {
            @Override
            public void run() {
                isInvincible.put(uuid, false);
            }
        }.runTaskLater(plugin, duration);
    }

    private void activateSkill2(Player player) {
        UUID uuid = player.getUniqueId();
        healingPlayers.put(uuid, 1.5);
        new BukkitRunnable() {
            int ticks = 0;
            int healed = 0;
            @Override
            public void run() {
                if (ticks >= 70) { // 3.5s
                    healingPlayers.remove(uuid);
                    cancel();
                    return;
                }
                if (ticks % 10 == 0 && healed < 40) {
                    int amount = Math.min(4, 40 - healed);
                    player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + amount));
                    healed += amount;
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public boolean absorbDamage(Player player, double damage) {
        UUID uuid = player.getUniqueId();
        int shield = tempShield.getOrDefault(uuid, 0);
        if (shield <= 0) return false;
        double absorbed = Math.min(shield, damage * 0.99);
        tempShield.put(uuid, shield - (int)Math.ceil(absorbed));
        return true;
    }

    public boolean isInvincible(Player player) { return isInvincible.getOrDefault(player.getUniqueId(), false); }
    public int getTempShield(Player player) { return tempShield.getOrDefault(player.getUniqueId(), 0); }
    public double getHealingMultiplier(Player player) { return healingPlayers.getOrDefault(player.getUniqueId(), 1.0); }
    public boolean isExActive(Player player) { return exActive.getOrDefault(player.getUniqueId(), false); }

    public void resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        energyBalls.remove(uuid); isInvincible.remove(uuid); tempShield.remove(uuid); exActive.remove(uuid); healingPlayers.remove(uuid);
    }

    public void resetAll() {
        energyBalls.clear(); isInvincible.clear(); tempShield.clear(); exActive.clear(); healingPlayers.clear();
    }
}