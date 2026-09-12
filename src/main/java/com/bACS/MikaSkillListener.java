package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class MikaSkillListener implements Listener {
    private final BACS plugin;
    private final GameManager gameManager;

    // 子弹时间状态
    private final Map<UUID, Boolean> bulletTimeActive = new HashMap<>();
    private final Map<UUID, Long> bulletTimeEnd = new HashMap<>();
    private final Map<UUID, Long> bulletTimeCooldown = new HashMap<>();
    private final Map<UUID, Integer> smallSkillCount = new HashMap<>();

    // EX技能状态
    private final Map<UUID, Boolean> exPrepared = new HashMap<>();
    private final Map<UUID, Long> exSlowEnd = new HashMap<>();

    // 龙息相关
    private final Set<UUID> dragonBreathVictims = new HashSet<>();
    private final Map<UUID, Long> dragonBreathHitTime = new HashMap<>();

    private static final long BULLET_TIME_DURATION = 8000; // 8秒
    private static final long BULLET_TIME_COOLDOWN = 12000; // 12秒
    private static final long EX_SLOW_DURATION = 7000; // 7秒

    public MikaSkillListener(BACS plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;

        startDragonBreathChecker();
    }

    public int getSmallSkillCount(Player player) {
        return smallSkillCount.getOrDefault(player.getUniqueId(), 0);
    }

    public void buySmallSkill(Player player) {
        UUID uuid = player.getUniqueId();
        int count = smallSkillCount.getOrDefault(uuid, 0);
        if (count >= 2) {
            player.sendMessage(Component.text("❌ 已达上限！", NamedTextColor.RED));
            return;
        }
        smallSkillCount.put(uuid, count + 1);
        player.sendMessage(Component.text("✅ 购买成功！圣三一的公主 x" + (count + 1), NamedTextColor.GREEN));
    }

    public boolean isBulletTimeActive(Player player) {
        return bulletTimeActive.getOrDefault(player.getUniqueId(), false) &&
                System.currentTimeMillis() < bulletTimeEnd.getOrDefault(player.getUniqueId(), 0L);
    }

    public boolean isExPrepared(Player player) {
        return exPrepared.getOrDefault(player.getUniqueId(), false);
    }

    // 触发小技能
    public void triggerSmallSkill(Player player) {
        UUID uuid = player.getUniqueId();

        if (isBulletTimeActive(player)) {
            player.sendMessage(Component.text("❌ 子弹时间已激活！", NamedTextColor.RED));
            return;
        }

        long cooldownEnd = bulletTimeCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
            player.sendMessage(Component.text("❌ 冷却中: " + remaining + "s", NamedTextColor.RED));
            return;
        }

        int count = smallSkillCount.getOrDefault(uuid, 0);
        if (count <= 0) {
            player.sendMessage(Component.text("❌ 没有可用次数！请在商店购买", NamedTextColor.RED));
            return;
        }

        smallSkillCount.put(uuid, count - 1);

        bulletTimeActive.put(uuid, true);
        bulletTimeEnd.put(uuid, System.currentTimeMillis() + BULLET_TIME_DURATION);

        player.sendMessage(Component.text("✨ 圣三一的公主！子弹时间激活！", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() >= bulletTimeEnd.getOrDefault(uuid, 0L)) {
                    bulletTimeActive.put(uuid, false);
                    bulletTimeCooldown.put(uuid, System.currentTimeMillis() + BULLET_TIME_COOLDOWN);
                    player.sendMessage(Component.text("⏰ 子弹时间结束", NamedTextColor.YELLOW));
                }
            }
        }.runTaskLater(plugin, BULLET_TIME_DURATION / 50);
    }

    // 触发EX技能准备
    public void triggerExPrepare(Player player) {
        UUID uuid = player.getUniqueId();

        if (exPrepared.getOrDefault(uuid, false)) {
            player.sendMessage(Component.text("❌ 已准备好强化攻击！", NamedTextColor.RED));
            return;
        }

        if (gameManager.getExSkillManager().getCharge(player) < 8) {
            player.sendMessage(Component.text("❌ EX技能充能不足！", NamedTextColor.RED));
            return;
        }

        gameManager.getExSkillManager().clearCharge(player);

        exPrepared.put(uuid, true);
        exSlowEnd.put(uuid, System.currentTimeMillis() + EX_SLOW_DURATION);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(EX_SLOW_DURATION / 50), 2, true, false));

        player.sendMessage(Component.text("💜 EX技能准备！右键主武器发射龙息弹！", NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.BOLD, true));
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (exPrepared.getOrDefault(uuid, false)) {
                    exPrepared.put(uuid, false);
                    exSlowEnd.put(uuid, 0L);
                    player.removePotionEffect(PotionEffectType.SLOWNESS);
                    gameManager.getExSkillManager().addCharge(player, 2);
                    player.sendMessage(Component.text("⏰ 强化攻击超时，返还2点充能", NamedTextColor.YELLOW));
                }
            }
        }.runTaskLater(plugin, EX_SLOW_DURATION / 50);
    }

    // 发射龙息弹
    public void fireDragonBreath(Player player) {
        UUID uuid = player.getUniqueId();

        if (!exPrepared.getOrDefault(uuid, false)) {
            return;
        }

        exPrepared.put(uuid, false);
        exSlowEnd.put(uuid, 0L);
        player.removePotionEffect(PotionEffectType.SLOWNESS);

        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().clone();

        player.sendMessage(Component.text("💜 龙息弹发射！", NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.BOLD, true));
        player.playSound(player.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 1.0f);

        new BukkitRunnable() {
            Location currentLoc = eyeLoc.clone();
            Vector velocity = direction.clone().multiply(3.0);
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks > 50) { cancel(); return; }

                currentLoc.add(velocity);

                // 使用可见粒子
                currentLoc.getWorld().spawnParticle(Particle.DRAGON_BREATH, currentLoc, 5, 0.3, 0.3, 0.3, 0.01);
                currentLoc.getWorld().spawnParticle(Particle.PORTAL, currentLoc, 3, 0.2, 0.2, 0.2, 0.01);

                // 检查是否碰到方块
                if (currentLoc.getBlock().getType().isSolid()) {
                    explodeDragonBreath(currentLoc, player);
                    cancel();
                    return;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // 龙息爆炸
    private void explodeDragonBreath(Location location, Player shooter) {
        location.getWorld().spawnParticle(Particle.EXPLOSION, location, 10, 1, 1, 1, 0.5);
        location.getWorld().spawnParticle(Particle.DRAGON_BREATH, location, 50, 4, 4, 4, 0.1);
        location.getWorld().playSound(location, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 1.0f);

        for (Player target : location.getWorld().getPlayers()) {
            if (target == shooter) continue;
            if (target.getLocation().distance(location) <= 8) {
                String shooterTeam = gameManager.getPlayerTeam(shooter);
                String targetTeam = gameManager.getPlayerTeam(target);
                if (shooterTeam != null && targetTeam != null && shooterTeam.equals(targetTeam)) continue;

                target.damage(120, shooter);
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0, true, false));

                dragonBreathVictims.add(target.getUniqueId());
                dragonBreathHitTime.put(target.getUniqueId(), System.currentTimeMillis());
            }
        }

        createDragonBreathCloud(location, shooter);
    }

    // 创建龙息云
    private void createDragonBreathCloud(Location location, Player shooter) {
        AreaEffectCloud cloud = location.getWorld().spawn(location, AreaEffectCloud.class);
        cloud.setRadius(8.0f);
        cloud.setDuration(200);
        cloud.setParticle(Particle.DRAGON_BREATH);
        cloud.setColor(Color.PURPLE);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks >= 200 || cloud.isDead()) {
                    cancel();
                    return;
                }

                for (Player target : cloud.getWorld().getPlayers()) {
                    if (target == shooter) continue;
                    if (target.getLocation().distance(cloud.getLocation()) <= 8) {
                        String shooterTeam = gameManager.getPlayerTeam(shooter);
                        String targetTeam = gameManager.getPlayerTeam(target);
                        if (shooterTeam != null && targetTeam != null && shooterTeam.equals(targetTeam)) continue;

                        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, true, false));
                        dragonBreathVictims.add(target.getUniqueId());
                        dragonBreathHitTime.put(target.getUniqueId(), System.currentTimeMillis());
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    // 检查龙息击杀奖励
    public void onKill(Player killer, Player victim) {
        if (dragonBreathVictims.contains(victim.getUniqueId())) {
            long hitTime = dragonBreathHitTime.getOrDefault(victim.getUniqueId(), 0L);
            if (System.currentTimeMillis() - hitTime <= 3000) {
                gameManager.getExSkillManager().addCharge(killer, 1);
                killer.sendMessage(Component.text("💜 龙息击杀奖励！+1 EX充能", NamedTextColor.DARK_PURPLE));
            }
            dragonBreathVictims.remove(victim.getUniqueId());
            dragonBreathHitTime.remove(victim.getUniqueId());
        }
    }

    // 子弹时间击杀奖励
    public void onBulletTimeKill(Player killer) {
        if (isBulletTimeActive(killer)) {
            UUID uuid = killer.getUniqueId();
            bulletTimeEnd.put(uuid, System.currentTimeMillis() + BULLET_TIME_DURATION);

            int currentAmmo = gameManager.getAmmoManager().getMagazineAmmo(uuid);
            HeroKit kit = gameManager.getPlayerHero(killer);
            if (kit != null) {
                if (currentAmmo < 15) {
                    gameManager.getAmmoManager().setMagazineAmmo(uuid, 15);
                    killer.sendMessage(Component.text("✨ 子弹时间延长！子弹补充至15发", NamedTextColor.LIGHT_PURPLE));
                } else {
                    killer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0, true, false));
                    killer.sendMessage(Component.text("✨ 子弹时间延长！获得速度效果", NamedTextColor.LIGHT_PURPLE));
                }
            }
        }
    }

    private void startDragonBreathChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<UUID, Long>> iterator = dragonBreathHitTime.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<UUID, Long> entry = iterator.next();
                    if (System.currentTimeMillis() - entry.getValue() > 3000) {
                        dragonBreathVictims.remove(entry.getKey());
                        iterator.remove();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("mika")) return;
        if (gameManager.getState() != GameState.ROUND_ACTIVE) return;
        if (!gameManager.isPlayerAlive(player)) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        // 蹲下左键触发小技能
        if (player.isSneaking() && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
            if (isWeapon(item, kit)) {
                event.setCancelled(true);
                triggerSmallSkill(player);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("mika")) return;

        ItemStack item = event.getItemDrop().getItemStack();

        if (player.isSneaking() && item.getType() == kit.getWeaponMaterial()) {
            event.setCancelled(true);
            triggerExPrepare(player);
        }
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("mika")) return;

        // 治疗减少40%
        event.setAmount(event.getAmount() * 0.6);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("mika")) return;

        // 子弹时间内受到伤害减少20%
        if (isBulletTimeActive(player)) {
            event.setDamage(event.getDamage() * 0.8);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            HeroKit kit = gameManager.getPlayerHero(attacker);
            if (kit != null && kit.getId().equals("mika")) {
                if (isBulletTimeActive(attacker)) {
                    event.setDamage(event.getDamage() * 1.1);
                }
            }
        }
    }

    private boolean isWeapon(ItemStack item, HeroKit kit) {
        if (item.getType() == kit.getWeaponMaterial() &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains(kit.getWeaponName())) {
            return true;
        }
        return item.getType() == Material.WOODEN_AXE || item.getType() == Material.IRON_AXE;
    }

    public void resetAll() {
        bulletTimeActive.clear();
        bulletTimeEnd.clear();
        bulletTimeCooldown.clear();
        smallSkillCount.clear();
        exPrepared.clear();
        exSlowEnd.clear();
        dragonBreathVictims.clear();
        dragonBreathHitTime.clear();
    }
}