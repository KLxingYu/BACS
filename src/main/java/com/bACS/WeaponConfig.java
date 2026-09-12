package com.bACS;

public class WeaponConfig {
    // ===== 怒焰 =====
    public static final int RAGE_PRICE = 500;
    public static final int RAGE_MAX_AMMO = 15;
    public static final int RAGE_INITIAL_RESERVE = 30;
    public static final long RAGE_FIRE_INTERVAL = 122;
    public static final long RAGE_RELOAD_TIME = 1000;
    public static final long RAGE_SWITCH_COOLDOWN = 1000;
    public static final int RAGE_PENETRATION = 1;
    public static final double RAGE_PENETRATION_DAMAGE = 0.5;
    public static final double RAGE_HEAD_DAMAGE = 79;
    public static final double RAGE_CHEST_DAMAGE = 29;
    public static final double RAGE_LIMB_DAMAGE = 20;
    public static final double RAGE_FALLOFF_START = 24.0;
    public static final double RAGE_FALLOFF_MULT = 0.7;
    public static final double RAGE_SPREAD_IDLE = 0.0;
    public static final double RAGE_SPREAD_MOVING = 3.0;
    public static final double RAGE_SPREAD_SPRINTING = 5.0;

    // ===== 手炮 =====
    public static final int HANDCANNON_PRICE = 600;
    public static final int HANDCANNON_MAX_AMMO = 2;
    public static final int HANDCANNON_INITIAL_RESERVE = 6;
    public static final long HANDCANNON_FIRE_INTERVAL = 167;
    public static final long HANDCANNON_RELOAD_TIME_FULL = 1700;
    public static final long HANDCANNON_RELOAD_TIME_PARTIAL = 1200;
    public static final long HANDCANNON_SWITCH_COOLDOWN = 1200;
    public static final int HANDCANNON_PELLETS = 13;
    public static final double HANDCANNON_HEAD_DAMAGE = 20;
    public static final double HANDCANNON_CHEST_DAMAGE = 10;
    public static final double HANDCANNON_LIMB_DAMAGE = 8;
    public static final double HANDCANNON_FALLOFF_START = 10.0;
    public static final double HANDCANNON_FALLOFF_PER_10 = 0.2;
    public static final double HANDCANNON_MAX_RANGE = 40.0;
    public static final double HANDCANNON_SPREAD_IDLE = 0.0;
    public static final double HANDCANNON_SPREAD_MOVING = 12.0;
    public static final double HANDCANNON_SPREAD_SPRINTING = 18.0;
    public static final double HANDCANNON_SPREAD_AIR = 18.0;
    // ★ 每颗弹丸额外散布（角度）
    public static final double HANDCANNON_PELLET_SPREAD = 2.0;

    // ===== 星野防御护盾 =====
    public static final int HOSHINO_NORMAL_SHIELD_DURABILITY = 150;
    public static final double HOSHINO_SHIELD_DAMAGE_REDUCTION = 0.85;
    public static final double HOSHINO_SHIELD_ABSORB_MULTIPLIER = 0.8;
    public static final double HOSHINO_SHIELD_SPEED_MULTIPLIER = 0.60;
    public static final int HOSHINO_ENHANCED_SHIELD_DURABILITY = 250;
    public static final double HOSHINO_ENHANCED_SHIELD_DAMAGE_REDUCTION = 0.75;
    public static final double HOSHINO_ENHANCED_SHIELD_ABSORB_MULTIPLIER = 0.65;
    public static final double HOSHINO_ENHANCED_SHIELD_SPEED_MULTIPLIER = 0.75;
}