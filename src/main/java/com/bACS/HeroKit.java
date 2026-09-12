package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class HeroKit {
    private final String id;
    private final String name;
    private final Component displayName;
    private final double maxHealth;
    private final int maxArmor;
    private final double armorDamageReduction;
    private final double armorIgnore;
    private final boolean canPenetrate;
    private final boolean canDamageTeammates;
    private final Material weaponMaterial;
    private final String weaponName;
    private final int chargeTime;
    private final double laserDamage;
    private final double laserArmorIgnore;
    private final boolean canPenetrateBlocks;
    private final double blockDamageReduction;
    private final List<String> description;
    private final int magazineSize;
    private final int maxMagazines;
    private final int initialReserveAmmo;   // ★ 新增：初始备用弹药（按发算）
    private final int maxReserveAmmo;       // ★ 新增：备用弹药上限
    private final int exSkillMaxCharge;
    private final Material meleeWeaponMaterial;
    private final String meleeWeaponName;
    private final double meleeDamage;
    private final double meleeCritDamage;
    private final double meleeBackstabDamage;
    private final double meleeBackstabCritDamage;
    private final double meleeAttackSpeed;
    private final double meleeSpeedBoost;
    private final double weaponSpeedReduction;

    private HeroKit(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.maxHealth = builder.maxHealth;
        this.maxArmor = builder.maxArmor;
        this.armorDamageReduction = builder.armorDamageReduction;
        this.armorIgnore = builder.armorIgnore;
        this.canPenetrate = builder.canPenetrate;
        this.canDamageTeammates = builder.canDamageTeammates;
        this.weaponMaterial = builder.weaponMaterial;
        this.weaponName = builder.weaponName;
        this.chargeTime = builder.chargeTime;
        this.laserDamage = builder.laserDamage;
        this.laserArmorIgnore = builder.laserArmorIgnore;
        this.canPenetrateBlocks = builder.canPenetrateBlocks;
        this.blockDamageReduction = builder.blockDamageReduction;
        this.description = builder.description;
        this.magazineSize = builder.magazineSize;
        this.maxMagazines = builder.maxMagazines;
        this.initialReserveAmmo = builder.initialReserveAmmo;
        this.maxReserveAmmo = builder.maxReserveAmmo;
        this.exSkillMaxCharge = builder.exSkillMaxCharge;
        this.meleeWeaponMaterial = builder.meleeWeaponMaterial;
        this.meleeWeaponName = builder.meleeWeaponName;
        this.meleeDamage = builder.meleeDamage;
        this.meleeCritDamage = builder.meleeCritDamage;
        this.meleeBackstabDamage = builder.meleeBackstabDamage;
        this.meleeBackstabCritDamage = builder.meleeBackstabCritDamage;
        this.meleeAttackSpeed = builder.meleeAttackSpeed;
        this.meleeSpeedBoost = builder.meleeSpeedBoost;
        this.weaponSpeedReduction = builder.weaponSpeedReduction;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public Component getDisplayName() { return displayName; }
    public double getMaxHealth() { return maxHealth; }
    public int getMaxArmor() { return maxArmor; }
    public double getArmorDamageReduction() { return armorDamageReduction; }
    public double getArmorIgnore() { return armorIgnore; }
    public boolean isCanPenetrate() { return canPenetrate; }
    public boolean isCanDamageTeammates() { return canDamageTeammates; }
    public Material getWeaponMaterial() { return weaponMaterial; }
    public String getWeaponName() { return weaponName; }
    public int getChargeTime() { return chargeTime; }
    public double getLaserDamage() { return laserDamage; }
    public double getLaserArmorIgnore() { return laserArmorIgnore; }
    public boolean isCanPenetrateBlocks() { return canPenetrateBlocks; }
    public double getBlockDamageReduction() { return blockDamageReduction; }
    public List<String> getDescription() { return description; }
    public int getMagazineSize() { return magazineSize; }
    public int getMaxMagazines() { return maxMagazines; }
    public int getInitialReserveAmmo() { return initialReserveAmmo; }
    public int getMaxReserveAmmo() { return maxReserveAmmo; }
    public int getExSkillMaxCharge() { return exSkillMaxCharge; }
    public Material getMeleeWeaponMaterial() { return meleeWeaponMaterial; }
    public String getMeleeWeaponName() { return meleeWeaponName; }
    public double getMeleeDamage() { return meleeDamage; }
    public double getMeleeCritDamage() { return meleeCritDamage; }
    public double getMeleeBackstabDamage() { return meleeBackstabDamage; }
    public double getMeleeBackstabCritDamage() { return meleeBackstabCritDamage; }
    public double getMeleeAttackSpeed() { return meleeAttackSpeed; }
    public double getMeleeSpeedBoost() { return meleeSpeedBoost; }
    public double getWeaponSpeedReduction() { return weaponSpeedReduction; }

    public ItemStack getMenuItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(displayName);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("❤ 生命值: " + maxHealth, NamedTextColor.RED));
        lore.add(Component.text("🛡 护甲值: " + maxArmor, NamedTextColor.AQUA));
        lore.add(Component.text("⚔ 主武器: " + weaponName, NamedTextColor.GOLD));
        lore.add(Component.text("🗡 近战武器: " + meleeWeaponName, NamedTextColor.GOLD));
        lore.add(Component.text(""));
        for (String desc : description) {
            lore.add(Component.text(desc, NamedTextColor.GRAY));
        }
        lore.add(Component.text(""));
        lore.add(Component.text("⚡ EX技能充能: " + exSkillMaxCharge + "点", NamedTextColor.GOLD));
        lore.add(Component.text("点击选择此英雄", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static class Builder {
        private String id;
        private String name;
        private Component displayName;
        private double maxHealth = 20;
        private int maxArmor = 100;
        private double armorDamageReduction = 0.5;
        private double armorIgnore = 0.0;
        private boolean canPenetrate = false;
        private boolean canDamageTeammates = false;
        private Material weaponMaterial = Material.DIAMOND_SWORD;
        private String weaponName = "武器";
        private int chargeTime = 1;
        private double laserDamage = 50;
        private double laserArmorIgnore = 0.0;
        private boolean canPenetrateBlocks = false;
        private double blockDamageReduction = 0.0;
        private List<String> description = new ArrayList<>();
        private int magazineSize = 5;
        private int maxMagazines = 5;
        private int initialReserveAmmo = 0;
        private int maxReserveAmmo = 0;
        private int exSkillMaxCharge = 10;
        private Material meleeWeaponMaterial = Material.WOODEN_SWORD;
        private String meleeWeaponName = "近战武器";
        private double meleeDamage = 10;
        private double meleeCritDamage = 15;
        private double meleeBackstabDamage = 50;
        private double meleeBackstabCritDamage = 75;
        private double meleeAttackSpeed = 1.0;
        private double meleeSpeedBoost = 0.0;
        private double weaponSpeedReduction = 0.0;


        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder displayName(Component displayName) { this.displayName = displayName; return this; }
        public Builder maxHealth(double maxHealth) { this.maxHealth = maxHealth; return this; }
        public Builder maxArmor(int maxArmor) { this.maxArmor = maxArmor; return this; }
        public Builder armorDamageReduction(double armorDamageReduction) { this.armorDamageReduction = armorDamageReduction; return this; }
        public Builder armorIgnore(double armorIgnore) { this.armorIgnore = armorIgnore; return this; }
        public Builder canPenetrate(boolean canPenetrate) { this.canPenetrate = canPenetrate; return this; }
        public Builder canDamageTeammates(boolean canDamageTeammates) { this.canDamageTeammates = canDamageTeammates; return this; }
        public Builder weaponMaterial(Material weaponMaterial) { this.weaponMaterial = weaponMaterial; return this; }
        public Builder weaponName(String weaponName) { this.weaponName = weaponName; return this; }
        public Builder chargeTime(int chargeTime) { this.chargeTime = chargeTime; return this; }
        public Builder laserDamage(double laserDamage) { this.laserDamage = laserDamage; return this; }
        public Builder laserArmorIgnore(double laserArmorIgnore) { this.laserArmorIgnore = laserArmorIgnore; return this; }
        public Builder canPenetrateBlocks(boolean canPenetrateBlocks) { this.canPenetrateBlocks = canPenetrateBlocks; return this; }
        public Builder blockDamageReduction(double blockDamageReduction) { this.blockDamageReduction = blockDamageReduction; return this; }
        public Builder description(String... description) { this.description = new ArrayList<>(List.of(description)); return this; }
        public Builder magazineSize(int magazineSize) { this.magazineSize = magazineSize; return this; }
        public Builder maxMagazines(int maxMagazines) { this.maxMagazines = maxMagazines; return this; }
        public Builder initialReserveAmmo(int initialReserveAmmo) { this.initialReserveAmmo = initialReserveAmmo; return this; }
        public Builder maxReserveAmmo(int maxReserveAmmo) { this.maxReserveAmmo = maxReserveAmmo; return this; }
        public Builder exSkillMaxCharge(int exSkillMaxCharge) { this.exSkillMaxCharge = exSkillMaxCharge; return this; }
        public Builder meleeWeaponMaterial(Material meleeWeaponMaterial) { this.meleeWeaponMaterial = meleeWeaponMaterial; return this; }
        public Builder meleeWeaponName(String meleeWeaponName) { this.meleeWeaponName = meleeWeaponName; return this; }
        public Builder meleeDamage(double meleeDamage) { this.meleeDamage = meleeDamage; return this; }
        public Builder meleeCritDamage(double meleeCritDamage) { this.meleeCritDamage = meleeCritDamage; return this; }
        public Builder meleeBackstabDamage(double meleeBackstabDamage) { this.meleeBackstabDamage = meleeBackstabDamage; return this; }
        public Builder meleeBackstabCritDamage(double meleeBackstabCritDamage) { this.meleeBackstabCritDamage = meleeBackstabCritDamage; return this; }
        public Builder meleeAttackSpeed(double meleeAttackSpeed) { this.meleeAttackSpeed = meleeAttackSpeed; return this; }
        public Builder meleeSpeedBoost(double meleeSpeedBoost) { this.meleeSpeedBoost = meleeSpeedBoost; return this; }
        public Builder weaponSpeedReduction(double weaponSpeedReduction) { this.weaponSpeedReduction = weaponSpeedReduction; return this; }

        public HeroKit build() {
            return new HeroKit(this);
        }
    }
}