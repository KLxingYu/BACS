package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class RakuFuSkillListener implements Listener {
    private final BACS plugin;
    private final GameManager gameManager;

    // ===== 被动：不灭 =====
    private final Map<UUID, Boolean> undyingTriggered = new HashMap<>();
    private final Map<UUID, Boolean> isUndyingActive = new HashMap<>();
    private final Map<UUID, Player> lastDamager = new HashMap<>();
    private final Map<UUID, Integer> undyingTaskId = new HashMap<>();
    private final Set<UUID> healedDuringUndying = new HashSet<>();

    // ===== 箭矢系统 =====
    private final Map<UUID, Integer> arrowCount = new HashMap<>();
    private final Map<UUID, Arrow> firedArrow = new HashMap<>();
    private final Map<UUID, Integer> arrowTaskId = new HashMap<>();
    private static final int MAX_ARROWS = 2;
    private static final int DEFAULT_ARROWS = 1;
    private static final int ARROW_SCAN_RADIUS = 25;
    private static final int ARROW_SCAN_MAX_TICKS = 88;   // ★ 4.4 秒保护

    // ★ 探测发光记录
    private final Map<UUID, Long> glowingUntil = new HashMap<>();

    // ===== EX技能 =====
    private final Map<UUID, Boolean> exActive = new HashMap<>();
    private final Map<UUID, Integer> exTaskId = new HashMap<>();

    public RakuFuSkillListener(BACS plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    // ============================================================
    // 被动：不灭
    // ============================================================

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("rakufu")) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return;
        }

        UUID uuid = player.getUniqueId();

        if (isUndyingActive.getOrDefault(uuid, false)) {
            event.setCancelled(true);
            return;
        }

        if (undyingTriggered.getOrDefault(uuid, false)) return;

        double finalDamage = event.getFinalDamage();
        if (player.getHealth() - finalDamage > 0) return;

        event.setCancelled(true);
        triggerUndying(player);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        HeroKit kit = gameManager.getPlayerHero(victim);
        if (kit == null || !kit.getId().equals("rakufu")) return;

        if (isUndyingActive.getOrDefault(victim.getUniqueId(), false)) {
            lastDamager.put(victim.getUniqueId(), damager);
        }
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("rakufu")) return;

        // ★ 排除原版自动回血
        EntityRegainHealthEvent.RegainReason reason = event.getRegainReason();
        if (reason == EntityRegainHealthEvent.RegainReason.REGEN ||
                reason == EntityRegainHealthEvent.RegainReason.SATIATED ||
                reason == EntityRegainHealthEvent.RegainReason.EATING) {
            return;
        }

        UUID uuid = player.getUniqueId();

        if (isUndyingActive.getOrDefault(uuid, false)) {
            if (healedDuringUndying.contains(uuid)) return;
            healedDuringUndying.add(uuid);

            if (undyingTaskId.containsKey(uuid)) {
                Bukkit.getScheduler().cancelTask(undyingTaskId.get(uuid));
                undyingTaskId.remove(uuid);
            }
            isUndyingActive.put(uuid, false);
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            lastDamager.remove(uuid);
            player.sendMessage(Component.text("💚 你被治愈了！不灭效果已解除！", NamedTextColor.GREEN));
        }
    }

    private void triggerUndying(Player player) {
        UUID uuid = player.getUniqueId();

        healedDuringUndying.remove(uuid);

        undyingTriggered.put(uuid, true);
        isUndyingActive.put(uuid, true);

        player.setHealth(1.0);

        // ★ 只给 RESISTANCE，不给 REGENERATION
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 4, true, false));

        player.sendMessage(Component.text("💀 不灭触发！3秒后死亡...", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);

        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    isUndyingActive.put(uuid, false);
                    cancel();
                    return;
                }

                isUndyingActive.put(uuid, false);
                player.removePotionEffect(PotionEffectType.RESISTANCE);
                player.removePotionEffect(PotionEffectType.REGENERATION);

                // 被治疗过 -> 解除不灭
                if (healedDuringUndying.contains(uuid)) {
                    player.sendMessage(Component.text("💚 你被治愈了！不灭效果已解除！", NamedTextColor.GREEN));
                    healedDuringUndying.remove(uuid);
                    undyingTaskId.remove(uuid);
                    lastDamager.remove(uuid);
                    return;
                }

                // 3s 到了 -> 强制死亡
                Player damager = lastDamager.get(uuid);
                if (damager != null && damager.isOnline()) {
                    damager.sendMessage(Component.text("💀 " + player.getName() + " 的不灭效果结束，你击杀了TA！", NamedTextColor.RED));
                }
                player.setHealth(0);

                lastDamager.remove(uuid);
                undyingTaskId.remove(uuid);
            }
        }.runTaskLater(plugin, 60L).getTaskId();

        undyingTaskId.put(uuid, taskId);
    }

    // ============================================================
    // 主动技能：弓箭探测
    // ============================================================

    public void giveArrow(Player player) {
        UUID uuid = player.getUniqueId();
        int count = arrowCount.getOrDefault(uuid, DEFAULT_ARROWS);
        if (count <= 0) return;

        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        meta.displayName(Component.text("🏹 探测箭矢", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true));
        meta.lore(List.of(
                Component.text("右键发射", NamedTextColor.GRAY),
                Component.text("探测25格内敌人", NamedTextColor.YELLOW),
                Component.text("不可丢弃/移动", NamedTextColor.DARK_GRAY)
        ));
        meta.setUnbreakable(true);
        arrow.setItemMeta(meta);

        player.getInventory().setItem(6, arrow);
    }

    public void resetArrow(Player player) {
        UUID uuid = player.getUniqueId();
        arrowCount.put(uuid, DEFAULT_ARROWS);
        if (firedArrow.containsKey(uuid)) {
            Arrow old = firedArrow.get(uuid);
            if (old != null && !old.isDead()) {
                old.remove();
            }
            firedArrow.remove(uuid);
        }
        if (arrowTaskId.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(arrowTaskId.get(uuid));
            arrowTaskId.remove(uuid);
        }
        giveArrow(player);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("rakufu")) return;
        if (gameManager.getState() != GameState.ROUND_ACTIVE) return;
        if (!gameManager.isPlayerAlive(player)) return;

        if (item.getType() == Material.ARROW &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().displayName().toString().contains("探测箭矢")) {

            if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR ||
                    event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                fireArrow(player);
            }
        }
    }

    private void fireArrow(Player player) {
        UUID uuid = player.getUniqueId();

        int count = arrowCount.getOrDefault(uuid, 0);
        if (count <= 0) {
            player.sendMessage(Component.text("❌ 没有箭矢！", NamedTextColor.RED));
            return;
        }

        if (firedArrow.containsKey(uuid)) {
            Arrow old = firedArrow.get(uuid);
            if (old != null && !old.isDead()) {
                player.sendMessage(Component.text("❌ 已有探测箭矢！", NamedTextColor.RED));
                return;
            }
            firedArrow.remove(uuid);
        }

        arrowCount.put(uuid, count - 1);
        player.getInventory().removeItem(player.getInventory().getItem(6));

        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        arrow.setDamage(0);
        arrow.setGravity(true);
        arrow.setCritical(false);

        arrow.setCustomName("探测箭矢");
        arrow.setCustomNameVisible(false);

        firedArrow.put(uuid, arrow);

        player.sendMessage(Component.text("🏹 探测箭矢已发射！", NamedTextColor.AQUA));

        startArrowScan(player, arrow);
    }

    /**
     * ★ 重写：明确的阶段控制 + 4.4s 保护 + 完成标记
     */
    private void startArrowScan(Player player, Arrow arrow) {
        UUID uuid = player.getUniqueId();

        if (arrowTaskId.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(arrowTaskId.get(uuid));
            arrowTaskId.remove(uuid);
        }

        int taskId = new BukkitRunnable() {
            int phase = 0;
            boolean hasLanded = false;
            Location landLoc = null;
            int ticks = 0;
            boolean finished = false;

            @Override
            public void run() {
                // 已完成，直接取消
                if (finished) {
                    cancel();
                    return;
                }

                ticks++;

                // ★ 4.4 秒保护
                if (ticks > ARROW_SCAN_MAX_TICKS) {
                    finished = true;
                    cleanupGlowing();
                    cleanupArrow(player);
                    cancel();
                    return;
                }

                // 箭矢已消失且还没落地 -> 直接清理
                if ((arrow.isDead() || !arrow.isValid()) && !hasLanded) {
                    finished = true;
                    cleanupArrow(player);
                    cancel();
                    return;
                }

                // 阶段 0：等待落地
                if (!hasLanded) {
                    if (arrow.isOnGround() || arrow.getLocation().getBlock().getType().isSolid()) {
                        hasLanded = true;
                        landLoc = arrow.getLocation().clone();
                        phase = 1;
                        player.sendMessage(Component.text("🔍 箭矢已落地，1s后开始探测...", NamedTextColor.YELLOW));
                    }
                    return;
                }

                // 阶段 1：第一次探测
                if (phase == 1) {
                    phase = 2;
                    scanForPlayers(player, landLoc);
                    player.sendMessage(Component.text("🔍 第一次探测完成！2.4s后第二次探测", NamedTextColor.YELLOW));
                    return;
                }

                // 阶段 2：第二次探测 + 清理
                if (phase == 2) {
                    phase = 3;
                    scanForPlayers(player, landLoc);
                    player.sendMessage(Component.text("🔍 第二次探测完成！", NamedTextColor.YELLOW));

                    if (!arrow.isDead()) {
                        arrow.remove();
                    }

                    finished = true;
                    cancel();

                    // 延时 1 秒清除发光
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            cleanupGlowing();
                            cleanupArrow(player);
                        }
                    }.runTaskLater(plugin, 20L);
                    return;
                }

                // 兜底：理论上不会到这里
                finished = true;
                cleanupGlowing();
                cleanupArrow(player);
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();

        arrowTaskId.put(uuid, taskId);
    }

    private void scanForPlayers(Player shooter, Location arrowLoc) {
        String shooterTeam = gameManager.getPlayerTeam(shooter);

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(shooter)) continue;
            if (!gameManager.isPlayerAlive(target)) continue;

            String targetTeam = gameManager.getPlayerTeam(target);
            if (targetTeam == null || shooterTeam == null || targetTeam.equals(shooterTeam)) continue;

            if (target.getLocation().distance(arrowLoc) > ARROW_SCAN_RADIUS) continue;
            if (!hasLineOfSight(arrowLoc, target)) continue;

            // 先移除旧发光，再给新的
            target.removePotionEffect(PotionEffectType.GLOWING);
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, true, false));
            glowingUntil.put(target.getUniqueId(), System.currentTimeMillis() + 1000);

            target.getWorld().spawnParticle(Particle.INSTANT_EFFECT, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            target.playSound(target.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 2.0f);
        }

        arrowLoc.getWorld().spawnParticle(Particle.END_ROD, arrowLoc, 30, 0.5, 0.5, 0.5, 0.1);
        arrowLoc.getWorld().playSound(arrowLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
    }

    private void cleanupGlowing() {
        for (UUID targetId : new HashSet<>(glowingUntil.keySet())) {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null && target.isOnline()) {
                target.removePotionEffect(PotionEffectType.GLOWING);
            }
        }
        glowingUntil.clear();
    }

    private boolean hasLineOfSight(Location from, Player target) {
        Location targetLoc = target.getEyeLocation();
        Vector direction = targetLoc.toVector().subtract(from.toVector()).normalize();
        double distance = from.distance(targetLoc);

        for (double i = 0; i < distance; i += 0.5) {
            Location check = from.clone().add(direction.clone().multiply(i));
            if (check.getBlock().getType().isSolid()) {
                return false;
            }
        }
        return true;
    }

    private void cleanupArrow(Player player) {
        UUID uuid = player.getUniqueId();
        if (arrowTaskId.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(arrowTaskId.get(uuid));
            arrowTaskId.remove(uuid);
        }
        if (firedArrow.containsKey(uuid)) {
            Arrow arrow = firedArrow.get(uuid);
            if (arrow != null && !arrow.isDead()) {
                arrow.remove();
            }
            firedArrow.remove(uuid);
        }
    }

    // ============================================================
    // EX技能
    // ============================================================

    public void triggerEX(Player player) {
        UUID uuid = player.getUniqueId();

        if (gameManager.getExSkillManager().getCharge(player) < 5) {
            player.sendMessage(Component.text("❌ EX充能不足！需要5点", NamedTextColor.RED));
            return;
        }

        if (exActive.getOrDefault(uuid, false)) {
            player.sendMessage(Component.text("❌ EX技能已激活！", NamedTextColor.RED));
            return;
        }

        gameManager.getExSkillManager().clearCharge(player);
        exActive.put(uuid, true);

        Location startLoc = player.getLocation().clone();

        for (int i = 0; i < 30; i++) {
            double radius = 2 + Math.random() * 3;
            double angle = Math.random() * 2 * Math.PI;
            double height = Math.random() * 3;
            Location effectLoc = startLoc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
            );
            player.getWorld().spawnParticle(Particle.INSTANT_EFFECT, effectLoc, 10, 0, 0, 0, 0.5);
        }
        player.getWorld().playSound(startLoc, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.0f);
        player.sendMessage(Component.text("🌀 EX技能蓄力中...2s后发射！", NamedTextColor.BLUE)
                .decoration(TextDecoration.BOLD, true));

        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !gameManager.isPlayerAlive(player)) {
                    exActive.put(uuid, false);
                    cancel();
                    return;
                }

                Location loc = player.getLocation().clone().add(0, 1, 0);

                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 10, 1, 1, 1, 0.5);
                loc.getWorld().spawnParticle(Particle.INSTANT_EFFECT, loc, 50, 3, 3, 3, 0.5);
                loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 1.0f);

                boolean hit = false;

                for (Player target : loc.getWorld().getPlayers()) {
                    if (target.equals(player)) continue;
                    if (!gameManager.isPlayerAlive(target)) continue;

                    String shooterTeam = gameManager.getPlayerTeam(player);
                    String targetTeam = gameManager.getPlayerTeam(target);
                    if (shooterTeam != null && targetTeam != null && shooterTeam.equals(targetTeam)) continue;

                    if (target.getLocation().distance(loc) > 8) continue;
                    if (!hasLineOfSight(loc, target)) continue;

                    hit = true;
                    target.damage(40, player);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 3, true, false));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, true, false));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 160, 0, true, false));

                    target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                    target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);

                    player.sendMessage(Component.text("💢 EX技能命中 " + target.getName() + "！", NamedTextColor.RED));
                }

                if (!hit) {
                    gameManager.getExSkillManager().addCharge(player, 1);
                    player.sendMessage(Component.text("❌ EX技能未命中，返还1点充能", NamedTextColor.YELLOW));
                }

                exActive.put(uuid, false);
            }
        }.runTaskLater(plugin, 40L).getTaskId();

        exTaskId.put(uuid, taskId);
    }

    // ============================================================
    // 重置
    // ============================================================

    public void onRoundStart(Player player) {
        UUID uuid = player.getUniqueId();
        undyingTriggered.put(uuid, false);
        isUndyingActive.put(uuid, false);
        healedDuringUndying.remove(uuid);
        lastDamager.remove(uuid);
        if (undyingTaskId.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(undyingTaskId.get(uuid));
            undyingTaskId.remove(uuid);
        }
        exActive.put(uuid, false);
        if (exTaskId.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(exTaskId.get(uuid));
            exTaskId.remove(uuid);
        }
        resetArrow(player);
        giveArrow(player);
    }

    public void onPlayerDeath(Player player) {
        UUID uuid = player.getUniqueId();
        isUndyingActive.put(uuid, false);
        healedDuringUndying.remove(uuid);
        if (undyingTaskId.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(undyingTaskId.get(uuid));
            undyingTaskId.remove(uuid);
        }
        exActive.put(uuid, false);
        if (exTaskId.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(exTaskId.get(uuid));
            exTaskId.remove(uuid);
        }
        cleanupArrow(player);
    }

    public void resetAll() {
        undyingTriggered.clear();
        isUndyingActive.clear();
        lastDamager.clear();
        undyingTaskId.clear();
        healedDuringUndying.clear();
        arrowCount.clear();
        firedArrow.clear();
        arrowTaskId.clear();
        exActive.clear();
        exTaskId.clear();
        glowingUntil.clear();
    }

    public boolean isUndyingActive(Player player) {
        return isUndyingActive.getOrDefault(player.getUniqueId(), false);
    }

    public int getArrowCount(Player player) {
        return arrowCount.getOrDefault(player.getUniqueId(), DEFAULT_ARROWS);
    }

    public void setArrowCount(Player player, int count) {
        arrowCount.put(player.getUniqueId(), Math.min(MAX_ARROWS, Math.max(0, count)));
    }

    public void addArrow(Player player) {
        UUID uuid = player.getUniqueId();
        int current = arrowCount.getOrDefault(uuid, DEFAULT_ARROWS);
        if (current >= MAX_ARROWS) return;
        arrowCount.put(uuid, current + 1);
        giveArrow(player);
    }
}