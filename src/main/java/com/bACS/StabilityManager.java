package com.bACS;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StabilityManager {
    private final Map<UUID, Integer> stability = new HashMap<>();
    private static final int DEFAULT_STABILITY = 6000;
    private static final int MAX_STABILITY = 10000;
    private static final int MIN_STABILITY = 0;

    public int getStability(Player player) {
        return stability.getOrDefault(player.getUniqueId(), DEFAULT_STABILITY);
    }

    public void setStability(Player player, int value) {
        stability.put(player.getUniqueId(), Math.max(MIN_STABILITY, Math.min(MAX_STABILITY, value)));
    }

    public void addStability(Player player, int amount) {
        int current = getStability(player);
        setStability(player, current + amount);
    }

    public int getEffectiveStability(Player player) {
        int base = getStability(player);
        boolean isSecondary = false;
        boolean isExempt = false;

        // 检查手持物品
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.hasItemMeta() && mainHand.getItemMeta().hasDisplayName()) {
            String name = mainHand.getItemMeta().displayName().toString();
            if (name.contains("制式手枪") || name.contains("灵异") || name.contains("裁决")) {
                isSecondary = true;
            }
            if (name.contains("超新星·光之剑") || name.contains("改装·光之剑")) {
                isExempt = true;
            }
        }

        if (isExempt) {
            return base;
        }

        int totalReduction = 0;
        double hSpeed = Math.sqrt(Math.pow(player.getVelocity().getX(), 2) + Math.pow(player.getVelocity().getZ(), 2));

        // 血量 < 30
        if (player.getHealth() < 30) {
            int red = (int)(base * 0.2);
            red = Math.max(1000, Math.min(1500, red));
            totalReduction += red;
        }

        // 水平速度 >= 1
        if (hSpeed >= 1.0) {
            int red = (int)(base * 0.3);
            red = Math.max(2999, red);
            totalReduction += red;
        }

        // 在空中
        if (!player.isOnGround()) {
            int red = (int)(base * 0.2);
            totalReduction += red;
        }

        if (isSecondary) {
            totalReduction = (int)(totalReduction * 0.5);
        }

        totalReduction = Math.min(totalReduction, 5000);
        return Math.max(MIN_STABILITY, base - totalReduction);
    }

    public double getDamageMultiplier(int effectiveStability) {
        double lower = 0.5 + (effectiveStability / 10000.0) * 0.49;
        double upper = 1.05;
        return lower + (upper - lower) * Math.random();
    }

    public void resetAll() {
        stability.clear();
    }

    public void resetPlayer(Player player) {
        stability.remove(player.getUniqueId());
    }
}