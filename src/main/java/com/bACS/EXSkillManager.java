package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EXSkillManager {
    private final Map<UUID, Integer> exCharges = new HashMap<>();
    private final Map<UUID, Boolean> exActive = new HashMap<>();
    private final Map<UUID, Integer> tempShield = new HashMap<>(); // 黑子临时护盾
    private final Map<UUID, Integer> specialShield = new HashMap<>(); // 凯伊特殊护盾
    private final Map<UUID, Double> damageBoost = new HashMap<>(); // 凯伊伤害提升

    public void addCharge(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        HeroKit kit = BACS.getInstance().getGameManager().getPlayerHero(player);
        if (kit == null) return;

        int current = exCharges.getOrDefault(uuid, 0);
        int maxCharge = kit.getExSkillMaxCharge();
        if (current >= maxCharge) return;

        exCharges.put(uuid, Math.min(maxCharge, current + amount));
    }

    public int getCharge(Player player) {
        return exCharges.getOrDefault(player.getUniqueId(), 0);
    }

    public boolean isExActive(Player player) {
        return exActive.getOrDefault(player.getUniqueId(), false);
    }

    public int getTempShield(Player player) {
        return tempShield.getOrDefault(player.getUniqueId(), 0);
    }

    public int getSpecialShield(Player player) {
        return specialShield.getOrDefault(player.getUniqueId(), 0);
    }

    public double getDamageBoost(Player player) {
        return damageBoost.getOrDefault(player.getUniqueId(), 0.0);
    }

    public void addTempShield(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        int current = tempShield.getOrDefault(uuid, 0);
        tempShield.put(uuid, Math.min(60, current + amount));
    }

    public void addSpecialShield(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        int current = specialShield.getOrDefault(uuid, 0);
        specialShield.put(uuid, current + amount);
    }

    public void setDamageBoost(Player player, double boost) {
        damageBoost.put(player.getUniqueId(), boost);
    }

    public boolean consumeTempShield(Player player, double damage) {
        UUID uuid = player.getUniqueId();
        int shield = tempShield.getOrDefault(uuid, 0);
        if (shield <= 0) return false;

        double absorbed = Math.min(shield, damage);
        tempShield.put(uuid, shield - (int)Math.ceil(absorbed));
        return true;
    }

    public boolean consumeSpecialShield(Player player, double damage) {
        UUID uuid = player.getUniqueId();
        int shield = specialShield.getOrDefault(uuid, 0);
        if (shield <= 0) return false;

        double absorbed = Math.min(shield, damage);
        specialShield.put(uuid, shield - (int)Math.ceil(absorbed));
        return true;
    }

    public void activateEX(Player player) {
        UUID uuid = player.getUniqueId();
        HeroKit kit = BACS.getInstance().getGameManager().getPlayerHero(player);
        if (kit == null) return;

        if (exCharges.getOrDefault(uuid, 0) < kit.getExSkillMaxCharge()) return;

        exCharges.put(uuid, 0);
        exActive.put(uuid, true);

        if (kit.getId().equals("kuroko")) {
            // 黑子EX技能
            addTempShield(player, 30);
            Bukkit.broadcast(Component.text("⚡ " + player.getName() + " 触发了EX技能！伤害+37%，立即获得30点特殊护甲！", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true));
        } else if (kit.getId().equals("al1s")) {
            // AL1S EX技能
            for (Player target : Bukkit.getOnlinePlayers()) {
                String targetTeam = BACS.getInstance().getGameManager().getPlayerTeam(target);
                String playerTeam = BACS.getInstance().getGameManager().getPlayerTeam(player);
                if (targetTeam != null && playerTeam != null && !targetTeam.equals(playerTeam)) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0, true, false));
                }
            }
            Bukkit.broadcast(Component.text("⚡ " + player.getName() + " 触发了EX技能！敌方全体获得发光效果！", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true));
        } else if (kit.getId().equals("kayi")) {
            // 凯伊EX技能
            addSpecialShield(player, 30); // 自身30点特殊护盾

            // 找到最近的队友
            Player nearestTeammate = findNearestTeammate(player);
            if (nearestTeammate != null) {
                addSpecialShield(nearestTeammate, 120); // 队友120点特殊护盾
                setDamageBoost(nearestTeammate, 0.15); // 队友伤害提升15%
                nearestTeammate.sendMessage(Component.text("⚡ " + player.getName() + " 的EX技能为你提供了120点特殊护盾和15%伤害提升！", NamedTextColor.GOLD));
            }

            Bukkit.broadcast(Component.text("⚡ " + player.getName() + " 触发了EX技能！", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true));
        }
    }

    private Player findNearestTeammate(Player player) {
        String playerTeam = BACS.getInstance().getGameManager().getPlayerTeam(player);
        if (playerTeam == null) return null;

        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target == player || !target.isOnline()) continue;
            if (!BACS.getInstance().getGameManager().isPlayerAlive(target)) continue;

            String targetTeam = BACS.getInstance().getGameManager().getPlayerTeam(target);
            if (targetTeam == null || !targetTeam.equals(playerTeam)) continue;

            double distance = player.getLocation().distance(target.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = target;
            }
        }

        return nearest;
    }

    public void onPlayerDeath(Player player) {
        UUID uuid = player.getUniqueId();
        if (exActive.getOrDefault(uuid, false)) {
            exActive.put(uuid, false);
            tempShield.remove(uuid);
        }
        specialShield.remove(uuid);
        damageBoost.remove(uuid);
    }
    public void setCharge(Player player, int amount) {
        exCharges.put(player.getUniqueId(), Math.max(0, amount));
    }

    public void onKill(Player player) {
        HeroKit kit = BACS.getInstance().getGameManager().getPlayerHero(player);
        if (kit != null && kit.getId().equals("kuroko") && isExActive(player)) {
            addTempShield(player, 30);
        }
    }
    public void clearCharge(Player player) {
        exCharges.put(player.getUniqueId(), 0);
    }

    // 回合开始时清理特殊护盾和伤害提升（黑子的临时护盾也清理）
    public void onRoundStart() {
        specialShield.clear();
        damageBoost.clear();
        tempShield.clear();
    }

    public void resetAll() {
        exCharges.clear();
        exActive.clear();
        tempShield.clear();
        specialShield.clear();
        damageBoost.clear();
    }
}