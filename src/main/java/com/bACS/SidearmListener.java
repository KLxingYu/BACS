package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SidearmListener implements Listener {
    private final BACS plugin;
    private final GameManager gameManager;
    private final Map<UUID, Boolean> isReloading = new HashMap<>();

    private static final long PISTOL_INTERVAL = 154;
    private static final long SPIRIT_GUN_INTERVAL = 179;
    private static final long VERDICT_INTERVAL = 250;
    private static final long PISTOL_RELOAD_TIME = 1200;
    private static final long SPIRIT_GUN_RELOAD_TIME = 1000;
    private static final long VERDICT_RELOAD_TIME = 2000;

    private static final int PISTOL_MAX_AMMO = 12;
    private static final int SPIRIT_GUN_MAX_AMMO = 15;
    private static final int VERDICT_MAX_AMMO = 6;

    public SidearmListener(BACS plugin, GameManager gameManager) {
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

        boolean isPistol = item.getType() == Material.WOODEN_AXE &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains("制式手枪");
        boolean isSpiritGun = item.getType() == Material.IRON_AXE &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains("灵异");
        boolean isVerdict = item.getType() == Material.IRON_AXE &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains("裁决");
        boolean isRage = item.getType() == Material.GOLDEN_AXE &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains("怒焰");
        boolean isHandCannon = item.getType() == Material.NETHERITE_AXE &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains("手炮");

        if (!isPistol && !isSpiritGun && !isVerdict && !isRage && !isHandCannon) return;

        UUID uuid = player.getUniqueId();

        if (isRage) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                plugin.getSidearmShootingSystem().startRageFire(player);
                return;
            }
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
                plugin.getSidearmShootingSystem().startRageReload(player);
                return;
            }
            return;
        }

        if (isHandCannon) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                plugin.getSidearmShootingSystem().startHandCannonFire(player);
                return;
            }
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
                plugin.getSidearmShootingSystem().startHandCannonReload(player);
                return;
            }
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            if (isReloading.getOrDefault(uuid, false)) {
                player.sendActionBar(Component.text("🔄 换弹中...", NamedTextColor.YELLOW));
                return;
            }
            shootOnce(player, isPistol, isSpiritGun, isVerdict);
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);

            if (isVerdict) {
                if (isReloading.getOrDefault(uuid, false)) {
                    player.sendActionBar(Component.text("🔄 换弹中...", NamedTextColor.YELLOW));
                    return;
                }
                shootOnce(player, false, false, true);
            } else {
                startReload(player, isPistol, false);
            }
        }
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (newItem == null) return;

        if (newItem.getType() == Material.GOLDEN_AXE &&
                newItem.hasItemMeta() && newItem.getItemMeta().hasDisplayName() &&
                newItem.getItemMeta().displayName().toString().contains("怒焰")) {
            plugin.getSidearmShootingSystem().setWeaponSwitchCooldown(player, WeaponConfig.RAGE_SWITCH_COOLDOWN);
        }
        if (newItem.getType() == Material.NETHERITE_AXE &&
                newItem.hasItemMeta() && newItem.getItemMeta().hasDisplayName() &&
                newItem.getItemMeta().displayName().toString().contains("手炮")) {
            plugin.getSidearmShootingSystem().setWeaponSwitchCooldown(player, WeaponConfig.HANDCANNON_SWITCH_COOLDOWN);
        }
    }

    // ============================================================
    // 丢弃副武器
    // ============================================================
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        ShopManager shop = gameManager.getShopManager();
        WeaponDataManager wdm = plugin.getWeaponDataManager();

        if (item.getType() == Material.WOODEN_AXE &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains("制式手枪")) {
            wdm.writeAmmo(item, shop.getPistolAmmo(player), shop.getPistolReserve(player));
            event.getItemDrop().setItemStack(item);
            shop.setHasPistol(player, false);
            shop.setPistolAmmo(player, 0);
            shop.setPistolReserve(player, 0);
            return;
        }

        if (item.getType() == Material.IRON_AXE &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains("灵异")) {
            wdm.writeAmmo(item, shop.getSpiritGunAmmo(player), shop.getSpiritGunReserve(player));
            event.getItemDrop().setItemStack(item);
            shop.setHasSpiritGun(player, false);
            shop.setSpiritGunAmmo(player, 0);
            shop.setSpiritGunReserve(player, 0);
            return;
        }

        if (item.getType() == Material.IRON_AXE &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains("裁决")) {
            wdm.writeAmmo(item, shop.getVerdictAmmo(player), shop.getVerdictReserve(player));
            event.getItemDrop().setItemStack(item);
            shop.setHasVerdict(player, false);
            shop.setVerdictAmmo(player, 0);
            shop.setVerdictReserve(player, 0);
            return;
        }

        if (item.getType() == Material.GOLDEN_AXE &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains("怒焰")) {
            wdm.writeAmmo(item, shop.getRageAmmo(player), shop.getRageReserve(player));
            event.getItemDrop().setItemStack(item);
            shop.setHasRage(player, false);
            shop.setRageAmmo(player, 0);
            shop.setRageReserve(player, 0);
            return;
        }

        if (item.getType() == Material.NETHERITE_AXE &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains("手炮")) {
            wdm.writeAmmo(item, shop.getHandCannonAmmo(player), shop.getHandCannonReserve(player));
            event.getItemDrop().setItemStack(item);
            shop.setHasHandCannon(player, false);
            shop.setHandCannonAmmo(player, 0);
            shop.setHandCannonReserve(player, 0);
            return;
        }
    }

    @EventHandler
    public void onPlayerAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack picked = event.getItem().getItemStack();

        boolean isPistol = picked.getType() == Material.WOODEN_AXE &&
                picked.hasItemMeta() && picked.getItemMeta().hasDisplayName() &&
                picked.getItemMeta().displayName().toString().contains("制式手枪");
        boolean isSpiritGun = picked.getType() == Material.IRON_AXE &&
                picked.hasItemMeta() && picked.getItemMeta().hasDisplayName() &&
                picked.getItemMeta().displayName().toString().contains("灵异");
        boolean isVerdict = picked.getType() == Material.IRON_AXE &&
                picked.hasItemMeta() && picked.getItemMeta().hasDisplayName() &&
                picked.getItemMeta().displayName().toString().contains("裁决");
        boolean isRage = picked.getType() == Material.GOLDEN_AXE &&
                picked.hasItemMeta() && picked.getItemMeta().hasDisplayName() &&
                picked.getItemMeta().displayName().toString().contains("怒焰");
        boolean isHandCannon = picked.getType() == Material.NETHERITE_AXE &&
                picked.hasItemMeta() && picked.getItemMeta().hasDisplayName() &&
                picked.getItemMeta().displayName().toString().contains("手炮");

        if (!isPistol && !isSpiritGun && !isVerdict && !isRage && !isHandCannon) return;

        ShopManager shop = gameManager.getShopManager();
        boolean hasSecondary = shop.hasPistol(player) || shop.hasSpiritGun(player)
                || shop.hasVerdict(player) || shop.hasRage(player) || shop.hasHandCannon(player);
        if (hasSecondary) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ 你已经有一把副武器！", NamedTextColor.RED));
        }
    }

    // ============================================================
    // 旧副武器射击
    // ============================================================
    private void shootOnce(Player player, boolean isPistol, boolean isSpiritGun, boolean isVerdict) {
        UUID uuid = player.getUniqueId();
        ShopManager shop = gameManager.getShopManager();

        long interval;
        int maxAmmo;

        if (isPistol) {
            interval = PISTOL_INTERVAL;
            maxAmmo = PISTOL_MAX_AMMO;
        } else if (isSpiritGun) {
            interval = SPIRIT_GUN_INTERVAL;
            maxAmmo = SPIRIT_GUN_MAX_AMMO;
        } else if (isVerdict) {
            interval = VERDICT_INTERVAL;
            maxAmmo = VERDICT_MAX_AMMO;
        } else {
            return;
        }

        if (!shop.canShootSidearm(uuid, interval)) return;

        int ammo;
        int reserve;
        if (isPistol) {
            ammo = shop.getPistolAmmo(player);
            reserve = shop.getPistolReserve(player);
        } else if (isSpiritGun) {
            ammo = shop.getSpiritGunAmmo(player);
            reserve = shop.getSpiritGunReserve(player);
        } else {
            ammo = shop.getVerdictAmmo(player);
            reserve = shop.getVerdictReserve(player);
        }

        if (ammo <= 0) {
            player.sendActionBar(Component.text("❌ 子弹用完！", NamedTextColor.RED));
            if (isVerdict) {
                startReload(player, false, true);
            } else {
                startReload(player, isPistol, false);
            }
            return;
        }

        shop.setLastSidearmShot(uuid, System.currentTimeMillis());

        if (isPistol) {
            shop.setPistolAmmo(player, ammo - 1);
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand != null && hand.getType() == Material.WOODEN_AXE) {
                plugin.getWeaponDataManager().writeAmmo(hand, ammo - 1, reserve);
            }
        } else if (isSpiritGun) {
            shop.setSpiritGunAmmo(player, ammo - 1);
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand != null && hand.getType() == Material.IRON_AXE) {
                plugin.getWeaponDataManager().writeAmmo(hand, ammo - 1, reserve);
            }
        } else {
            shop.setVerdictAmmo(player, ammo - 1);
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand != null && hand.getType() == Material.IRON_AXE) {
                plugin.getWeaponDataManager().writeAmmo(hand, ammo - 1, reserve);
            }
        }

        shootSidearm(player, isPistol, isSpiritGun, isVerdict);
    }

    private void startReload(Player player, boolean isPistol, boolean isVerdict) {
        UUID uuid = player.getUniqueId();
        ShopManager shop = gameManager.getShopManager();

        if (isReloading.getOrDefault(uuid, false)) return;

        int maxAmmo;
        long reloadTime;
        int currentAmmo;
        int reserve;

        if (isPistol) {
            maxAmmo = PISTOL_MAX_AMMO;
            reloadTime = PISTOL_RELOAD_TIME;
            currentAmmo = shop.getPistolAmmo(player);
            reserve = shop.getPistolReserve(player);
        } else if (isVerdict) {
            maxAmmo = VERDICT_MAX_AMMO;
            reloadTime = VERDICT_RELOAD_TIME;
            currentAmmo = shop.getVerdictAmmo(player);
            reserve = shop.getVerdictReserve(player);
        } else {
            maxAmmo = SPIRIT_GUN_MAX_AMMO;
            reloadTime = SPIRIT_GUN_RELOAD_TIME;
            currentAmmo = shop.getSpiritGunAmmo(player);
            reserve = shop.getSpiritGunReserve(player);
        }

        if (currentAmmo >= maxAmmo) {
            player.sendActionBar(Component.text("✅ 子弹已满！", NamedTextColor.GREEN));
            return;
        }

        int need = maxAmmo - currentAmmo;
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
                if (isPistol) {
                    shop.setPistolAmmo(player, currentAmmo + actual);
                    shop.setPistolReserve(player, reserve - actual);
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand != null && hand.getType() == Material.WOODEN_AXE) {
                        plugin.getWeaponDataManager().writeAmmo(hand, currentAmmo + actual, reserve - actual);
                    }
                } else if (isVerdict) {
                    shop.setVerdictAmmo(player, currentAmmo + actual);
                    shop.setVerdictReserve(player, reserve - actual);
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand != null && hand.getType() == Material.IRON_AXE) {
                        plugin.getWeaponDataManager().writeAmmo(hand, currentAmmo + actual, reserve - actual);
                    }
                } else {
                    shop.setSpiritGunAmmo(player, currentAmmo + actual);
                    shop.setSpiritGunReserve(player, reserve - actual);
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand != null && hand.getType() == Material.IRON_AXE) {
                        plugin.getWeaponDataManager().writeAmmo(hand, currentAmmo + actual, reserve - actual);
                    }
                }
                isReloading.put(uuid, false);
                player.sendActionBar(Component.text("✅ 换弹完成！+" + actual + "发", NamedTextColor.GREEN));
            }
        }.runTaskLater(plugin, reloadTime / 50);
    }

    private void shootSidearm(Player player, boolean isPistol, boolean isSpiritGun, boolean isVerdict) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().clone();

        double spread = player.isSprinting() ? 2.0 : 0.0;
        if (spread > 0) {
            direction.add(new Vector(
                    (Math.random() - 0.5) * spread,
                    (Math.random() - 0.5) * spread,
                    (Math.random() - 0.5) * spread
            )).normalize();
        }

        int maxPenetration;
        boolean silent;

        if (isPistol) {
            maxPenetration = 2;
            silent = false;
        } else if (isSpiritGun) {
            maxPenetration = 3;
            silent = true;
        } else {
            maxPenetration = 4;
            silent = false;
        }

        if (!silent) {
            if (isVerdict) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.2f);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.3f, 1.5f);
            }
        }

        new BukkitRunnable() {
            Location currentLoc = eyeLoc.clone();
            Location prevLoc = eyeLoc.clone();
            Vector directionVec = direction.clone();
            double traveled = 0;
            int penetratedBlocks = 0;
            boolean hasHit = false;

            @Override
            public void run() {
                if (hasHit) { cancel(); return; }
                if (traveled > 90) { cancel(); return; }

                double step = 0.5;
                for (int i = 0; i < 6; i++) {
                    traveled += step;
                    prevLoc = currentLoc.clone();
                    currentLoc.add(directionVec.clone().multiply(step));

                    Material blockType = currentLoc.getBlock().getType();
                    if (blockType != Material.AIR && blockType != Material.CAVE_AIR && blockType != Material.VOID_AIR &&
                            blockType != Material.WATER && blockType != Material.LAVA) {
                        if (penetratedBlocks < maxPenetration) {
                            penetratedBlocks++;
                            currentLoc.getWorld().spawnParticle(Particle.BLOCK, currentLoc, 3, 0.2, 0.2, 0.2, 0, blockType.createBlockData());
                        } else {
                            hasHit = true;
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
                            String hitPart = getHitPartByRelativeY(target, hitLoc);
                            double damage;
                            if (isPistol) {
                                damage = hitPart.equals("HEAD") ? 45 : hitPart.equals("CHEST") ? 25 : 20;
                            } else if (isSpiritGun) {
                                damage = hitPart.equals("HEAD") ? 65 : hitPart.equals("CHEST") ? 30 : 26;
                            } else {
                                damage = hitPart.equals("HEAD") ? 120 : hitPart.equals("CHEST") ? 50 : 45;
                            }
                            if (penetratedBlocks > 0) {
                                if (isVerdict) {
                                    damage *= 0.65;
                                } else {
                                    damage *= isPistol ? 0.5 : 0.75;
                                }
                            }
                            target.damage(damage, player);
                            cancel();
                            return;
                        }
                    }
                }

                if (!silent) {
                    if (isVerdict) {
                        currentLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, currentLoc, 3, 0.1, 0.1, 0.1, 0.01);
                        currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 2, 0.05, 0.05, 0.05, 0.01);
                    } else {
                        currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 2, 0.05, 0.05, 0.05, 0.01);
                    }
                } else {
                    player.spawnParticle(Particle.FLAME, currentLoc, 2, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        isReloading.remove(event.getPlayer().getUniqueId());
        plugin.getSidearmShootingSystem().resetPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        isReloading.remove(event.getEntity().getUniqueId());
        plugin.getSidearmShootingSystem().resetPlayer(event.getEntity());
    }
}