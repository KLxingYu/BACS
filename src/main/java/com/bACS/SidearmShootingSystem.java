package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SidearmShootingSystem {
    private final BACS plugin;
    private final GameManager gameManager;

    private final Map<UUID, Boolean> isFiring = new HashMap<>();
    private final Map<UUID, Integer> fireTaskId = new HashMap<>();
    private final Map<UUID, Boolean> isReloading = new HashMap<>();
    private final Map<UUID, Long> weaponSwitchCooldown = new HashMap<>();

    public SidearmShootingSystem(BACS plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    // ============================================================
    // 碰撞箱判定
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
    // 怒焰
    // ============================================================
    public void startRageFire(Player player) {
        UUID uuid = player.getUniqueId();
        ShopManager shop = gameManager.getShopManager();

        Long switchEnd = weaponSwitchCooldown.get(uuid);
        if (switchEnd != null && System.currentTimeMillis() < switchEnd) {
            long remain = (switchEnd - System.currentTimeMillis()) / 1000 + 1;
            player.sendActionBar(Component.text("⏳ 武器切换冷却: " + remain + "s", NamedTextColor.RED));
            return;
        }

        if (isReloading.getOrDefault(uuid, false)) {
            player.sendActionBar(Component.text("🔄 换弹中...", NamedTextColor.YELLOW));
            return;
        }

        if (shop.getRageAmmo(player) <= 0) {
            player.sendActionBar(Component.text("❌ 没有弹药！", NamedTextColor.RED));
            startRageReload(player);
            return;
        }

        if (isFiring.getOrDefault(uuid, false)) return;
        isFiring.put(uuid, true);

        rageShoot(player);

        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !isFiring.getOrDefault(uuid, false)) {
                    cancel();
                    return;
                }
                if (gameManager.getState() != GameState.ROUND_ACTIVE || !gameManager.isPlayerAlive(player)) {
                    stopRageFire(player);
                    cancel();
                    return;
                }
                if (isReloading.getOrDefault(uuid, false)) return;
                if (shop.getRageAmmo(player) <= 0) {
                    stopRageFire(player);
                    startRageReload(player);
                    cancel();
                    return;
                }
                rageShoot(player);
            }
        }.runTaskTimer(plugin, 2L, 2L).getTaskId();

        fireTaskId.put(uuid, taskId);
    }

    public void stopRageFire(Player player) {
        UUID uuid = player.getUniqueId();
        isFiring.put(uuid, false);
        Integer task = fireTaskId.remove(uuid);
        if (task != null) Bukkit.getScheduler().cancelTask(task);
    }

    private void rageShoot(Player player) {
        UUID uuid = player.getUniqueId();
        ShopManager shop = gameManager.getShopManager();

        if (!shop.canShootSidearm(uuid, WeaponConfig.RAGE_FIRE_INTERVAL)) return;

        int ammo = shop.getRageAmmo(player);
        if (ammo <= 0) return;

        shop.setRageAmmo(player, ammo - 1);
        shop.setLastSidearmShot(uuid, System.currentTimeMillis());

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null && hand.getType() == Material.GOLDEN_AXE) {
            plugin.getWeaponDataManager().writeAmmo(hand, ammo - 1, shop.getRageReserve(player));
        }

        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().clone();

        double spread = calculateSpread(player,
                WeaponConfig.RAGE_SPREAD_IDLE,
                WeaponConfig.RAGE_SPREAD_MOVING,
                WeaponConfig.RAGE_SPREAD_SPRINTING,
                5.0);
        applySpread(direction, spread);

        final Vector finalDirection = direction;

        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.4f, 1.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.2f, 2.0f);

        new BukkitRunnable() {
            Location currentLoc = eyeLoc.clone();
            Location prevLoc = eyeLoc.clone();
            Vector velocity = finalDirection.clone().multiply(2.5);
            double traveled = 0;
            int penetrated = 0;
            boolean hasHit = false;

            @Override
            public void run() {
                if (hasHit) { cancel(); return; }
                traveled += velocity.length();
                prevLoc = currentLoc.clone();
                currentLoc.add(velocity);
                if (traveled > 60) { cancel(); return; }

                currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 2, 0.05, 0.05, 0.05, 0.01);
                currentLoc.getWorld().spawnParticle(Particle.SMOKE, currentLoc, 1, 0.05, 0.05, 0.05, 0.01);

                if (isBlockAtLocation(currentLoc)) {
                    if (penetrated < WeaponConfig.RAGE_PENETRATION) {
                        penetrated++;
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
                    String st = gameManager.getPlayerTeam(player);
                    String tt = gameManager.getPlayerTeam(target);
                    if (st != null && tt != null && st.equals(tt)) continue;

                    if (segmentHitsPlayer(prevLoc, currentLoc, target)) {
                        hasHit = true;
                        Location hitLoc = getHitLocation(prevLoc, currentLoc, target);
                        double damage = calcRageDamage(target, hitLoc, traveled);
                        if (penetrated > 0) damage *= WeaponConfig.RAGE_PENETRATION_DAMAGE;
                        target.damage(damage, player);
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private double calcRageDamage(Player target, Location hitLoc, double traveled) {
        String part = getHitPartByRelativeY(target, hitLoc);
        double damage;
        if (part.equals("HEAD")) {
            damage = WeaponConfig.RAGE_HEAD_DAMAGE;
            target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
        } else if (part.equals("CHEST")) {
            damage = WeaponConfig.RAGE_CHEST_DAMAGE;
        } else {
            damage = WeaponConfig.RAGE_LIMB_DAMAGE;
        }
        if (traveled > WeaponConfig.RAGE_FALLOFF_START) {
            damage *= WeaponConfig.RAGE_FALLOFF_MULT;
        }
        return damage;
    }

    public void startRageReload(Player player) {
        UUID uuid = player.getUniqueId();
        ShopManager shop = gameManager.getShopManager();

        if (isReloading.getOrDefault(uuid, false)) return;

        int currentAmmo = shop.getRageAmmo(player);
        int maxAmmo = WeaponConfig.RAGE_MAX_AMMO;
        if (currentAmmo >= maxAmmo) {
            player.sendActionBar(Component.text("✅ 子弹已满！", NamedTextColor.GREEN));
            return;
        }

        int need = maxAmmo - currentAmmo;
        int reserve = shop.getRageReserve(player);
        if (reserve <= 0) {
            player.sendActionBar(Component.text("❌ 没有备用弹药！", NamedTextColor.RED));
            return;
        }
        int actual = Math.min(need, reserve);

        isReloading.put(uuid, true);
        player.sendActionBar(Component.text("🔄 换弹中...", NamedTextColor.YELLOW));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    isReloading.put(uuid, false);
                    cancel();
                    return;
                }
                shop.setRageAmmo(player, currentAmmo + actual);
                shop.setRageReserve(player, reserve - actual);
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand != null && hand.getType() == Material.GOLDEN_AXE) {
                    plugin.getWeaponDataManager().writeAmmo(hand, currentAmmo + actual, reserve - actual);
                }
                isReloading.put(uuid, false);
                player.sendActionBar(Component.text("✅ 换弹完成！+" + actual + "发", NamedTextColor.GREEN));
            }
        }.runTaskLater(plugin, WeaponConfig.RAGE_RELOAD_TIME / 50);
    }

    // ============================================================
    // 手炮
    // ============================================================
    public void startHandCannonFire(Player player) {
        UUID uuid = player.getUniqueId();
        ShopManager shop = gameManager.getShopManager();

        Long switchEnd = weaponSwitchCooldown.get(uuid);
        if (switchEnd != null && System.currentTimeMillis() < switchEnd) {
            long remain = (switchEnd - System.currentTimeMillis()) / 1000 + 1;
            player.sendActionBar(Component.text("⏳ 武器切换冷却: " + remain + "s", NamedTextColor.RED));
            return;
        }

        if (isReloading.getOrDefault(uuid, false)) {
            player.sendActionBar(Component.text("🔄 换弹中...", NamedTextColor.YELLOW));
            return;
        }

        if (shop.getHandCannonAmmo(player) <= 0) {
            player.sendActionBar(Component.text("❌ 没有弹药！", NamedTextColor.RED));
            startHandCannonReload(player);
            return;
        }

        handCannonShoot(player);
    }

    private void handCannonShoot(Player player) {
        UUID uuid = player.getUniqueId();
        ShopManager shop = gameManager.getShopManager();

        if (!shop.canShootSidearm(uuid, WeaponConfig.HANDCANNON_FIRE_INTERVAL)) return;

        int ammo = shop.getHandCannonAmmo(player);
        if (ammo <= 0) return;

        shop.setHandCannonAmmo(player, ammo - 1);
        shop.setLastSidearmShot(uuid, System.currentTimeMillis());

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null && hand.getType() == Material.NETHERITE_AXE) {
            plugin.getWeaponDataManager().writeAmmo(hand, ammo - 1, shop.getHandCannonReserve(player));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);

        Location eyeLoc = player.getEyeLocation();
        Vector baseDir = eyeLoc.getDirection().clone();

        // 根据移动状态的基础散布
        double baseSpread = calculateSpread(player,
                WeaponConfig.HANDCANNON_SPREAD_IDLE,
                WeaponConfig.HANDCANNON_SPREAD_MOVING,
                WeaponConfig.HANDCANNON_SPREAD_SPRINTING,
                WeaponConfig.HANDCANNON_SPREAD_AIR);

        // ★ 消耗 1 发弹药，发射 13 弹丸，每颗弹丸额外加 2.0° 随机散布
        for (int i = 0; i < WeaponConfig.HANDCANNON_PELLETS; i++) {
            Vector dir = baseDir.clone();

            // 先应用基础散布
            if (baseSpread > 0) {
                applySpread(dir, baseSpread);
            }

            // 再应用每颗弹丸的额外散布
            applySpread(dir, WeaponConfig.HANDCANNON_PELLET_SPREAD);

            shootHandCannonPellet(player, eyeLoc.clone(), dir);
        }
    }

    private void shootHandCannonPellet(Player player, Location startLoc, Vector direction) {
        new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            Location prevLoc = startLoc.clone();
            Vector velocity = direction.clone().multiply(2.0);
            double traveled = 0;
            boolean hasHit = false;

            @Override
            public void run() {
                if (hasHit) { cancel(); return; }
                traveled += velocity.length();
                prevLoc = currentLoc.clone();
                currentLoc.add(velocity);
                if (traveled > WeaponConfig.HANDCANNON_MAX_RANGE) { cancel(); return; }

                currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 1, 0.02, 0.02, 0.02, 0.01);

                if (isBlockAtLocation(currentLoc)) {
                    cancel();
                    return;
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
                        double damage = calcHandCannonDamage(target, hitLoc, traveled);
                        target.damage(damage, player);
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private double calcHandCannonDamage(Player target, Location hitLoc, double traveled) {
        String part = getHitPartByRelativeY(target, hitLoc);
        double damage;
        if (part.equals("HEAD")) {
            damage = WeaponConfig.HANDCANNON_HEAD_DAMAGE;
        } else if (part.equals("CHEST")) {
            damage = WeaponConfig.HANDCANNON_CHEST_DAMAGE;
        } else {
            damage = WeaponConfig.HANDCANNON_LIMB_DAMAGE;
        }
        if (traveled > WeaponConfig.HANDCANNON_FALLOFF_START) {
            double falloffDistance = traveled - WeaponConfig.HANDCANNON_FALLOFF_START;
            double falloffMultiplier = 1.0 - (falloffDistance / 10.0) * 0.2;
            damage *= Math.max(0, falloffMultiplier);
        }
        return damage;
    }

    public void startHandCannonReload(Player player) {
        UUID uuid = player.getUniqueId();
        ShopManager shop = gameManager.getShopManager();

        if (isReloading.getOrDefault(uuid, false)) return;

        int currentAmmo = shop.getHandCannonAmmo(player);
        int maxAmmo = WeaponConfig.HANDCANNON_MAX_AMMO;
        if (currentAmmo >= maxAmmo) {
            player.sendActionBar(Component.text("✅ 子弹已满！", NamedTextColor.GREEN));
            return;
        }

        int need = maxAmmo - currentAmmo;
        int reserve = shop.getHandCannonReserve(player);
        if (reserve <= 0) {
            player.sendActionBar(Component.text("❌ 没有备用弹药！", NamedTextColor.RED));
            return;
        }
        int actual = Math.min(need, reserve);

        isReloading.put(uuid, true);
        long reloadTime = currentAmmo > 0 ? WeaponConfig.HANDCANNON_RELOAD_TIME_PARTIAL : WeaponConfig.HANDCANNON_RELOAD_TIME_FULL;

        player.sendActionBar(Component.text("🔄 换弹中...", NamedTextColor.YELLOW));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    isReloading.put(uuid, false);
                    cancel();
                    return;
                }
                shop.setHandCannonAmmo(player, currentAmmo + actual);
                shop.setHandCannonReserve(player, reserve - actual);
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand != null && hand.getType() == Material.NETHERITE_AXE) {
                    plugin.getWeaponDataManager().writeAmmo(hand, currentAmmo + actual, reserve - actual);
                }
                isReloading.put(uuid, false);
                player.sendActionBar(Component.text("✅ 换弹完成！+" + actual + "发", NamedTextColor.GREEN));
            }
        }.runTaskLater(plugin, reloadTime / 50);
    }

    // ============================================================
    // 偏差计算
    // ============================================================
    private double calculateSpread(Player player, double idleSpread, double movingSpread, double sprintSpread, double airSpread) {
        if (!player.isOnGround()) {
            return airSpread;
        }
        if (player.isSprinting()) {
            return sprintSpread;
        }
        if (player.getVelocity().lengthSquared() > 0.01) {
            return movingSpread;
        }
        return idleSpread;
    }

    private void applySpread(Vector direction, double angleDegrees) {
        if (angleDegrees <= 0) return;

        double angleRad = Math.toRadians(angleDegrees);
        double randomAngle = Math.random() * angleRad;
        double randomYaw = Math.random() * 2 * Math.PI;

        Vector forward = direction.clone().normalize();
        Vector worldUp = new Vector(0, 1, 0);

        Vector right;
        if (Math.abs(forward.dot(worldUp)) > 0.999) {
            right = new Vector(1, 0, 0);
        } else {
            right = forward.clone().crossProduct(worldUp).normalize();
        }
        Vector up = right.clone().crossProduct(forward).normalize();

        Vector offset = right.clone().multiply(Math.cos(randomYaw) * Math.tan(randomAngle))
                .add(up.clone().multiply(Math.sin(randomYaw) * Math.tan(randomAngle)));

        direction.add(offset).normalize();
    }

    // ============================================================
    // 武器切换冷却
    // ============================================================
    public void setWeaponSwitchCooldown(Player player, long millis) {
        weaponSwitchCooldown.put(player.getUniqueId(), System.currentTimeMillis() + millis);
    }

    public boolean isReloading(Player player) {
        return isReloading.getOrDefault(player.getUniqueId(), false);
    }

    public void resetAll() {
        isFiring.clear();
        fireTaskId.clear();
        isReloading.clear();
        weaponSwitchCooldown.clear();
    }

    public void resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        isFiring.remove(uuid);
        Integer task = fireTaskId.remove(uuid);
        if (task != null) Bukkit.getScheduler().cancelTask(task);
        isReloading.remove(uuid);
        weaponSwitchCooldown.remove(uuid);
    }
}