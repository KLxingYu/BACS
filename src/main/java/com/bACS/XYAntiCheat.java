package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class XYAntiCheat implements Listener {
    private final BACS plugin;
    private final GameManager gameManager;

    private final Map<UUID, Double> suspicionLevel = new HashMap<>();
    private static final double MAX_SUSPICION = 160.0;

    private static final double MAX_ATTACK_DISTANCE = 3.5;
    private static final double MAX_ATTACK_DISTANCE_SNEAK = 4.0;

    private final Map<UUID, Long> flightViolationTime = new HashMap<>();
    private static final long FLIGHT_CHECK_INTERVAL = 250;

    private final Map<UUID, Float> lastPlayerYaw = new HashMap<>();
    private final Map<UUID, Float> lastPlayerPitch = new HashMap<>();
    private final Map<UUID, Long> lastRotationTime = new HashMap<>();
    private final Map<UUID, Boolean> suspiciousRotation = new HashMap<>();
    private static final float MAX_ROTATION_SPEED = 180.0f;

    private final Map<UUID, Location> lastMoveLocation = new HashMap<>();
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();
    private final Map<UUID, Long> speedViolationStart = new HashMap<>();
    private static final long SPEED_CHECK_INTERVAL = 250;
    private final Set<UUID> speedBlocked = new HashSet<>();

    private final Map<UUID, Long> lastJumpTime = new HashMap<>();
    private static final long JUMP_GRACE_PERIOD = 500;

    private final Map<UUID, Long> lastStairsTime = new HashMap<>();
    private static final long STAIRS_GRACE_PERIOD = 300;

    private final Map<UUID, Long> omnidirectionalSprintViolation = new HashMap<>();
    private static final long OMNIDIRECTIONAL_SPRINT_INTERVAL = 250;

    private final Map<UUID, List<Long>> leftClicks = new HashMap<>();
    private final Map<UUID, List<Long>> rightClicks = new HashMap<>();
    private static final int MAX_CPS = 30;
    private static final long CPS_WINDOW = 1000;

    private final Set<UUID> attackBlocked = new HashSet<>();
    private final Map<UUID, Long> attackBlockEnd = new HashMap<>();
    private static final long ATTACK_BLOCK_DURATION = 3000;

    private final Set<UUID> serinaAttackExempt = new HashSet<>();
    private final Map<UUID, Long> serinaExemptTime = new HashMap<>();
    private static final long SERINA_EXEMPT_DURATION = 1500;

    private int totalKicked = 0;
    private File kickLogFile;

    public XYAntiCheat(BACS plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        loadKickCount();
        startAntiCheatTasks();
    }

    private boolean isBypass(Player player) {
        return player.isOp() || player.hasPermission("bacs.admin") || player.hasPermission("xyanticheat.bypass");
    }

    private boolean shouldCheck(Player player) {
        if (isBypass(player)) return false;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return false;
        if (gameManager.getState() != GameState.ROUND_ACTIVE) return false;
        return true;
    }

    private void loadKickCount() {
        kickLogFile = new File(plugin.getDataFolder(), "kick_count.txt");
        if (kickLogFile.exists()) {
            try {
                Scanner scanner = new Scanner(kickLogFile);
                if (scanner.hasNextInt()) totalKicked = scanner.nextInt();
                scanner.close();
            } catch (IOException e) {}
        }
    }

    private void saveKickCount() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            FileWriter writer = new FileWriter(kickLogFile);
            writer.write(String.valueOf(totalKicked));
            writer.close();
        } catch (IOException e) {}
    }

    private void startAntiCheatTasks() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!shouldCheck(player)) continue;
                    checkFlight(player);
                    checkSpeed(player);
                    checkOmnidirectionalSprint(player);
                    checkRotation(player);
                    checkCPS(player);
                    checkAttackBlock(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupOldData();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ============================================================
    // 检查方法
    // ============================================================

    private void checkAttackBlock(Player player) {
        UUID uuid = player.getUniqueId();
        Long blockEnd = attackBlockEnd.get(uuid);
        if (blockEnd != null && System.currentTimeMillis() >= blockEnd) {
            attackBlocked.remove(uuid);
            attackBlockEnd.remove(uuid);
        }
    }

    private void checkFlight(Player player) {
        if (player.isFlying() || player.getAllowFlight()) return;
        if (player.isOnGround()) {
            flightViolationTime.remove(player.getUniqueId());
            return;
        }
        Vector velocity = player.getVelocity();
        double verticalVelocity = velocity.getY();
        UUID uuid = player.getUniqueId();
        Long jumpTime = lastJumpTime.get(uuid);
        long now = System.currentTimeMillis();
        if (jumpTime != null && now - jumpTime < JUMP_GRACE_PERIOD) return;
        if (Math.abs(verticalVelocity) < 0.01 || verticalVelocity > 0.5) {
            if (player.hasPotionEffect(PotionEffectType.JUMP_BOOST)) return;
            Location below = player.getLocation().clone().subtract(0, 0.1, 0);
            if (below.getBlock().getType().isSolid()) return;
            Long violationTime = flightViolationTime.get(uuid);
            if (violationTime == null) {
                flightViolationTime.put(uuid, now);
            } else if (now - violationTime >= FLIGHT_CHECK_INTERVAL) {
                addSuspicion(player, 50.0, "飞行作弊");
                flightViolationTime.put(uuid, now);
                blockAttack(player, "飞行作弊");
            }
        }
    }

    private void checkSpeed(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        Long jumpTime = lastJumpTime.get(uuid);
        if (jumpTime != null && now - jumpTime < JUMP_GRACE_PERIOD) {
            lastMoveLocation.put(uuid, player.getLocation().clone());
            lastMoveTime.put(uuid, now);
            return;
        }

        Location currentLoc = player.getLocation();
        Location lastLoc = lastMoveLocation.get(uuid);
        Long lastTime = lastMoveTime.get(uuid);

        lastMoveLocation.put(uuid, currentLoc.clone());
        lastMoveTime.put(uuid, now);

        if (lastLoc == null || lastTime == null) return;
        if (!lastLoc.getWorld().equals(currentLoc.getWorld())) return;

        boolean isOnStairs = isPlayerOnStairsOrSlabs(player);
        if (isOnStairs) lastStairsTime.put(uuid, now);

        Long stairsTime = lastStairsTime.get(uuid);
        boolean isInStairsGrace = stairsTime != null && now - stairsTime < STAIRS_GRACE_PERIOD;

        double yDiff = currentLoc.getY() - lastLoc.getY();
        boolean isOnIncline = Math.abs(yDiff) > 0.05 && (isOnStairs || isInStairsGrace);

        double horizontalDistance = Math.sqrt(
                Math.pow(currentLoc.getX() - lastLoc.getX(), 2) +
                        Math.pow(currentLoc.getZ() - lastLoc.getZ(), 2)
        );

        double timeDiff = (now - lastTime) / 1000.0;
        if (timeDiff <= 0) return;

        double horizontalSpeed = horizontalDistance / timeDiff;

        double baseSpeed = player.isSprinting() ? 5.612 : 4.317;
        int speedLevel = 0;
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            speedLevel = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
        }
        double speedMultiplier = 1.0 + (speedLevel * 0.24);
        double maxAllowedSpeed = baseSpeed * speedMultiplier * 1.5;

        if (isOnIncline || isOnStairs || isInStairsGrace) maxAllowedSpeed *= 1.3;
        if (!player.isOnGround()) maxAllowedSpeed *= 1.2;

        if (horizontalSpeed > maxAllowedSpeed) {
            Long violationStart = speedViolationStart.get(uuid);
            if (violationStart == null) {
                speedViolationStart.put(uuid, now);
            } else if (now - violationStart >= SPEED_CHECK_INTERVAL) {
                addSuspicion(player, 15.0, "速度异常");
                speedViolationStart.put(uuid, now);
                blockAttack(player, "速度异常");
            }
            if (violationStart != null && now - violationStart >= SPEED_CHECK_INTERVAL * 3 && lastLoc != null) {
                player.teleport(lastLoc);
                lastMoveLocation.put(uuid, lastLoc.clone());
                lastMoveTime.put(uuid, now);
                speedViolationStart.remove(uuid);
                return;
            }
        } else {
            speedViolationStart.remove(uuid);
        }
    }

    private boolean isPlayerOnStairsOrSlabs(Player player) {
        Location loc = player.getLocation();
        Location below = loc.clone().subtract(0, 0.1, 0);
        Location below2 = loc.clone().subtract(0, 0.6, 0);
        Location below3 = loc.clone().subtract(0, 1.1, 0);
        Material[] blocks = {below.getBlock().getType(), below2.getBlock().getType(), below3.getBlock().getType()};
        for (Material blockType : blocks) {
            String name = blockType.name();
            if (name.contains("STAIRS") || name.contains("SLAB") || name.contains("CARPET") || name.contains("SNOW") || name.contains("PATH")) return true;
            if (!blockType.isSolid() && blockType != Material.AIR && blockType != Material.CAVE_AIR && blockType != Material.VOID_AIR) return true;
        }
        return false;
    }

    private void checkOmnidirectionalSprint(Player player) {
        if (!player.isSprinting()) {
            omnidirectionalSprintViolation.remove(player.getUniqueId());
            return;
        }
        UUID uuid = player.getUniqueId();
        Location currentLoc = player.getLocation();
        Location lastLoc = lastMoveLocation.get(uuid);
        if (lastLoc == null) return;
        if (!lastLoc.getWorld().equals(currentLoc.getWorld())) return;
        double moveX = currentLoc.getX() - lastLoc.getX();
        double moveZ = currentLoc.getZ() - lastLoc.getZ();
        if (Math.abs(moveX) < 0.001 && Math.abs(moveZ) < 0.001) return;
        float yaw = currentLoc.getYaw();
        double facingX = -Math.sin(Math.toRadians(yaw));
        double facingZ = Math.cos(Math.toRadians(yaw));
        double dotProduct = (moveX * facingX + moveZ * facingZ) /
                (Math.sqrt(moveX * moveX + moveZ * moveZ) * Math.sqrt(facingX * facingX + facingZ * facingZ));
        if (dotProduct < -0.3) {
            long now = System.currentTimeMillis();
            Long violationTime = omnidirectionalSprintViolation.get(uuid);
            if (violationTime == null) {
                omnidirectionalSprintViolation.put(uuid, now);
            } else if (now - violationTime >= OMNIDIRECTIONAL_SPRINT_INTERVAL) {
                addSuspicion(player, 15.0, "全方向疾跑");
                omnidirectionalSprintViolation.put(uuid, now);
                blockAttack(player, "全方向疾跑");
                if (now - violationTime >= OMNIDIRECTIONAL_SPRINT_INTERVAL * 3 && lastLoc != null) {
                    player.teleport(lastLoc);
                    lastMoveLocation.put(uuid, lastLoc.clone());
                    lastMoveTime.put(uuid, now);
                    omnidirectionalSprintViolation.remove(uuid);
                    return;
                }
            }
        } else {
            omnidirectionalSprintViolation.remove(uuid);
        }
    }

    private void checkRotation(Player player) {
        UUID uuid = player.getUniqueId();
        Location currentLoc = player.getLocation();
        float currentYaw = currentLoc.getYaw();
        float currentPitch = currentLoc.getPitch();
        long now = System.currentTimeMillis();
        Float lastYaw = lastPlayerYaw.get(uuid);
        Float lastPitch = lastPlayerPitch.get(uuid);
        Long lastTime = lastRotationTime.get(uuid);
        lastPlayerYaw.put(uuid, currentYaw);
        lastPlayerPitch.put(uuid, currentPitch);
        lastRotationTime.put(uuid, now);
        if (lastYaw == null || lastPitch == null || lastTime == null) return;
        float yawDiff = Math.abs(currentYaw - lastYaw);
        float pitchDiff = Math.abs(currentPitch - lastPitch);
        if (yawDiff > 180) yawDiff = 360 - yawDiff;
        long tickDiff = (now - lastTime) / 50;
        if (tickDiff <= 0) return;
        float rotationSpeedPerTick = (yawDiff + pitchDiff) / tickDiff;
        if (rotationSpeedPerTick > MAX_ROTATION_SPEED) {
            suspiciousRotation.put(uuid, true);
        } else {
            suspiciousRotation.put(uuid, false);
        }
    }

    private void checkCPS(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        List<Long> leftClickTimes = leftClicks.getOrDefault(uuid, new ArrayList<>());
        List<Long> rightClickTimes = rightClicks.getOrDefault(uuid, new ArrayList<>());
        leftClickTimes.removeIf(time -> now - time > CPS_WINDOW);
        rightClickTimes.removeIf(time -> now - time > CPS_WINDOW);
        int leftCPS = leftClickTimes.size();
        int rightCPS = rightClickTimes.size();
        if (leftCPS > MAX_CPS || rightCPS > MAX_CPS) {
            kickPlayer(player, "点击速度过快 (CPS > " + MAX_CPS + ")");
        }
    }

    // ============================================================
    // 攻击阻止系统
    // ============================================================

    private void blockAttack(Player player, String reason) {
        UUID uuid = player.getUniqueId();
        attackBlocked.add(uuid);
        attackBlockEnd.put(uuid, System.currentTimeMillis() + ATTACK_BLOCK_DURATION);
        player.sendMessage(Component.text("❌ 反作弊系统已暂时阻止你的攻击！", NamedTextColor.RED));
    }

    private boolean isAttackBlocked(Player player) {
        UUID uuid = player.getUniqueId();
        Long blockEnd = attackBlockEnd.get(uuid);
        if (blockEnd != null && System.currentTimeMillis() < blockEnd) {
            return true;
        }
        attackBlocked.remove(uuid);
        attackBlockEnd.remove(uuid);
        return false;
    }

    // ============================================================
    // 神名攻击豁免
    // ============================================================

    private boolean isSerinaWeapon(Player player) {
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit != null && kit.getId().equals("serina")) {
            return true;
        }

        String equippedId = gameManager.getShopManager().getEquippedMainWeapon(player);
        if (equippedId != null && equippedId.equals("serina")) {
            return true;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.hasItemMeta() &&
                mainHand.getItemMeta().hasDisplayName() &&
                mainHand.getItemMeta().displayName().toString().contains("神名")) {
            return true;
        }

        UUID uuid = player.getUniqueId();
        if (serinaAttackExempt.contains(uuid)) {
            Long exemptUntil = serinaExemptTime.get(uuid);
            if (exemptUntil != null && System.currentTimeMillis() < exemptUntil) {
                return true;
            } else {
                serinaAttackExempt.remove(uuid);
                serinaExemptTime.remove(uuid);
            }
        }

        return false;
    }

    public void exemptSerinaAttack(Player player) {
        if (player != null) {
            serinaAttackExempt.add(player.getUniqueId());
            serinaExemptTime.put(player.getUniqueId(), System.currentTimeMillis() + SERINA_EXEMPT_DURATION);
        }
    }

    public boolean isExempted(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();
        if (serinaAttackExempt.contains(uuid)) {
            Long exemptUntil = serinaExemptTime.get(uuid);
            if (exemptUntil != null && System.currentTimeMillis() < exemptUntil) {
                return true;
            } else {
                serinaAttackExempt.remove(uuid);
                serinaExemptTime.remove(uuid);
            }
        }
        return false;
    }

    // ============================================================
    // 怀疑度管理
    // ============================================================

    private void addSuspicion(Player player, double amount, String reason) {
        UUID uuid = player.getUniqueId();
        double current = suspicionLevel.getOrDefault(uuid, 0.0);
        double newLevel = current + amount;
        suspicionLevel.put(uuid, newLevel);
        if (amount >= 20) {
            plugin.getLogger().warning("[XYANTICHEAT] " + player.getName() + " 触发 " + reason + "，怀疑度: " + newLevel);
        }
        if (newLevel >= MAX_SUSPICION) {
            kickPlayer(player, reason);
        }
    }

    private void kickPlayer(Player player, String reason) {
        totalKicked++;
        saveKickCount();
        Bukkit.broadcast(Component.text("[XYANTICHEAT]: " + player.getName() + " 由于被封禁或被踢出而掉线！", NamedTextColor.RED));
        final int kickNumber = totalKicked;
        final String kickReason = reason;
        new BukkitRunnable() {
            @Override
            public void run() {
                player.kick(Component.text(
                        "§c§l您已被[XYanticheat]踢出服务器！\n" +
                                "§7您是被[XYanticheat]处理的第§c" + kickNumber + "§7个人！\n" +
                                "§e请注意！这不是封禁！\n" +
                                "§7请规范您的游戏行为，您可以重新进入服务器。\n" +
                                "§c§l踢出原因: §e" + kickReason
                ));
            }
        }.runTaskLater(plugin, 1L);
    }

    private void cleanupOldData() {
        long now = System.currentTimeMillis();
        for (UUID uuid : leftClicks.keySet()) leftClicks.get(uuid).removeIf(time -> now - time > CPS_WINDOW);
        for (UUID uuid : rightClicks.keySet()) rightClicks.get(uuid).removeIf(time -> now - time > CPS_WINDOW);
        speedViolationStart.entrySet().removeIf(entry -> now - entry.getValue() > 5000);
        flightViolationTime.entrySet().removeIf(entry -> now - entry.getValue() > 5000);
        omnidirectionalSprintViolation.entrySet().removeIf(entry -> now - entry.getValue() > 5000);
        lastJumpTime.entrySet().removeIf(entry -> now - entry.getValue() > 5000);
        lastStairsTime.entrySet().removeIf(entry -> now - entry.getValue() > 5000);
        attackBlockEnd.entrySet().removeIf(entry -> now > entry.getValue());
        attackBlocked.removeIf(uuid -> !attackBlockEnd.containsKey(uuid));
        serinaExemptTime.entrySet().removeIf(entry -> now > entry.getValue());
        serinaAttackExempt.removeIf(uuid -> !serinaExemptTime.containsKey(uuid));
    }

    // ============================================================
    // 事件处理
    // ============================================================

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!shouldCheck(attacker)) return;

        if (isSerinaWeapon(attacker)) {
            serinaAttackExempt.add(attacker.getUniqueId());
            serinaExemptTime.put(attacker.getUniqueId(), System.currentTimeMillis() + SERINA_EXEMPT_DURATION);
            return;
        }

        if (isAttackBlocked(attacker)) {
            event.setCancelled(true);
            attacker.sendMessage(Component.text("❌ 你的攻击已被反作弊系统阻止！", NamedTextColor.RED));
            return;
        }

        // ★ 远程武器攻击豁免（加入怒焰、手炮）
        ItemStack mainHand = attacker.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.hasItemMeta() && mainHand.getItemMeta().hasDisplayName()) {
            String displayName = mainHand.getItemMeta().displayName().toString();
            if (displayName.contains("制式手枪") || displayName.contains("灵异") || displayName.contains("裁决")
                    || displayName.contains("怒焰") || displayName.contains("手炮")) {
                return;
            }
            for (HeroKit kit : GameManager.HERO_KITS) {
                if (kit.getWeaponMaterial() == Material.AIR) continue;
                if (mainHand.getType() == kit.getWeaponMaterial() && displayName.contains(kit.getWeaponName())) {
                    return;
                }
            }
        }

        double maxDistance = attacker.isSneaking() ? MAX_ATTACK_DISTANCE_SNEAK : MAX_ATTACK_DISTANCE;
        double actualDistance = attacker.getLocation().distance(victim.getLocation());
        if (actualDistance > maxDistance) {
            event.setCancelled(true);
            addSuspicion(attacker, 10.0, "攻击距离异常");
            blockAttack(attacker, "攻击距离异常");
        }

        UUID uuid = attacker.getUniqueId();
        if (suspiciousRotation.getOrDefault(uuid, false)) {
            event.setCancelled(true);
            addSuspicion(attacker, 20.0, "转头速度异常");
            blockAttack(attacker, "转头速度异常");
            suspiciousRotation.put(uuid, false);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!shouldCheck(player)) return;

        if (isSerinaWeapon(player)) {
            return;
        }

        if (isAttackBlocked(player)) {
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
                return;
            }
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            leftClicks.computeIfAbsent(uuid, k -> new ArrayList<>()).add(now);
        } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            rightClicks.computeIfAbsent(uuid, k -> new ArrayList<>()).add(now);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!shouldCheck(player)) return;
        if (event.getFrom().getY() < event.getTo().getY() && !player.isOnGround() && event.getFrom().getY() - event.getFrom().getBlockY() < 0.1) {
            lastJumpTime.put(player.getUniqueId(), System.currentTimeMillis());
        }
        Vector velocity = player.getVelocity();
        if (velocity.getY() > 0.3 && velocity.getY() < 0.6) {
            lastJumpTime.put(player.getUniqueId(), System.currentTimeMillis());
        }
        if (isPlayerOnStairsOrSlabs(player)) {
            lastStairsTime.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        suspicionLevel.put(player.getUniqueId(), 0.0);
        lastMoveLocation.put(player.getUniqueId(), player.getLocation().clone());
        lastMoveTime.put(player.getUniqueId(), System.currentTimeMillis());
        lastPlayerYaw.put(player.getUniqueId(), player.getLocation().getYaw());
        lastPlayerPitch.put(player.getUniqueId(), player.getLocation().getPitch());
        lastRotationTime.put(player.getUniqueId(), System.currentTimeMillis());
        leftClicks.remove(player.getUniqueId());
        rightClicks.remove(player.getUniqueId());
        flightViolationTime.remove(player.getUniqueId());
        speedViolationStart.remove(player.getUniqueId());
        speedBlocked.remove(player.getUniqueId());
        omnidirectionalSprintViolation.remove(player.getUniqueId());
        lastJumpTime.remove(player.getUniqueId());
        lastStairsTime.remove(player.getUniqueId());
        attackBlocked.remove(player.getUniqueId());
        attackBlockEnd.remove(player.getUniqueId());
        serinaAttackExempt.remove(player.getUniqueId());
        serinaExemptTime.remove(player.getUniqueId());
        suspiciousRotation.put(player.getUniqueId(), false);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        suspicionLevel.remove(uuid);
        lastMoveLocation.remove(uuid);
        lastMoveTime.remove(uuid);
        lastPlayerYaw.remove(uuid);
        lastPlayerPitch.remove(uuid);
        lastRotationTime.remove(uuid);
        leftClicks.remove(uuid);
        rightClicks.remove(uuid);
        flightViolationTime.remove(uuid);
        speedViolationStart.remove(uuid);
        speedBlocked.remove(uuid);
        omnidirectionalSprintViolation.remove(uuid);
        lastJumpTime.remove(uuid);
        lastStairsTime.remove(uuid);
        attackBlocked.remove(uuid);
        attackBlockEnd.remove(uuid);
        serinaAttackExempt.remove(uuid);
        serinaExemptTime.remove(uuid);
        suspiciousRotation.remove(uuid);
    }

    public double getSuspicion(Player player) {
        return suspicionLevel.getOrDefault(player.getUniqueId(), 0.0);
    }

    public void resetSuspicion(Player player) {
        suspicionLevel.put(player.getUniqueId(), 0.0);
    }

    public boolean isAttackBlockedByAntiCheat(Player player) {
        return isAttackBlocked(player);
    }

    public void resetAttackBlock(Player player) {
        UUID uuid = player.getUniqueId();
        attackBlocked.remove(uuid);
        attackBlockEnd.remove(uuid);
    }

    public int getTotalKicked() {
        return totalKicked;
    }
}