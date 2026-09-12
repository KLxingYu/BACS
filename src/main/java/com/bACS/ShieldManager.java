package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShieldManager {
    private final Map<UUID, Integer> shieldDurability = new HashMap<>();
    private final Map<UUID, Integer> shieldMaxDurability = new HashMap<>();
    private final Map<UUID, Boolean> isBlocking = new HashMap<>();
    private final Map<UUID, Boolean> isEnhancedShield = new HashMap<>();
    private final Map<UUID, Boolean> shieldBrokenThisRound = new HashMap<>();
    private final Map<UUID, Long> dashCooldown = new HashMap<>();

    // ===== 普通盾 =====
    public static final int NORMAL_SHIELD_DURABILITY = 150;
    public static final double NORMAL_SHIELD_DAMAGE_REDUCTION = 0.85;
    public static final double NORMAL_SHIELD_ABSORB_MULTIPLIER = 0.8;
    public static final double NORMAL_SHIELD_SPEED_MULTIPLIER = 0.60;

    // ===== 强化盾 =====
    public static final int ENHANCED_SHIELD_DURABILITY = 250;
    public static final double ENHANCED_SHIELD_DAMAGE_REDUCTION = 0.75;
    public static final double ENHANCED_SHIELD_ABSORB_MULTIPLIER = 0.65;
    public static final double ENHANCED_SHIELD_SPEED_MULTIPLIER = 0.75;
    public static final int ENHANCED_SHIELD_MAX = 270;

    // ============================================================
    // 给予护盾
    // ============================================================
    public void giveNormalShield(Player player) {
        UUID uuid = player.getUniqueId();
        shieldDurability.put(uuid, NORMAL_SHIELD_DURABILITY);
        shieldMaxDurability.put(uuid, NORMAL_SHIELD_DURABILITY);
        isEnhancedShield.put(uuid, false);
        shieldBrokenThisRound.put(uuid, false);
        isBlocking.put(uuid, false);
    }

    public void giveEnhancedShield(Player player) {
        UUID uuid = player.getUniqueId();
        int currentDurability = shieldDurability.getOrDefault(uuid, 0);
        int newDurability = Math.max(currentDurability, ENHANCED_SHIELD_DURABILITY);
        shieldDurability.put(uuid, newDurability);
        shieldMaxDurability.put(uuid, ENHANCED_SHIELD_MAX);
        isEnhancedShield.put(uuid, true);
        shieldBrokenThisRound.put(uuid, false);
    }

    // ============================================================
    // 查询
    // ============================================================
    public int getDurability(Player player) {
        return shieldDurability.getOrDefault(player.getUniqueId(), 0);
    }

    public int getMaxDurability(Player player) {
        return shieldMaxDurability.getOrDefault(player.getUniqueId(), 0);
    }

    public boolean hasShield(Player player) {
        UUID uuid = player.getUniqueId();
        if (shieldBrokenThisRound.getOrDefault(uuid, false)) return false;
        return shieldDurability.getOrDefault(uuid, 0) > 0;
    }

    public boolean isEnhanced(Player player) {
        return isEnhancedShield.getOrDefault(player.getUniqueId(), false);
    }

    public boolean isBlocking(Player player) {
        return isBlocking.getOrDefault(player.getUniqueId(), false);
    }

    public void setBlocking(Player player, boolean blocking) {
        isBlocking.put(player.getUniqueId(), blocking);
    }

    public boolean isShieldBrokenThisRound(Player player) {
        return shieldBrokenThisRound.getOrDefault(player.getUniqueId(), false);
    }

    // ============================================================
    // 消耗耐久
    // ============================================================
    public void consumeDurability(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        int current = shieldDurability.getOrDefault(uuid, 0);
        int newValue = current - amount;
        if (newValue <= 0) {
            shieldDurability.put(uuid, 0);
            shieldBrokenThisRound.put(uuid, true);
            isEnhancedShield.put(uuid, false);
            isBlocking.put(uuid, false);
            player.sendMessage(Component.text("💔 你的盾牌已损坏！本回合无法再使用！", NamedTextColor.RED));

            // ★ 盾牌损坏：3秒失明 + 3秒缓慢4
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 3, true, false));
        } else {
            shieldDurability.put(uuid, newValue);
        }
    }

    // ============================================================
    // 击杀获得耐久
    // ============================================================
    public void onKill(Player player) {
        UUID uuid = player.getUniqueId();
        if (!hasShield(player)) return;
        int current = shieldDurability.getOrDefault(uuid, 0);
        int max = shieldMaxDurability.getOrDefault(uuid, 0);
        int newValue = Math.min(max, current + 20);
        shieldDurability.put(uuid, newValue);
    }

    // ============================================================
    // 跃进冷却
    // ============================================================
    public boolean canDash(Player player) {
        Long end = dashCooldown.get(player.getUniqueId());
        return end == null || System.currentTimeMillis() >= end;
    }

    public void setDashCooldown(Player player, long durationMillis) {
        dashCooldown.put(player.getUniqueId(), System.currentTimeMillis() + durationMillis);
    }

    public long getDashCooldownRemaining(Player player) {
        Long end = dashCooldown.get(player.getUniqueId());
        if (end == null) return 0;
        return Math.max(0, end - System.currentTimeMillis());
    }

    // ============================================================
    // 重置
    // ============================================================
    public void resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        shieldDurability.remove(uuid);
        shieldMaxDurability.remove(uuid);
        isBlocking.remove(uuid);
        isEnhancedShield.remove(uuid);
        shieldBrokenThisRound.remove(uuid);
        dashCooldown.remove(uuid);
    }

    public void resetRound(Player player) {
        UUID uuid = player.getUniqueId();
        shieldBrokenThisRound.put(uuid, false);
        isBlocking.put(uuid, false);
        isEnhancedShield.put(uuid, false);
        dashCooldown.remove(uuid);
    }

    public void resetAll() {
        shieldDurability.clear();
        shieldMaxDurability.clear();
        isBlocking.clear();
        isEnhancedShield.clear();
        shieldBrokenThisRound.clear();
        dashCooldown.clear();
    }
}