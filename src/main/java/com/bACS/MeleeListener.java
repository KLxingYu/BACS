package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class MeleeListener implements Listener {
    private final BACS plugin;
    private final GameManager gameManager;
    private final Map<UUID, Long> lastMeleeAttack = new HashMap<>();
    private final Map<UUID, Long> weaponSwitchTime = new HashMap<>();
    private final Random random = new Random();
    private static final long MELEE_SWITCH_COOLDOWN = 500;
    private static final long MAIN_WEAPON_SWITCH_COOLDOWN_AL1S = 2000;
    private static final long MAIN_WEAPON_SWITCH_COOLDOWN_KUROKO = 1200;
    private static final long MAIN_WEAPON_SWITCH_COOLDOWN_KAYI = 1750;

    public MeleeListener(BACS plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (gameManager.getState() != GameState.ROUND_ACTIVE) return;
        if (!gameManager.isPlayerAlive(attacker) || !gameManager.isPlayerAlive(victim)) return;

        HeroKit kit = gameManager.getPlayerHero(attacker);
        if (kit == null) return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType() != kit.getMeleeWeaponMaterial()) return;
        if (!weapon.hasItemMeta() || !weapon.getItemMeta().hasDisplayName()) return;
        if (!weapon.getItemMeta().displayName().toString().contains(kit.getMeleeWeaponName())) return;

        UUID uuid = attacker.getUniqueId();
        Long switchTime = weaponSwitchTime.get(uuid);
        if (switchTime != null && System.currentTimeMillis() - switchTime < MELEE_SWITCH_COOLDOWN) {
            event.setCancelled(true);
            attacker.sendActionBar(Component.text("⏳ 武器切换冷却中...", NamedTextColor.RED));
            return;
        }

        // 检查攻速冷却
        long lastAttack = lastMeleeAttack.getOrDefault(uuid, 0L);
        long attackCooldown = (long)(1000.0 / kit.getMeleeAttackSpeed());
        if (System.currentTimeMillis() - lastAttack < attackCooldown) {
            event.setCancelled(true);
            return;
        }

        // 检查队友
        String attackerTeam = gameManager.getPlayerTeam(attacker);
        String victimTeam = gameManager.getPlayerTeam(victim);
        if (attackerTeam != null && victimTeam != null && attackerTeam.equals(victimTeam)) {
            event.setCancelled(true);
            return;
        }

        lastMeleeAttack.put(uuid, System.currentTimeMillis());

        // 计算伤害
        double damage = kit.getMeleeDamage();
        boolean isBackstab = isBackstab(attacker, victim);
        boolean isCrit = random.nextDouble() < 0.2;

        if (isBackstab) {
            damage = isCrit ? kit.getMeleeBackstabCritDamage() : kit.getMeleeBackstabDamage();
        } else if (isCrit) {
            damage = kit.getMeleeCritDamage();
        }

        // 凯伊伤害提升（来自EX技能）
        double damageBoost = gameManager.getExSkillManager().getDamageBoost(attacker);
        if (damageBoost > 0) {
            damage *= (1 + damageBoost);
        }

        event.setDamage(damage);

        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
        victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);

        if (isBackstab && isCrit) {
            victim.sendMessage(Component.text("💀 被背刺暴击！", NamedTextColor.DARK_RED));
        } else if (isBackstab) {
            victim.sendMessage(Component.text("💀 被背刺！", NamedTextColor.RED));
        } else if (isCrit) {
            victim.sendMessage(Component.text("💥 暴击！", NamedTextColor.GOLD));
        }
    }

    private boolean isBackstab(Player attacker, Player victim) {
        Vector attackerDir = attacker.getLocation().getDirection().normalize();
        Vector victimDir = victim.getLocation().getDirection().normalize();
        double dot = attackerDir.dot(victimDir);
        return dot > 0.3;
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null) return;

        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (newItem == null) return;

        long cooldown = 0;
        Material cooldownMaterial = null;

        if (newItem.getType() == kit.getMeleeWeaponMaterial() &&
                newItem.hasItemMeta() && newItem.getItemMeta().hasDisplayName() &&
                newItem.getItemMeta().displayName().toString().contains(kit.getMeleeWeaponName())) {
            cooldown = MELEE_SWITCH_COOLDOWN;
            cooldownMaterial = kit.getMeleeWeaponMaterial();
        } else if (newItem.getType() == kit.getWeaponMaterial() &&
                newItem.hasItemMeta() && newItem.getItemMeta().hasDisplayName() &&
                newItem.getItemMeta().displayName().toString().contains(kit.getWeaponName())) {
            if (kit.getId().equals("al1s")) {
                cooldown = MAIN_WEAPON_SWITCH_COOLDOWN_AL1S;
            } else if (kit.getId().equals("kuroko")) {
                cooldown = MAIN_WEAPON_SWITCH_COOLDOWN_KUROKO;
            } else if (kit.getId().equals("kayi")) {
                cooldown = MAIN_WEAPON_SWITCH_COOLDOWN_KAYI;
            }
            cooldownMaterial = kit.getWeaponMaterial();
        }

        if (cooldown > 0 && cooldownMaterial != null) {
            weaponSwitchTime.put(player.getUniqueId(), System.currentTimeMillis());
            player.setCooldown(cooldownMaterial, (int)(cooldown / 50));
        }

        updateWalkSpeed(player, newItem, kit);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null) return;
        updateWalkSpeed(player, mainHand, kit);
    }

    private void updateWalkSpeed(Player player, ItemStack item, HeroKit kit) {
        float speed = 0.2f;

        if (item.getType() == kit.getMeleeWeaponMaterial() &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains(kit.getMeleeWeaponName())) {
            speed *= (1 + kit.getMeleeSpeedBoost());
        } else if (item.getType() == kit.getWeaponMaterial() &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains(kit.getWeaponName())) {
            speed *= (1 - kit.getWeaponSpeedReduction());
        }

        player.setWalkSpeed(speed);
    }
}