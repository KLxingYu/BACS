package com.bACS;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AmmoManager {
    private final Map<UUID, Integer> magazineAmmo = new HashMap<>();    // 当前弹夹内子弹数
    private final Map<UUID, Integer> reserveAmmo = new HashMap<>();     // ★ 备用弹药（按发算）
    private final Map<UUID, Integer> reserveMagazines = new HashMap<>(); // 旧：备用弹夹数量（保留兼容）
    private final Map<UUID, Boolean> isReloading = new HashMap<>();
    private final Map<UUID, Long> lastShotTime = new HashMap<>();

    // ============================================================
    // 弹夹子弹数
    // ============================================================
    public int getMagazineAmmo(UUID playerId) {
        return magazineAmmo.getOrDefault(playerId, 0);
    }

    public void setMagazineAmmo(UUID playerId, int ammo) {
        magazineAmmo.put(playerId, Math.max(0, ammo));
    }

    // ============================================================
    // 备用弹药（按发算）
    // ============================================================
    public int getReserveAmmo(UUID playerId) {
        return reserveAmmo.getOrDefault(playerId, 0);
    }

    public void setReserveAmmo(UUID playerId, int amount) {
        reserveAmmo.put(playerId, Math.max(0, amount));
    }

    public void addReserveAmmo(UUID playerId, int amount) {
        reserveAmmo.put(playerId, getReserveAmmo(playerId) + amount);
    }

    /**
     * 消耗备用弹药，返回实际消耗量
     */
    public int consumeReserveAmmo(UUID playerId, int amount) {
        int current = getReserveAmmo(playerId);
        int actual = Math.min(current, amount);
        reserveAmmo.put(playerId, current - actual);
        return actual;
    }

    // ============================================================
    // 旧：备用弹夹数量（保留兼容）
    // ============================================================
    public int getReserveMagazines(UUID playerId) {
        return reserveMagazines.getOrDefault(playerId, 0);
    }

    public void addReserveMagazines(UUID playerId, int count) {
        reserveMagazines.put(playerId, getReserveMagazines(playerId) + count);
    }

    public void setReserveMagazines(UUID playerId, int amount) {
        reserveMagazines.put(playerId, Math.max(0, amount));
    }

    public boolean consumeReserveMagazine(UUID playerId) {
        int current = getReserveMagazines(playerId);
        if (current <= 0) return false;
        reserveMagazines.put(playerId, current - 1);
        return true;
    }

    // ============================================================
    // 射击
    // ============================================================
    public boolean consumeBullet(UUID playerId) {
        int current = getMagazineAmmo(playerId);
        if (current <= 0) return false;
        magazineAmmo.put(playerId, current - 1);
        return true;
    }

    // ============================================================
    // 换弹
    // ============================================================
    public boolean isReloading(UUID playerId) {
        return isReloading.getOrDefault(playerId, false);
    }

    public void setReloading(UUID playerId, boolean reloading) {
        isReloading.put(playerId, reloading);
    }

    // ============================================================
    // 冷却
    // ============================================================
    public boolean canShoot(UUID playerId, long cooldownMillis) {
        long last = lastShotTime.getOrDefault(playerId, 0L);
        return System.currentTimeMillis() - last >= cooldownMillis;
    }

    public long getCooldownRemaining(UUID playerId, long cooldownMillis) {
        long last = lastShotTime.getOrDefault(playerId, 0L);
        long elapsed = System.currentTimeMillis() - last;
        long remaining = cooldownMillis - elapsed;
        return Math.max(0, remaining);
    }

    public void setLastShotTime(UUID playerId, long time) {
        lastShotTime.put(playerId, time);
    }

    // ============================================================
    // 重置
    // ============================================================
    public void resetRound(UUID playerId, int magazineSize) {
        magazineAmmo.put(playerId, magazineSize);
        isReloading.put(playerId, false);
        lastShotTime.put(playerId, 0L);
    }

    public void resetRound(UUID playerId, int magazineSize, int initialReserveAmmo) {
        magazineAmmo.put(playerId, magazineSize);
        reserveAmmo.put(playerId, initialReserveAmmo);
        isReloading.put(playerId, false);
        lastShotTime.put(playerId, 0L);
    }

    public void resetPlayer(UUID playerId) {
        magazineAmmo.remove(playerId);
        reserveAmmo.remove(playerId);
        reserveMagazines.remove(playerId);
        isReloading.remove(playerId);
        lastShotTime.remove(playerId);
    }

    public void resetAll() {
        magazineAmmo.clear();
        reserveAmmo.clear();
        reserveMagazines.clear();
        isReloading.clear();
        lastShotTime.clear();
    }
}