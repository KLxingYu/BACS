package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SoulFireListener implements Listener {
    private final BACS plugin;
    private final GameManager gameManager;
    private final AmmoManager ammoManager;
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();
    private final Map<UUID, Boolean> isSneaking = new HashMap<>();
    private final Map<UUID, Long> weaponSwitchTime = new HashMap<>();
    private final Map<UUID, Boolean> isHoldingLeftClick = new HashMap<>();
    private final Map<UUID, Integer> shootingTaskIds = new HashMap<>();
    private final Map<UUID, Boolean> isHoldingRightClick = new HashMap<>();
    private final Map<UUID, Integer> rightClickShootTaskIds = new HashMap<>();
    private final Map<UUID, Long> lastRightClickTime = new HashMap<>();
    private final Map<UUID, Long> lastLeftClickTime = new HashMap<>();
    private final Random random = new Random();
    private static final long SWITCH_COOLDOWN = 1000;
    private static final long SHOT_INTERVAL = 116;

    // ===== AL1S 蓄力系统 =====
    private final Map<UUID, Integer> chargeTicks = new HashMap<>();
    private final Map<UUID, Long> lastChargeTime = new HashMap<>();
    private final Map<UUID, Boolean> isCharging = new HashMap<>();
    private final Map<UUID, Long> al1sCooldownEnd = new HashMap<>();
    private final Map<UUID, Boolean> chargeFullWarned = new HashMap<>();
    private static final int MAX_CHARGE_TICKS = 60;
    private static final double MAX_CHARGE_BONUS = 0.30;
    private static final long AL1S_COOLDOWN = 4500;

    // ===== 拉洛芙破晓连射系统 =====
    private final Map<UUID, Long> lastRakufuClickTime = new HashMap<>();
    private final Map<UUID, Boolean> rakufuAutoFire = new HashMap<>();
    private final Map<UUID, Integer> rakufuTaskId = new HashMap<>();

    public SoulFireListener(BACS plugin, GameManager gameManager, AmmoManager ammoManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.ammoManager = ammoManager;
        startAl1sSneakCheck();
    }

    // ============================================================
    // 碰撞箱判定工具
    // ============================================================

    private boolean segmentHitsPlayer(Location from, Location to, Player target) {
        Location feet = target.getLocation();
        double minX = feet.getX() - 0.3;
        double maxX = feet.getX() + 0.3;
        double minY = feet.getY();
        double maxY = feet.getY() + 1.8;
        double minZ = feet.getZ() - 0.3;
        double maxZ = feet.getZ() + 0.3;

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();

        double tMin = 0.0, tMax = 1.0;

        if (Math.abs(dx) < 1e-8) {
            if (from.getX() < minX || from.getX() > maxX) return false;
        } else {
            double t1 = (minX - from.getX()) / dx;
            double t2 = (maxX - from.getX()) / dx;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        if (Math.abs(dy) < 1e-8) {
            if (from.getY() < minY || from.getY() > maxY) return false;
        } else {
            double t1 = (minY - from.getY()) / dy;
            double t2 = (maxY - from.getY()) / dy;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        if (Math.abs(dz) < 1e-8) {
            if (from.getZ() < minZ || from.getZ() > maxZ) return false;
        } else {
            double t1 = (minZ - from.getZ()) / dz;
            double t2 = (maxZ - from.getZ()) / dz;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        return tMax >= 0 && tMin <= 1;
    }

    private Location getHitLocation(Location from, Location to, Player target) {
        Location feet = target.getLocation();
        Location center = feet.clone().add(0, 0.9, 0);

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double len2 = dx * dx + dy * dy + dz * dz;
        if (len2 < 1e-8) return center;

        double t = ((center.getX() - from.getX()) * dx +
                (center.getY() - from.getY()) * dy +
                (center.getZ() - from.getZ()) * dz) / len2;
        t = Math.max(0, Math.min(1, t));

        return new Location(from.getWorld(),
                from.getX() + dx * t,
                from.getY() + dy * t,
                from.getZ() + dz * t);
    }

    /**
     * 按相对脚底高度判定击中部位（宽松版：头部阈值 1.35）
     */
    private String getHitPartByRelativeY(Player target, Location hitLoc) {
        double relY = hitLoc.getY() - target.getLocation().getY();
        if (relY >= 1.35) return "HEAD";
        if (relY >= 0.85) return "CHEST";
        return "LIMB";
    }

    private boolean isBlockAtLocation(Location loc) {
        Material type = loc.getBlock().getType();
        if (type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR) return false;
        if (type == Material.WATER || type == Material.LAVA) return false;
        return type.isSolid();
    }

    // ============================================================
    // AL1S 蓄力
    // ============================================================

    public void startAl1sSneakCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    HeroKit kit = gameManager.getPlayerHero(player);
                    if (kit == null || !kit.getId().equals("al1s")) continue;
                    if (gameManager.getState() != GameState.ROUND_ACTIVE) continue;
                    if (!gameManager.isPlayerAlive(player)) continue;

                    UUID uuid = player.getUniqueId();
                    if (isCharging.getOrDefault(uuid, false)) continue;
                    if (!player.isSneaking()) continue;

                    tryStartAl1sCharge(player, kit);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void tryStartAl1sCharge(Player player, HeroKit kit) {
        UUID uuid = player.getUniqueId();

        if (isCharging.getOrDefault(uuid, false)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        boolean isHoldingWeapon = item != null && item.getType() == kit.getWeaponMaterial() &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains(kit.getWeaponName());
        if (!isHoldingWeapon) return;

        Long cooldownEnd = al1sCooldownEnd.get(uuid);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000 + 1;
            player.sendActionBar(Component.text("⏳ 冷却中: " + remaining + "s", NamedTextColor.RED));
            return;
        }

        if (ammoManager.isReloading(uuid)) {
            player.sendActionBar(Component.text("🔄 换弹中...", NamedTextColor.YELLOW));
            return;
        }

        if (ammoManager.getMagazineAmmo(uuid) <= 0) {
            player.sendMessage(Component.text("❌ 没有弹药！", NamedTextColor.RED));
            return;
        }

        isCharging.put(uuid, true);
        chargeTicks.put(uuid, 0);
        chargeFullWarned.put(uuid, false);
        lastChargeTime.put(uuid, System.currentTimeMillis());

        // 药水效果只在开始蓄力时给一次
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 24, 0, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 3, true, false));

        player.sendMessage(Component.text("⚡ 开始蓄力...", NamedTextColor.GOLD));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);

        startChargeTask(player, kit);
    }

    private void startChargeTask(Player player, HeroKit kit) {
        UUID uuid = player.getUniqueId();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isCharging.getOrDefault(uuid, false)) {
                    cancel();
                    return;
                }

                if (!player.isOnline() || !gameManager.isPlayerAlive(player)) {
                    isCharging.put(uuid, false);
                    chargeTicks.put(uuid, 0);
                    chargeFullWarned.put(uuid, false);
                    cancel();
                    return;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                boolean isHoldingWeapon = item != null && item.getType() == kit.getWeaponMaterial() &&
                        item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                        item.getItemMeta().displayName().toString().contains(kit.getWeaponName());

                if (!isHoldingWeapon) {
                    isCharging.put(uuid, false);
                    chargeTicks.put(uuid, 0);
                    chargeFullWarned.put(uuid, false);
                    cancel();
                    return;
                }

                int ticks = chargeTicks.getOrDefault(uuid, 0);

                if (ticks < MAX_CHARGE_TICKS) {
                    ticks++;
                    chargeTicks.put(uuid, ticks);

                    if (ticks % 20 == 0) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f + (ticks / 20) * 0.15f);
                    }

                    if (ticks >= MAX_CHARGE_TICKS && !chargeFullWarned.getOrDefault(uuid, false)) {
                        chargeFullWarned.put(uuid, true);
                        player.sendMessage(Component.text("⚡ 蓄力已满！站起来发射", NamedTextColor.GOLD));
                        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void releaseAl1sCharge(Player player, HeroKit kit) {
        UUID uuid = player.getUniqueId();

        if (!isCharging.getOrDefault(uuid, false)) return;

        isCharging.put(uuid, false);
        int ticks = chargeTicks.getOrDefault(uuid, 0);
        chargeTicks.put(uuid, 0);
        chargeFullWarned.put(uuid, false);

        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.BLINDNESS);

        if (ammoManager.getMagazineAmmo(uuid) > 0 && !ammoManager.isReloading(uuid)) {
            ammoManager.consumeBullet(uuid);
            ammoManager.setLastShotTime(uuid, System.currentTimeMillis());

            double bonus = Math.min((ticks / 20.0) * 0.02, MAX_CHARGE_BONUS);

            // ★ 新伤害：头 400 / 胸 275 / 四肢 250
            double baseDamage = 275 + 275 * bonus;
            double headDamage = 400 + 400 * bonus;
            double limbDamage = 250 + 250 * bonus;

            shootAl1sLaser(player, kit, bonus, baseDamage, headDamage, limbDamage);
            updateAmmoDisplay(player, kit);

            al1sCooldownEnd.put(uuid, System.currentTimeMillis() + AL1S_COOLDOWN);
            player.sendMessage(Component.text("💥 蓄力发射！伤害加成: " + (int)(bonus * 100) + "%", NamedTextColor.GOLD));
        } else {
            player.sendMessage(Component.text("❌ 没有弹药！", NamedTextColor.RED));
        }
    }

    // ============================================================
    // 交互入口
    // ============================================================

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;
        if (gameManager.getState() != GameState.ROUND_ACTIVE) return;
        if (!gameManager.isPlayerAlive(player)) return;

        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null) return;

        UUID uuid = player.getUniqueId();

        String equippedId = gameManager.getShopManager().getEquippedMainWeapon(player);
        HeroKit equippedKit = null;
        if (equippedId != null) {
            equippedKit = gameManager.getHeroKitById(equippedId);
        }

        boolean isMainWeapon = false;
        HeroKit activeKit = null;

        if (equippedKit != null && equippedKit.getWeaponMaterial() != Material.AIR &&
                item.getType() == equippedKit.getWeaponMaterial() &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains(equippedKit.getWeaponName())) {
            isMainWeapon = true;
            activeKit = equippedKit;
        }

        if (!isMainWeapon && kit != null && kit.getWeaponMaterial() != Material.AIR &&
                item.getType() == kit.getWeaponMaterial() &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains(kit.getWeaponName())) {
            isMainWeapon = true;
            activeKit = kit;
        }

        if (!isMainWeapon || activeKit == null) return;

        // ===== 泉：右键长按全自动 =====
        if (activeKit.getId().equals("izumi") && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            startRightClickShooting(player, activeKit);
            return;
        }

        // ===== 芹奈：神名霰弹右键射击 =====
        if (activeKit.getId().equals("serina")) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                if (ammoManager.isReloading(uuid)) {
                    player.sendActionBar(Component.text("🔄 换弹中...", NamedTextColor.YELLOW));
                    return;
                }
                int ammo = ammoManager.getMagazineAmmo(uuid);
                if (ammo <= 0) {
                    player.sendMessage(Component.text("❌ 没有弹药！", NamedTextColor.RED));
                    reloadWeapon(player, activeKit);
                    return;
                }
                ammoManager.consumeBullet(uuid);
                ammoManager.setLastShotTime(uuid, System.currentTimeMillis());
                shootSerinaShotgun(player, activeKit);
                updateAmmoDisplay(player, activeKit);
                return;
            }
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
                reloadWeapon(player, activeKit);
                return;
            }
        }

        // ===== 未花：右键单发/长按全自动 =====
        if (activeKit.getId().equals("mika")) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                MikaSkillListener mikaListener = BACS.getInstance().getMikaSkillListener();
                if (mikaListener != null && mikaListener.isExPrepared(player)) {
                    mikaListener.fireDragonBreath(player);
                } else {
                    startRightClickShooting(player, activeKit);
                }
                return;
            }
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                if (!player.isSneaking()) {
                    event.setCancelled(true);
                    reloadWeapon(player, activeKit);
                    return;
                }
            }
        }

        // ===== 黑子：右键全自动，左键换弹 =====
        if (activeKit.getId().equals("kuroko")) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                startRightClickShooting(player, activeKit);
                return;
            }
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
                reloadWeapon(player, activeKit);
                return;
            }
        }

        // ===== 优香：右键全自动 =====
        if (activeKit.getId().equals("yuuka") && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            startRightClickShooting(player, activeKit);
            return;
        }

        // ===== AL1S 独立处理 =====
        if (activeKit.getId().equals("al1s")) {
            boolean holdingMainWeapon = item.getType() == activeKit.getWeaponMaterial() &&
                    item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().displayName().toString().contains(activeKit.getWeaponName());

            if (holdingMainWeapon && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                event.setCancelled(true);
                if (isCharging.getOrDefault(uuid, false)) {
                    releaseAl1sCharge(player, activeKit);
                } else {
                    handleAl1sRightClick(player, activeKit);
                }
                return;
            }

            if (holdingMainWeapon && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
                event.setCancelled(true);
                if (player.isSneaking()) {
                    return;
                }
                handleAl1sLeftClick(player, activeKit);
                return;
            }
        }

        // ===== 拉洛芙：破晓 =====
        if (activeKit.getId().equals("rakufu")) {
            boolean holdingMainWeapon = item.getType() == activeKit.getWeaponMaterial() &&
                    item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().displayName().toString().contains(activeKit.getWeaponName());

            if (holdingMainWeapon && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                event.setCancelled(true);
                handleRakufuRightClick(player, activeKit);
                return;
            }
            if (holdingMainWeapon && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
                event.setCancelled(true);
                reloadWeapon(player, activeKit);
                return;
            }
        }

        // ===== 通用：左键射击 =====
        long switchCooldown = activeKit.getId().equals("kayi") ? 1750 :
                activeKit.getId().equals("izumi") ? 800 :
                        activeKit.getId().equals("mika") ? 800 :
                                activeKit.getId().equals("yuuka") ? 800 :
                                        activeKit.getId().equals("serina") ? 500 :
                                                activeKit.getId().equals("rakufu") ? 500 : SWITCH_COOLDOWN;
        Long switchTime = weaponSwitchTime.get(uuid);
        if (switchTime != null && System.currentTimeMillis() - switchTime < switchCooldown) {
            player.sendActionBar(Component.text("⏳ 武器切换冷却中...", NamedTextColor.RED));
            return;
        }

        if (activeKit.getId().equals("izumi") && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
            event.setCancelled(true);
            reloadWeapon(player, activeKit);
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);

            if (activeKit.getId().equals("mika") && player.isSneaking()) {
                MikaSkillListener mikaListener = BACS.getInstance().getMikaSkillListener();
                if (mikaListener != null) {
                    mikaListener.triggerSmallSkill(player);
                }
                return;
            }
            if (activeKit.getId().equals("yuuka") && player.isSneaking()) {
                YuukaSkillListener yuukaListener = BACS.getInstance().getYuukaSkillListener();
                if (yuukaListener != null) {
                    yuukaListener.triggerEX(player);
                }
                return;
            }

            if (activeKit.getId().equals("kayi")) {
                shootOnceKayi(player, activeKit);
            } else if (activeKit.getId().equals("al1s")) {
                // AL1S左键由上面独立处理
            } else if (activeKit.getId().equals("rakufu")) {
                // 拉洛芙左键换弹已处理
            } else {
                startLeftClickShooting(player, activeKit);
            }
        }

        if (!activeKit.getId().equals("izumi") && !activeKit.getId().equals("mika") &&
                !activeKit.getId().equals("yuuka") && !activeKit.getId().equals("kuroko") &&
                !activeKit.getId().equals("serina") && !activeKit.getId().equals("al1s") &&
                !activeKit.getId().equals("rakufu") &&
                (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (player.isSneaking()) {
                event.setCancelled(true);
                gameManager.getExSkillManager().activateEX(player);
            }
        }
    }

    // ============================================================
    // AL1S 单发
    // ============================================================

    private void handleAl1sRightClick(Player player, HeroKit kit) {
        UUID uuid = player.getUniqueId();

        Long cooldownEnd = al1sCooldownEnd.get(uuid);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000 + 1;
            player.sendActionBar(Component.text("⏳ 冷却中: " + remaining + "s", NamedTextColor.RED));
            return;
        }

        if (ammoManager.isReloading(uuid)) {
            player.sendActionBar(Component.text("🔄 换弹中...", NamedTextColor.YELLOW));
            return;
        }

        if (ammoManager.getMagazineAmmo(uuid) <= 0) {
            player.sendMessage(Component.text("❌ 没有弹药！", NamedTextColor.RED));
            reloadWeapon(player, kit);
            return;
        }

        ammoManager.consumeBullet(uuid);
        ammoManager.setLastShotTime(uuid, System.currentTimeMillis());
        // ★ 新伤害：头 400 / 胸 275 / 四肢 250
        shootAl1sLaser(player, kit, 0, 275, 400, 250);
        updateAmmoDisplay(player, kit);

        al1sCooldownEnd.put(uuid, System.currentTimeMillis() + AL1S_COOLDOWN);
    }

    private void handleAl1sLeftClick(Player player, HeroKit kit) {
        UUID uuid = player.getUniqueId();

        if (isCharging.getOrDefault(uuid, false)) return;

        Long cooldownEnd = al1sCooldownEnd.get(uuid);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000 + 1;
            player.sendActionBar(Component.text("⏳ 冷却中: " + remaining + "s", NamedTextColor.RED));
            return;
        }

        if (ammoManager.isReloading(uuid)) return;
        if (ammoManager.getMagazineAmmo(uuid) <= 0) {
            player.sendMessage(Component.text("❌ 没有弹药！", NamedTextColor.RED));
            reloadWeapon(player, kit);
            return;
        }

        ammoManager.consumeBullet(uuid);
        ammoManager.setLastShotTime(uuid, System.currentTimeMillis());
        // ★ 新伤害：头 400 / 胸 275 / 四肢 250
        shootAl1sLaser(player, kit, 0, 275, 400, 250);
        updateAmmoDisplay(player, kit);

        al1sCooldownEnd.put(uuid, System.currentTimeMillis() + AL1S_COOLDOWN);
    }

    private void shootAl1sLaser(Player player, HeroKit kit, double bonus,
                                double baseDamage, double headDamage, double limbDamage) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().clone();

        double rangeMultiplier = 1.0 + bonus;
        double speed = 6.0 * rangeMultiplier;
        int maxPenetration = 1;

        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 2.0f);
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 0.8f, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.6f, 1.0f);

        new BukkitRunnable() {
            Location currentLoc = eyeLoc.clone();
            Location prevLoc = eyeLoc.clone();
            Vector velocity = direction.clone().multiply(speed);
            int ticks = 0;
            int penetrated = 0;
            boolean hasHit = false;

            @Override
            public void run() {
                if (hasHit) { cancel(); return; }
                ticks++;
                if (ticks > 50) { cancel(); return; }

                prevLoc = currentLoc.clone();
                currentLoc.add(velocity);

                currentLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, currentLoc, 3, 0.1, 0.1, 0.1, 0.01);

                if (isBlockAtLocation(currentLoc)) {
                    if (penetrated < maxPenetration) {
                        penetrated++;
                    } else {
                        cancel();
                        return;
                    }
                }

                for (Player target : currentLoc.getWorld().getPlayers()) {
                    if (target == player || !target.isOnline()) continue;
                    if (!gameManager.isPlayerAlive(target)) continue;
                    String st = gameManager.getPlayerTeam(player);
                    String tt = gameManager.getPlayerTeam(target);
                    if (st != null && tt != null && st.equals(tt) && !kit.isCanDamageTeammates()) continue;

                    if (segmentHitsPlayer(prevLoc, currentLoc, target)) {
                        hasHit = true;
                        Location hitLoc = getHitLocation(prevLoc, currentLoc, target);
                        String part = getHitPartByRelativeY(target, hitLoc);
                        double damage;
                        if (part.equals("HEAD")) {
                            damage = headDamage;
                            target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
                        } else if (part.equals("CHEST")) {
                            damage = baseDamage;
                        } else {
                            damage = limbDamage;
                        }
                        if (penetrated > 0) {
                            damage *= 0.6;
                        }
                        spawnBloodEffect(target);
                        target.damage(damage, player);
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ============================================================
    // 拉洛芙破晓
    // ============================================================

    private void handleRakufuRightClick(Player player, HeroKit kit) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastClick = lastRakufuClickTime.get(uuid);

        boolean isAutoFire = false;
        if (lastClick != null && now - lastClick < 200) {
            isAutoFire = true;
        }
        lastRakufuClickTime.put(uuid, now);

        if (rakufuAutoFire.getOrDefault(uuid, false)) {
            stopRakufuShooting(player);
            startRakufuAutoFire(player, kit);
            return;
        }

        if (isAutoFire) {
            startRakufuAutoFire(player, kit);
        } else {
            if (ammoManager.isReloading(uuid)) {
                player.sendActionBar(Component.text("🔄 换弹中...", NamedTextColor.YELLOW));
                return;
            }
            if (ammoManager.getMagazineAmmo(uuid) <= 0) {
                player.sendMessage(Component.text("❌ 没有弹药！", NamedTextColor.RED));
                reloadWeapon(player, kit);
                return;
            }
            shootRakufu(player, kit, player.isSneaking());
            updateAmmoDisplay(player, kit);
        }
    }

    private void startRakufuAutoFire(Player player, HeroKit kit) {
        UUID uuid = player.getUniqueId();
        if (rakufuAutoFire.getOrDefault(uuid, false)) return;
        rakufuAutoFire.put(uuid, true);

        if (ammoManager.isReloading(uuid)) {
            player.sendActionBar(Component.text("🔄 换弹中...", NamedTextColor.YELLOW));
            rakufuAutoFire.put(uuid, false);
            return;
        }
        if (ammoManager.getMagazineAmmo(uuid) <= 0) {
            player.sendMessage(Component.text("❌ 没有弹药！", NamedTextColor.RED));
            reloadWeapon(player, kit);
            rakufuAutoFire.put(uuid, false);
            return;
        }
        shootRakufu(player, kit, player.isSneaking());
        updateAmmoDisplay(player, kit);

        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !gameManager.isPlayerAlive(player)) {
                    stopRakufuShooting(player);
                    cancel();
                    return;
                }
                if (gameManager.getState() != GameState.ROUND_ACTIVE) {
                    stopRakufuShooting(player);
                    cancel();
                    return;
                }
                Long lastClick = lastRakufuClickTime.get(uuid);
                if (lastClick == null || System.currentTimeMillis() - lastClick > 300) {
                    stopRakufuShooting(player);
                    cancel();
                    return;
                }
                if (ammoManager.isReloading(uuid)) {
                    player.sendActionBar(Component.text("🔄 换弹中...", NamedTextColor.YELLOW));
                    return;
                }
                if (ammoManager.getMagazineAmmo(uuid) <= 0) {
                    player.sendMessage(Component.text("❌ 没有弹药！", NamedTextColor.RED));
                    reloadWeapon(player, kit);
                    stopRakufuShooting(player);
                    cancel();
                    return;
                }
                boolean sneaking = player.isSneaking();
                if (sneaking) {
                    player.setWalkSpeed(0f);
                    player.setFlySpeed(0f);
                }
                shootRakufu(player, kit, sneaking);
                updateAmmoDisplay(player, kit);
            }
        }.runTaskTimer(plugin, 2L, 2L).getTaskId();

        rakufuTaskId.put(uuid, taskId);
    }

    private void stopRakufuShooting(Player player) {
        UUID uuid = player.getUniqueId();
        rakufuAutoFire.put(uuid, false);
        Integer task = rakufuTaskId.remove(uuid);
        if (task != null) {
            Bukkit.getScheduler().cancelTask(task);
        }
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
    }

    private void shootRakufu(Player player, HeroKit kit, boolean isSneaking) {
        UUID uuid = player.getUniqueId();
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().clone();

        double spread = 0.0;
        if (!player.isOnGround()) {
            spread = 4.0;
        } else if (player.isSprinting()) {
            spread = 3.0;
        } else if (player.getVelocity().lengthSquared() > 0.01) {
            spread = 1.5;
        }
        if (isSneaking) spread = 0.0;

        if (spread > 0) {
            direction.add(new Vector(
                    (Math.random() - 0.5) * spread,
                    (Math.random() - 0.5) * spread,
                    (Math.random() - 0.5) * spread
            )).normalize();
        }

        double baseDamage = 22;
        double headDamage = 52;
        double limbDamage = 18;

        if (isSneaking) {
            baseDamage *= 1.3;
            headDamage *= 1.3;
            limbDamage *= 1.3;
        }

        final double fBaseDamage = baseDamage;
        final double fHeadDamage = headDamage;
        final double fLimbDamage = limbDamage;

        int maxPenetration = 1;
        double penetrationDamage = isSneaking ? 0.75 : 0.6;
        final double fPenetrationDamage = penetrationDamage;

        double speed = 5.0;

        ammoManager.consumeBullet(uuid);
        ammoManager.setLastShotTime(uuid, System.currentTimeMillis());

        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.3f, 1.2f);

        new BukkitRunnable() {
            Location currentLoc = eyeLoc.clone();
            Location prevLoc = eyeLoc.clone();
            Vector velocity = direction.clone().multiply(speed);
            int ticks = 0;
            int penetrated = 0;
            boolean hasHit = false;

            @Override
            public void run() {
                if (hasHit) { cancel(); return; }
                ticks++;
                if (ticks > 50) { cancel(); return; }

                prevLoc = currentLoc.clone();
                currentLoc.add(velocity);

                currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 1, 0.05, 0.05, 0.05, 0.01);

                if (isBlockAtLocation(currentLoc)) {
                    if (penetrated < maxPenetration) {
                        penetrated++;
                    } else {
                        cancel();
                        return;
                    }
                }

                for (Player target : currentLoc.getWorld().getPlayers()) {
                    if (target == player || !target.isOnline()) continue;
                    if (!gameManager.isPlayerAlive(target)) continue;
                    String st = gameManager.getPlayerTeam(player);
                    String tt = gameManager.getPlayerTeam(target);
                    if (st != null && tt != null && st.equals(tt)) continue;

                    if (segmentHitsPlayer(prevLoc, currentLoc, target)) {
                        hasHit = true;
                        Location hitLoc = getHitLocation(prevLoc, currentLoc, target);
                        String part = getHitPartByRelativeY(target, hitLoc);
                        double finalDamage;
                        if (part.equals("HEAD")) {
                            finalDamage = fHeadDamage;
                            target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
                        } else if (part.equals("CHEST")) {
                            finalDamage = fBaseDamage;
                        } else {
                            finalDamage = fLimbDamage;
                        }
                        if (penetrated > 0) {
                            finalDamage *= fPenetrationDamage;
                        }
                        target.damage(finalDamage, player);
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ============================================================
    // 左键连射
    // ============================================================

    private void startLeftClickShooting(Player player, HeroKit kit) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        lastLeftClickTime.put(uuid, now);

        shootOnce(player, kit);

        if (isHoldingLeftClick.getOrDefault(uuid, false)) return;

        isHoldingLeftClick.put(uuid, true);

        int taskId = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                Long lastClick = lastLeftClickTime.get(uuid);
                if (lastClick == null || System.currentTimeMillis() - lastClick > 250) {
                    stopShooting(player);
                    cancel();
                    return;
                }

                if (ticks < 4) return;

                if (!player.isOnline()) {
                    stopShooting(player);
                    cancel();
                    return;
                }

                ItemStack currentItem = player.getInventory().getItemInMainHand();
                if (currentItem == null || currentItem.getType() != kit.getWeaponMaterial()) {
                    stopShooting(player);
                    cancel();
                    return;
                }

                if (gameManager.getState() != GameState.ROUND_ACTIVE || !gameManager.isPlayerAlive(player)) {
                    stopShooting(player);
                    cancel();
                    return;
                }

                shootOnce(player, kit);
            }
        }.runTaskTimer(plugin, 2L, 2L).getTaskId();

        shootingTaskIds.put(uuid, taskId);
    }

    private void shootOnceKayi(Player player, HeroKit kit) {
        UUID uuid = player.getUniqueId();
        long cooldown = 600;
        if (!ammoManager.canShoot(uuid, cooldown)) {
            long remaining = ammoManager.getCooldownRemaining(uuid, cooldown) / 1000 + 1;
            player.sendActionBar(Component.text("⏳ 冷却中: " + remaining + "s", NamedTextColor.RED));
            return;
        }
        if (ammoManager.isReloading(uuid)) return;
        if (ammoManager.getMagazineAmmo(uuid) <= 0) {
            player.sendMessage(Component.text("❌ 没有弹药！", NamedTextColor.RED));
            reloadWeapon(player, kit);
            return;
        }
        ammoManager.consumeBullet(uuid);
        ammoManager.setLastShotTime(uuid, System.currentTimeMillis());
        shootLaser(player, kit);
        updateAmmoDisplay(player, kit);
        player.setCooldown(kit.getWeaponMaterial(), (int)(cooldown / 50));
    }

    private void shootOnce(Player player, HeroKit kit) {
        UUID uuid = player.getUniqueId();
        long cooldown = kit.getId().equals("kuroko") ? SHOT_INTERVAL :
                kit.getId().equals("izumi") ? 98 :
                        kit.getId().equals("mika") ? 106 :
                                kit.getId().equals("yuuka") ? 87 :
                                        2500;
        if (!ammoManager.canShoot(uuid, cooldown)) return;
        if (ammoManager.isReloading(uuid)) return;
        if (ammoManager.getMagazineAmmo(uuid) <= 0) {
            player.sendMessage(Component.text("❌ 没有弹药！", NamedTextColor.RED));
            reloadWeapon(player, kit);
            stopShooting(player);
            return;
        }
        ammoManager.consumeBullet(uuid);
        ammoManager.setLastShotTime(uuid, System.currentTimeMillis());

        shootLaser(player, kit);
        updateAmmoDisplay(player, kit);
        player.setCooldown(kit.getWeaponMaterial(), (int)(cooldown / 50));
    }

    private void stopShooting(Player player) {
        UUID uuid = player.getUniqueId();
        isHoldingLeftClick.put(uuid, false);
        Integer task = shootingTaskIds.remove(uuid);
        if (task != null) Bukkit.getScheduler().cancelTask(task);
    }

    // ============================================================
    // 右键连射
    // ============================================================

    private void startRightClickShooting(Player player, HeroKit kit) {
        UUID uuid = player.getUniqueId();
        lastRightClickTime.put(uuid, System.currentTimeMillis());
        if (isHoldingRightClick.getOrDefault(uuid, false)) return;
        isHoldingRightClick.put(uuid, true);
        shootOnce(player, kit);
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { stopRightClickShooting(player); cancel(); return; }
                if (System.currentTimeMillis() - lastRightClickTime.getOrDefault(uuid, 0L) > 150) {
                    stopRightClickShooting(player);
                    cancel();
                    return;
                }
                if (gameManager.getState() != GameState.ROUND_ACTIVE || !gameManager.isPlayerAlive(player)) {
                    stopRightClickShooting(player);
                    cancel();
                    return;
                }
                shootOnce(player, kit);
            }
        }.runTaskTimer(plugin, 2L, 2L).getTaskId();
        rightClickShootTaskIds.put(uuid, taskId);
    }

    private void stopRightClickShooting(Player player) {
        UUID uuid = player.getUniqueId();
        isHoldingRightClick.put(uuid, false);
        Integer task = rightClickShootTaskIds.remove(uuid);
        if (task != null) Bukkit.getScheduler().cancelTask(task);
    }

    // ============================================================
    // 换弹（新模型：从备用弹药扣实际补充量）
    // ============================================================

    private void reloadWeapon(Player player, HeroKit kit) {
        UUID uuid = player.getUniqueId();

        // 拉洛芙破晓
        if (kit.getId().equals("rakufu")) {
            int currentAmmo = ammoManager.getMagazineAmmo(uuid);
            int maxAmmo = 50;
            if (currentAmmo >= maxAmmo) {
                player.sendMessage(Component.text("子弹已满！", NamedTextColor.YELLOW));
                return;
            }
            if (ammoManager.isReloading(uuid)) return;

            int need = maxAmmo - currentAmmo;
            int reserve = ammoManager.getReserveAmmo(uuid);
            if (reserve <= 0) {
                player.sendMessage(Component.text("❌ 没有备用弹药！", NamedTextColor.RED));
                return;
            }
            int actual = Math.min(need, reserve);

            ammoManager.setReloading(uuid, true);
            player.sendMessage(Component.text("🔄 换弹中...", NamedTextColor.YELLOW));

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        ammoManager.setReloading(uuid, false);
                        cancel();
                        return;
                    }
                    ammoManager.consumeReserveAmmo(uuid, actual);
                    ammoManager.setMagazineAmmo(uuid, currentAmmo + actual);
                    ammoManager.setReloading(uuid, false);
                    player.sendMessage(Component.text("✅ 换弹完成！+" + actual + "发", NamedTextColor.GREEN));
                    updateAmmoDisplay(player, kit);
                }
            }.runTaskLater(plugin, 40L);
            return;
        }

        // 芹奈神名
        if (kit.getId().equals("serina")) {
            int currentAmmo = ammoManager.getMagazineAmmo(uuid);
            int maxAmmo = 6;
            if (currentAmmo >= maxAmmo) {
                player.sendMessage(Component.text("子弹已满！", NamedTextColor.YELLOW));
                return;
            }
            if (ammoManager.isReloading(uuid)) return;

            int need = maxAmmo - currentAmmo;
            int reserve = ammoManager.getReserveAmmo(uuid);
            if (reserve <= 0) {
                player.sendMessage(Component.text("❌ 没有备用弹药！", NamedTextColor.RED));
                return;
            }
            int actual = Math.min(need, reserve);

            ammoManager.setReloading(uuid, true);
            player.sendMessage(Component.text("🔄 换弹中...", NamedTextColor.YELLOW));

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        ammoManager.setReloading(uuid, false);
                        cancel();
                        return;
                    }
                    ammoManager.consumeReserveAmmo(uuid, actual);
                    ammoManager.setMagazineAmmo(uuid, currentAmmo + actual);
                    ammoManager.setReloading(uuid, false);
                    player.sendMessage(Component.text("✅ 换弹完成！+" + actual + "发", NamedTextColor.GREEN));
                    updateAmmoDisplay(player, kit);
                }
            }.runTaskLater(plugin, 30L);
            return;
        }

        // 其他英雄
        int currentAmmo = ammoManager.getMagazineAmmo(uuid);
        int maxAmmo = kit.getMagazineSize();
        if (currentAmmo >= maxAmmo) {
            player.sendMessage(Component.text("子弹已满！", NamedTextColor.YELLOW));
            return;
        }

        int need = maxAmmo - currentAmmo;
        int reserve = ammoManager.getReserveAmmo(uuid);
        if (reserve <= 0) {
            player.sendMessage(Component.text("❌ 没有备用弹药！", NamedTextColor.RED));
            return;
        }
        int actual = Math.min(need, reserve);

        ammoManager.setReloading(uuid, true);
        player.sendMessage(Component.text("🔄 换弹中...", NamedTextColor.YELLOW));
        long reloadTicks = kit.getId().equals("izumi") ? 35 : kit.getId().equals("mika") ? 30 : kit.getId().equals("yuuka") ? 30 : 40;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    ammoManager.setReloading(uuid, false);
                    cancel();
                    return;
                }
                ammoManager.consumeReserveAmmo(uuid, actual);
                ammoManager.setMagazineAmmo(uuid, currentAmmo + actual);
                ammoManager.setReloading(uuid, false);
                player.sendMessage(Component.text("✅ 换弹完成！+" + actual + "发", NamedTextColor.GREEN));
                updateAmmoDisplay(player, kit);
            }
        }.runTaskLater(plugin, reloadTicks);
    }

    // ============================================================
    // 神名霰弹
    // ============================================================

    private void shootSerinaShotgun(Player player, HeroKit kit) {
        XYAntiCheat antiCheat = plugin.getXYAntiCheat();
        if (antiCheat != null) {
            antiCheat.exemptSerinaAttack(player);
        }

        Location eyeLoc = player.getEyeLocation();
        Vector baseDirection = eyeLoc.getDirection().clone();

        int pellets = 15;
        double maxRange = 25.0;
        double baseDamageChest = 13.0;
        double baseDamageLimb = 9.0;
        double baseDamageHead = 19.0;
        int maxPenetration = 1;

        double velocity = player.getVelocity().length();
        double spread;
        if (velocity < 0.5) {
            spread = 0.15 + Math.random() * 0.15;
        } else if (velocity < 1.0) {
            spread = 0.2 + Math.random() * 0.2;
        } else {
            spread = 0.3 + Math.random() * 0.3;
        }
        if (player.isSneaking()) spread *= 0.7;
        if (!player.isOnGround()) spread *= 1.5;

        for (int i = 0; i < pellets; i++) {
            Vector direction = baseDirection.clone();
            direction.add(new Vector(
                    (Math.random() - 0.5) * spread,
                    (Math.random() - 0.5) * spread,
                    (Math.random() - 0.5) * spread
            )).normalize();
            shootSerinaPellet(player, direction, baseDamageChest, baseDamageLimb, baseDamageHead, maxRange, maxPenetration);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.3f, 1.2f);
    }

    private void shootSerinaPellet(Player player, Vector direction,
                                   double baseDamageChest, double baseDamageLimb, double baseDamageHead,
                                   double maxRange, int maxPenetration) {
        Location eyeLoc = player.getEyeLocation();
        double speed = 4.0;

        new BukkitRunnable() {
            Location currentLoc = eyeLoc.clone();
            Location prevLoc = eyeLoc.clone();
            Vector velocity = direction.clone().multiply(speed);
            double traveled = 0;
            int penetratedBlocks = 0;
            boolean hasHit = false;

            @Override
            public void run() {
                if (hasHit) { cancel(); return; }

                traveled += velocity.length();
                prevLoc = currentLoc.clone();
                currentLoc.add(velocity);
                if (traveled > maxRange) { cancel(); return; }

                currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 2, 0.05, 0.05, 0.05, 0.01);
                currentLoc.getWorld().spawnParticle(Particle.GLOW, currentLoc, 1, 0.03, 0.03, 0.03, 0.01);

                if (isBlockAtLocation(currentLoc)) {
                    if (penetratedBlocks < maxPenetration) {
                        penetratedBlocks++;
                        currentLoc.getWorld().spawnParticle(Particle.BLOCK, currentLoc, 5, 0.2, 0.2, 0.2, 0,
                                currentLoc.getBlock().getBlockData());
                    } else {
                        cancel();
                        return;
                    }
                }

                for (Player target : currentLoc.getWorld().getPlayers()) {
                    if (target == player || !target.isOnline()) continue;
                    if (!gameManager.isPlayerAlive(target)) continue;

                    String shooterTeam = gameManager.getPlayerTeam(player);
                    String targetTeam = gameManager.getPlayerTeam(target);
                    if (shooterTeam != null && targetTeam != null && shooterTeam.equals(targetTeam)) continue;

                    if (segmentHitsPlayer(prevLoc, currentLoc, target)) {
                        hasHit = true;
                        Location hitLoc = getHitLocation(prevLoc, currentLoc, target);
                        String part = getHitPartByRelativeY(target, hitLoc);
                        double damage;
                        if (part.equals("HEAD")) {
                            damage = baseDamageHead;
                            target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
                        } else if (part.equals("CHEST")) {
                            damage = baseDamageChest;
                        } else {
                            damage = baseDamageLimb;
                        }

                        if (traveled > 8 && traveled <= 12) {
                            damage *= 0.7;
                        } else if (traveled > 12) {
                            damage *= 0.45;
                        }

                        if (penetratedBlocks > 0) damage *= 0.3;

                        target.damage(damage, player);
                        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                                target.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.1);
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ============================================================
    // 主武器激光
    // ============================================================

    private void shootLaser(Player player, HeroKit kit) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().clone();
        UUID uuid = player.getUniqueId();

        double playerSpeed = player.getVelocity().length();
        if ((kit.getId().equals("al1s") || kit.getId().equals("kayi")) && playerSpeed >= 1.5) {
            double spreadAmount = 2.0;
            direction.add(new Vector(
                    (random.nextDouble() - 0.5) * spreadAmount,
                    (random.nextDouble() - 0.5) * spreadAmount,
                    (random.nextDouble() - 0.5) * spreadAmount
            )).normalize();
        }

        double spread = calculateSpread(player, kit);
        if (spread > 0) {
            direction.add(new Vector(
                    (Math.random() - 0.5) * spread,
                    (Math.random() - 0.5) * spread,
                    (Math.random() - 0.5) * spread
            )).normalize();
        }

        String heroId = kit.getId();
        double speed = heroId.equals("al1s") ? 6.0 : 5.0;

        int maxPenetration = 1;

        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 2.0f);

        new BukkitRunnable() {
            Location currentLoc = eyeLoc.clone();
            Location prevLoc = eyeLoc.clone();
            Vector velocity = direction.clone().multiply(speed);
            int ticks = 0;
            int penetrated = 0;
            boolean hasHit = false;

            @Override
            public void run() {
                if (hasHit) { cancel(); return; }
                ticks++;
                if (ticks > 50) { cancel(); return; }

                prevLoc = currentLoc.clone();
                currentLoc.add(velocity);

                if (heroId.equals("izumi")) {
                    currentLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, currentLoc, 2, 0.05, 0.05, 0.05, 0.01);
                    currentLoc.getWorld().spawnParticle(Particle.GLOW, currentLoc, 1, 0.05, 0.05, 0.05, 0.01);
                } else if (heroId.equals("kuroko")) {
                    currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 3, 0.1, 0.1, 0.1, 0.01);
                } else if (heroId.equals("al1s")) {
                    currentLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, currentLoc, 3, 0.1, 0.1, 0.1, 0.01);
                } else if (heroId.equals("mika")) {
                    currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 3, 0.1, 0.1, 0.1, 0.01);
                    currentLoc.getWorld().spawnParticle(Particle.GLOW, currentLoc, 1, 0.05, 0.05, 0.05, 0.01);
                } else if (heroId.equals("yuuka")) {
                    currentLoc.getWorld().spawnParticle(Particle.PORTAL, currentLoc, 3, 0.1, 0.1, 0.1, 0.01);
                } else {
                    currentLoc.getWorld().spawnParticle(Particle.END_ROD, currentLoc, 2, 0.05, 0.05, 0.05, 0.01);
                }

                if (isBlockAtLocation(currentLoc)) {
                    if (penetrated < maxPenetration) {
                        penetrated++;
                    } else {
                        cancel();
                        return;
                    }
                }

                for (Player target : currentLoc.getWorld().getPlayers()) {
                    if (target == player || !target.isOnline()) continue;
                    if (!gameManager.isPlayerAlive(target)) continue;
                    String st = gameManager.getPlayerTeam(player);
                    String tt = gameManager.getPlayerTeam(target);
                    if (st != null && tt != null && st.equals(tt) && !kit.isCanDamageTeammates()) continue;

                    if (segmentHitsPlayer(prevLoc, currentLoc, target)) {
                        hasHit = true;
                        Location hitLoc = getHitLocation(prevLoc, currentLoc, target);
                        double damage = calculateDamage(player, target, kit, penetrated, hitLoc);
                        spawnBloodEffect(target);
                        target.damage(damage, player);
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ============================================================
    // 辅助
    // ============================================================

    private void spawnBloodEffect(Player target) {
        Location loc = target.getLocation().clone().add(0, 1, 0);
        try {
            target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc, 20, 0.3, 0.5, 0.3, 0.1);
        } catch (Exception e) {}
    }

    private double calculateDamage(Player shooter, Player target, HeroKit kit, int penetrated, Location hitLoc) {
        double damage = kit.getLaserDamage();
        String heroId = kit.getId();

        String part = getHitPartByRelativeY(target, hitLoc);

        if (heroId.equals("mika")) {
            damage = kit.getLaserDamage() * 1.55;
            target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
        } else if (heroId.equals("yuuka")) {
            double distance = shooter.getLocation().distance(target.getLocation());
            double baseDamage = 16;
            if (part.equals("HEAD")) {
                baseDamage = 25;
                target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
            } else if (part.equals("LIMB")) {
                baseDamage = 13;
            }
            if (distance > 30) baseDamage *= 0.8;
            damage = baseDamage;
        } else {
            if (part.equals("HEAD")) {
                damage *= heroId.equals("izumi") ? 1.85 : heroId.equals("kuroko") ? 1.7 : heroId.equals("al1s") ? 1.5 : 1.7;
                target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
            } else if (part.equals("CHEST")) {
                damage *= heroId.equals("izumi") ? 1.0 : heroId.equals("kuroko") ? 1.1 : heroId.equals("al1s") ? 1.02 : 1.05;
            } else {
                damage *= heroId.equals("izumi") ? 1.1 : heroId.equals("kuroko") ? 0.8 : heroId.equals("al1s") ? 0.9 : 1.0;
            }
        }

        if (penetrated > 0) {
            if (heroId.equals("mika")) damage *= 0.7;
            else damage *= kit.getBlockDamageReduction();
        }

        if (heroId.equals("mika")) {
            MikaSkillListener mikaListener = BACS.getInstance().getMikaSkillListener();
            if (mikaListener != null && mikaListener.isBulletTimeActive(shooter)) {
                damage *= 1.1;
            }
        }

        if (heroId.equals("izumi")) {
            String shooterTeam = gameManager.getPlayerTeam(shooter);
            String targetTeam = gameManager.getPlayerTeam(target);
            if (shooterTeam != null && targetTeam != null && !shooterTeam.equals(targetTeam)) {
                shooter.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1, true, false));
            }
        }

        return damage;
    }

    private double calculateSpread(Player player, HeroKit kit) {
        double spread;
        String heroId = kit.getId();

        if (heroId.equals("mika")) {
            if (player.isOnGround()) {
                if (isPlayerMoving(player)) spread = 3.0;
                else spread = 0.0;
            } else {
                spread = 4.0;
            }
            return spread;
        }

        if (heroId.equals("yuuka")) {
            if (player.isOnGround()) {
                if (isPlayerMoving(player)) spread = 1.2;
                else spread = 0.0;
            } else {
                spread = 1.8;
            }
            return spread;
        }

        if (player.isOnGround()) {
            if (isPlayerMoving(player)) spread = heroId.equals("kuroko") ? 1.5 : heroId.equals("kayi") ? 0.1 : 0.3;
            else spread = 0.0;
        } else {
            spread = heroId.equals("kuroko") ? 2.0 : heroId.equals("kayi") ? 0.3 : 0.6;
        }
        if (kit.getId().equals("kuroko") && gameManager.getExSkillManager().isExActive(player)) spread *= 0.7;
        if (kit.getId().equals("al1s") && player.isSneaking() && !isPlayerMoving(player)) spread = 0.0;
        return spread;
    }

    private boolean isPlayerMoving(Player player) {
        Vector v = player.getVelocity();
        return Math.abs(v.getX()) > 0.05 || Math.abs(v.getZ()) > 0.05;
    }

    private void updateAmmoDisplay(Player player, HeroKit kit) {
        UUID uuid = player.getUniqueId();

        if (kit.getId().equals("serina")) {
            int ammo = ammoManager.getMagazineAmmo(uuid);
            int reserve = ammoManager.getReserveAmmo(uuid);
            int exCharge = gameManager.getExSkillManager().getCharge(player);
            player.sendActionBar(Component.text("🔫 神名 | 子弹: " + ammo + "/6 | 备用: " + reserve + " | ⚡ " + exCharge + "/" + kit.getExSkillMaxCharge(), NamedTextColor.YELLOW));
            return;
        }

        if (kit.getId().equals("al1s")) {
            int ammo = ammoManager.getMagazineAmmo(uuid);
            int reserve = ammoManager.getReserveAmmo(uuid);
            int exCharge = gameManager.getExSkillManager().getCharge(player);
            Long cooldownEnd = al1sCooldownEnd.get(uuid);
            String cooldownText = "";
            if (cooldownEnd != null) {
                long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000 + 1;
                if (remaining > 0) cooldownText = " ⏳" + remaining + "s";
            }
            String chargeText = "";
            if (isCharging.getOrDefault(uuid, false)) {
                int ticks = chargeTicks.getOrDefault(uuid, 0);
                double bonus = Math.min((ticks / 20.0) * 0.02, MAX_CHARGE_BONUS);
                chargeText = " ⚡蓄力" + (int)(bonus * 100) + "%";
            }
            player.sendActionBar(Component.text("🔫 超新星·光之剑 | 子弹: " + ammo + "/" + kit.getMagazineSize() + " | 备用: " + reserve + " | ⚡ " + exCharge + "/" + kit.getExSkillMaxCharge() + cooldownText + chargeText, NamedTextColor.YELLOW));
            return;
        }

        if (kit.getId().equals("rakufu")) {
            int ammo = ammoManager.getMagazineAmmo(uuid);
            int reserve = ammoManager.getReserveAmmo(uuid);
            int exCharge = gameManager.getExSkillManager().getCharge(player);
            String autoText = rakufuAutoFire.getOrDefault(uuid, false) ? " 🔥连射" : "";
            player.sendActionBar(Component.text("🔫 破晓 | 子弹: " + ammo + "/50 | 备用: " + reserve + " | ⚡ " + exCharge + "/" + kit.getExSkillMaxCharge() + autoText, NamedTextColor.YELLOW));
            return;
        }

        int ammo = ammoManager.getMagazineAmmo(uuid);
        int reserve = ammoManager.getReserveAmmo(uuid);
        player.sendActionBar(Component.text("🔫 " + ammo + "/" + kit.getMagazineSize() + " | 备用: " + reserve +
                " | ⚡ " + gameManager.getExSkillManager().getCharge(player) + "/" + kit.getExSkillMaxCharge(), NamedTextColor.YELLOW));
    }

    public void resetAmmo(Player player, HeroKit kit) {
        UUID uuid = player.getUniqueId();

        if (kit.getId().equals("serina")) {
            ammoManager.setMagazineAmmo(uuid, 6);
            updateAmmoDisplay(player, kit);
            return;
        }

        if (kit.getId().equals("rakufu")) {
            ammoManager.setMagazineAmmo(uuid, 50);
            updateAmmoDisplay(player, kit);
            return;
        }

        ammoManager.resetRound(uuid, kit.getMagazineSize(), kit.getInitialReserveAmmo());
        updateAmmoDisplay(player, kit);
    }

    // ============================================================
    // 事件
    // ============================================================

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        HeroKit kit = gameManager.getPlayerHero(player);

        if (kit == null || !kit.getId().equals("al1s")) return;

        UUID uuid = player.getUniqueId();

        if (event.isSneaking()) {
            tryStartAl1sCharge(player, kit);
        } else {
            if (isCharging.getOrDefault(uuid, false)) {
                releaseAl1sCharge(player, kit);
            }
        }
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        stopShooting(player);
        stopRightClickShooting(player);
        stopRakufuShooting(player);

        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null) return;
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (newItem != null && newItem.getType() == kit.getWeaponMaterial() &&
                newItem.hasItemMeta() && newItem.getItemMeta().hasDisplayName() &&
                newItem.getItemMeta().displayName().toString().contains(kit.getWeaponName())) {
            weaponSwitchTime.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        stopShooting(event.getPlayer());
        stopRightClickShooting(event.getPlayer());
        stopRakufuShooting(event.getPlayer());
    }

    /**
     * 主武器按 Q 丢弃：遍历所有 HERO_KITS 匹配
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        // 遍历所有 HeroKit 找匹配的主武器
        HeroKit weaponKit = null;
        for (HeroKit hk : GameManager.HERO_KITS) {
            if (hk.getWeaponMaterial() == Material.AIR) continue;
            if (item.getType() == hk.getWeaponMaterial() &&
                    item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().displayName().toString().contains(hk.getWeaponName())) {
                weaponKit = hk;
                break;
            }
        }

        if (weaponKit != null) {
            // 蹲下时不可丢弃（用于触发某些技能，如未花蹲下丢主武器）
            if (player.isSneaking() && weaponKit.getId().equals("mika")) {
                event.setCancelled(true);
                MikaSkillListener mikaListener = BACS.getInstance().getMikaSkillListener();
                if (mikaListener != null) mikaListener.triggerExPrepare(player);
                return;
            }

            // 按 Q 丢弃主武器
            gameManager.getShopManager().setEquippedMainWeapon(player, null);
            player.sendMessage(Component.text("📤 主武器已丢弃", NamedTextColor.YELLOW));
            gameManager.getAmmoManager().resetPlayer(player.getUniqueId());
            return;
        }

        // 近战武器不可丢弃
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit != null && item.getType() == kit.getMeleeWeaponMaterial() &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains(kit.getMeleeWeaponName())) {
            if (kit.getId().equals("serina") && item.getItemMeta().displayName().toString().contains("治疗之杖")) {
                return;
            }
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ 近战武器不可丢弃！", NamedTextColor.RED));
            return;
        }

        if (item.getType() == Material.GHAST_TEAR && item.hasItemMeta() &&
                item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().equals(GameManager.SHOP_ITEM_NAME)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = event.getCurrentItem();
        if (item != null && isRestrictedItem(item)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ 此物品不可移动！", NamedTextColor.RED));
        }
        ItemStack cursor = event.getCursor();
        if (cursor != null && isRestrictedItem(cursor)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ 此物品不可移动！", NamedTextColor.RED));
        }
    }

    private boolean isRestrictedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        String name = item.getItemMeta().displayName().toString();
        return name.contains("超新星·光之剑") || name.contains("α·突击步枪") || name.contains("改装·光之剑") || name.contains("α·奇兵") ||
                name.contains("Quis ut Deus") || name.contains("逻辑与理性") || name.contains("神名") || name.contains("破晓") ||
                name.contains("匕首") || name.contains("长刀") || name.contains("军用匕首") || name.contains("合金稿") ||
                name.contains("圣三一之剑") || name.contains("计算之刃") || name.contains("治疗之杖") || name.contains("战术匕首") ||
                name.contains("治疗之光") || name.contains("EX.在这里的我") || name.contains("商店") || name.contains("探测箭矢") ||
                name.contains("拆弹器") || name.contains("炸弹起爆器") || name.contains("炸弹部署器") || name.contains("炸药追踪器") ||
                name.contains("裁决") || name.contains("灵异") || name.contains("制式手枪") || name.contains("盾牌") ||
                name.contains("标记器") || name.contains("EX技能");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.setMaximumNoDamageTicks(0);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        stopShooting(player);
        stopRightClickShooting(player);
        stopRakufuShooting(player);
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit != null) {
            ammoManager.resetRound(player.getUniqueId(), kit.getMagazineSize(), kit.getInitialReserveAmmo());
            if (kit.getId().equals("al1s")) {
                isCharging.put(player.getUniqueId(), false);
                chargeTicks.remove(player.getUniqueId());
                chargeFullWarned.remove(player.getUniqueId());
                al1sCooldownEnd.remove(player.getUniqueId());
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                player.removePotionEffect(PotionEffectType.BLINDNESS);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        stopShooting(event.getPlayer());
        stopRightClickShooting(event.getPlayer());
        stopRakufuShooting(event.getPlayer());
        lastMoveTime.remove(uuid);
        isSneaking.remove(uuid);
        weaponSwitchTime.remove(uuid);
        lastLeftClickTime.remove(uuid);
        ammoManager.resetPlayer(uuid);
        isCharging.remove(uuid);
        chargeTicks.remove(uuid);
        chargeFullWarned.remove(uuid);
        lastChargeTime.remove(uuid);
        al1sCooldownEnd.remove(uuid);
        lastRakufuClickTime.remove(uuid);
        rakufuAutoFire.remove(uuid);
        rakufuTaskId.remove(uuid);
    }
}