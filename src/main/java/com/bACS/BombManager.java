package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BombManager {
    private final BACS plugin;
    private final GameManager gameManager;

    private UUID bombHolder = null;
    private Location droppedBombLoc = null;
    private Location deployedBombLoc = null;
    private boolean bombDeployed = false;
    private boolean bombExploded = false;
    private int defuseStage = 0;
    private int defuseProgress = 0;
    private boolean isDefusing = false;
    private UUID defusingPlayer = null;
    private int bombTimer = 40;
    private int bombTaskId = -1;
    private int compassTaskId = -1;
    private int defuseTaskId = -1;

    // ===== 炸弹部署状态 =====
    private final Map<UUID, Boolean> isPlanting = new HashMap<>();
    private final Map<UUID, Integer> plantTaskId = new HashMap<>();
    private final Map<UUID, Integer> plantProgress = new HashMap<>();
    private static final int PLANT_REQUIRED_TICKS = 80;
    private static final int PLANT_ALERT_RADIUS = 26;

    private final Map<UUID, Integer> defuseProgressMap = new HashMap<>();

    public BombManager(BACS plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    public boolean isBombDeployed() {
        return bombDeployed;
    }

    public void giveRoundItems() {
        bombHolder = null;
        droppedBombLoc = null;
        deployedBombLoc = null;
        bombDeployed = false;
        bombExploded = false;
        defuseStage = 0;
        defuseProgress = 0;
        isDefusing = false;
        defusingPlayer = null;
        bombTimer = 40;
        defuseProgressMap.clear();
        isPlanting.clear();
        plantProgress.clear();

        List<Player> attackers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (gameManager.getPlayerTeam(player) != null &&
                    gameManager.getPlayerTeam(player).equals(GameManager.TEAM_ATTACKER) &&
                    gameManager.isPlayerAlive(player)) {
                attackers.add(player);
            }
        }

        if (attackers.isEmpty()) return;

        Player bombCarrier = attackers.get(new Random().nextInt(attackers.size()));
        bombHolder = bombCarrier.getUniqueId();
        giveBombItem(bombCarrier);

        for (Player attacker : attackers) {
            giveCompass(attacker);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (gameManager.getPlayerTeam(player) != null &&
                    gameManager.getPlayerTeam(player).equals(GameManager.TEAM_DEFENDER) &&
                    gameManager.isPlayerAlive(player)) {
                giveDefuser(player);
            }
        }

        startCompassUpdater();
    }

    private void giveBombItem(Player player) {
        ItemStack bomb = new ItemStack(Material.QUARTZ);
        ItemMeta meta = bomb.getItemMeta();
        meta.displayName(Component.text("炸药", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
        meta.lore(List.of(
                Component.text("前往A/B/C包点部署", NamedTextColor.GRAY),
                Component.text("在石英块上右键", NamedTextColor.YELLOW),
                Component.text("然后蹲下4秒部署", NamedTextColor.GOLD),
                Component.text("拆除需要7秒，分2阶段", NamedTextColor.LIGHT_PURPLE),
                Component.text("可丢弃，不可被移动", NamedTextColor.DARK_GRAY)
        ));
        meta.setUnbreakable(true);
        bomb.setItemMeta(meta);

        player.getInventory().setItem(3, bomb);
        player.sendMessage(Component.text("💣 你获得了炸药！", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
    }

    private void giveCompass(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        meta.displayName(Component.text("炸药追踪器", NamedTextColor.AQUA));
        meta.lore(List.of(
                Component.text("指向炸药位置", NamedTextColor.GRAY),
                Component.text("不可丢弃", NamedTextColor.DARK_GRAY)
        ));
        meta.setUnbreakable(true);
        compass.setItemMeta(meta);

        player.getInventory().setItem(4, compass);
    }

    private void giveDefuser(Player player) {
        ItemStack shears = new ItemStack(Material.SHEARS);
        ItemMeta meta = shears.getItemMeta();
        meta.displayName(Component.text("拆弹器", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        meta.lore(List.of(
                Component.text("右键阳光传感器拆除炸弹", NamedTextColor.GRAY),
                Component.text("拆除需要7秒，分2阶段", NamedTextColor.LIGHT_PURPLE),
                Component.text("不可丢弃", NamedTextColor.DARK_GRAY)
        ));
        meta.setUnbreakable(true);
        shears.setItemMeta(meta);

        player.getInventory().setItem(3, shears);
    }

    private void startCompassUpdater() {
        if (compassTaskId != -1) {
            Bukkit.getScheduler().cancelTask(compassTaskId);
        }

        compassTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            Location targetLoc = getBombLocation();
            if (targetLoc == null) return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (gameManager.getPlayerTeam(player) == null ||
                        !gameManager.getPlayerTeam(player).equals(GameManager.TEAM_ATTACKER)) {
                    continue;
                }

                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() == Material.COMPASS) {
                        CompassMeta meta = (CompassMeta) item.getItemMeta();
                        if (meta != null && meta.hasDisplayName() &&
                                meta.displayName().toString().contains("炸药追踪器")) {
                            meta.setLodestone(targetLoc);
                            item.setItemMeta(meta);
                        }
                    }
                }
            }
        }, 0L, 10L);
    }

    private Location getBombLocation() {
        if (bombDeployed && deployedBombLoc != null) {
            return deployedBombLoc;
        }
        if (droppedBombLoc != null) {
            return droppedBombLoc;
        }
        if (bombHolder != null) {
            Player holder = Bukkit.getPlayer(bombHolder);
            if (holder != null && holder.isOnline()) {
                return holder.getLocation();
            }
        }
        return null;
    }

    public void onBombDropped(Player player, ItemStack item) {
        if (item.getType() != Material.QUARTZ) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().displayName().toString().contains("炸药")) return;
        if (bombDeployed) return;

        droppedBombLoc = player.getLocation().clone();
        bombHolder = null;

        droppedBombLoc.getWorld().spawnParticle(Particle.END_ROD, droppedBombLoc, 20, 0.5, 0.5, 0.5, 0.1);
        droppedBombLoc.getWorld().playSound(droppedBombLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
    }

    public void onBombDeploy(Player player) {
        if (bombHolder == null || !bombHolder.equals(player.getUniqueId())) return;
        if (bombDeployed) return;

        Location loc = player.getLocation();
        Location below = loc.clone().subtract(0, 1, 0);
        if (below.getBlock().getType() != Material.QUARTZ_BLOCK) {
            player.sendMessage(Component.text("❌ 必须在石英块上部署炸药！", NamedTextColor.RED));
            return;
        }

        player.getInventory().setItem(3, null);

        ItemStack sensor = new ItemStack(Material.DAYLIGHT_DETECTOR);
        ItemMeta meta = sensor.getItemMeta();
        meta.displayName(Component.text("💣 炸弹部署器", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        meta.lore(List.of(
                Component.text("蹲下4秒部署炸弹", NamedTextColor.GRAY),
                Component.text("移动或取消蹲下将中断部署", NamedTextColor.YELLOW),
                Component.text("不可使用/放置", NamedTextColor.DARK_GRAY)
        ));
        meta.setUnbreakable(true);
        sensor.setItemMeta(meta);
        player.getInventory().setItem(3, sensor);

        player.sendMessage(Component.text("💣 蹲下4秒以部署炸弹！", NamedTextColor.GOLD));
        player.sendMessage(Component.text("⚠ 移动或取消蹲下将中断部署！", NamedTextColor.YELLOW));
    }

    public void startPlanting(Player player) {
        UUID uuid = player.getUniqueId();

        if (isPlanting.getOrDefault(uuid, false)) return;

        ItemStack item = player.getInventory().getItem(3);
        if (item == null || item.getType() != Material.DAYLIGHT_DETECTOR) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().displayName().toString().contains("炸弹部署器")) return;

        Location below = player.getLocation().clone().subtract(0, 1, 0);
        if (below.getBlock().getType() != Material.QUARTZ_BLOCK) {
            player.sendMessage(Component.text("❌ 必须在石英块上部署！", NamedTextColor.RED));
            return;
        }

        if (player.isSprinting() || player.isSwimming() || player.isFlying()) {
            player.sendMessage(Component.text("❌ 部署时不能疾跑/游泳/飞行！", NamedTextColor.RED));
            return;
        }

        isPlanting.put(uuid, true);
        plantProgress.put(uuid, 0);

        broadcastPlantAlert(player);

        player.sendMessage(Component.text("⏳ 开始部署炸弹... 保持蹲下4秒", NamedTextColor.YELLOW));

        int taskId = new BukkitRunnable() {
            Location startLoc = player.getLocation().clone();
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelPlant(player);
                    cancel();
                    return;
                }

                if (!player.isSneaking()) {
                    player.sendMessage(Component.text("❌ 部署中断！取消蹲下", NamedTextColor.RED));
                    cancelPlant(player);
                    cancel();
                    return;
                }

                if (player.getLocation().distance(startLoc) > 0.3) {
                    player.sendMessage(Component.text("❌ 部署中断！移动了位置", NamedTextColor.RED));
                    cancelPlant(player);
                    cancel();
                    return;
                }

                ItemStack currentItem = player.getInventory().getItem(3);
                if (currentItem == null || currentItem.getType() != Material.DAYLIGHT_DETECTOR) {
                    player.sendMessage(Component.text("❌ 部署中断！丢失了部署器", NamedTextColor.RED));
                    cancelPlant(player);
                    cancel();
                    return;
                }

                Location currentBelow = player.getLocation().clone().subtract(0, 1, 0);
                if (currentBelow.getBlock().getType() != Material.QUARTZ_BLOCK) {
                    player.sendMessage(Component.text("❌ 部署中断！离开了石英块", NamedTextColor.RED));
                    cancelPlant(player);
                    cancel();
                    return;
                }

                ticks++;
                int progress = (ticks * 100) / PLANT_REQUIRED_TICKS;
                plantProgress.put(uuid, ticks);

                String progressBar = createProgressBar(progress);
                player.sendActionBar(Component.text("💣 部署炸弹: " + progressBar + " " + progress + "%", NamedTextColor.GOLD));

                if (ticks % 20 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f + (ticks / 20) * 0.1f);
                    player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.01);
                }

                if (ticks >= PLANT_REQUIRED_TICKS) {
                    deployBomb(player);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L).getTaskId();

        plantTaskId.put(uuid, taskId);
    }

    private void broadcastPlantAlert(Player planter) {
        Location planterLoc = planter.getLocation();

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.isOnline()) continue;
            if (target.equals(planter)) continue;

            Location targetLoc = target.getLocation();
            if (!planterLoc.getWorld().equals(targetLoc.getWorld())) continue;

            double distance = targetLoc.distance(planterLoc);
            if (distance > PLANT_ALERT_RADIUS) continue;

            float volume = (float) Math.max(0.2, 1.0 - (distance / PLANT_ALERT_RADIUS) * 0.8);
            float pitch = (float) Math.max(1.0, 2.0 - (distance / PLANT_ALERT_RADIUS) * 1.0);

            target.playSound(planterLoc, Sound.BLOCK_NOTE_BLOCK_PLING, volume, pitch);

            if (distance < 12) {
                target.playSound(planterLoc, Sound.BLOCK_NOTE_BLOCK_BELL, volume * 0.7f, pitch + 0.3f);
            }

            if (distance < 6) {
                target.playSound(planterLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, volume * 0.5f, pitch + 0.5f);
            }
        }

        planter.playSound(planterLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 2.0f);
        planter.playSound(planterLoc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 2.3f);
        planter.playSound(planterLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 2.5f);
    }

    private String createProgressBar(int progress) {
        int bars = 20;
        int filled = (progress * bars) / 100;
        StringBuilder sb = new StringBuilder();
        sb.append("§a");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                sb.append("█");
            } else {
                sb.append("§7█");
            }
        }
        return sb.toString();
    }

    public void cancelPlant(Player player) {
        UUID uuid = player.getUniqueId();
        isPlanting.put(uuid, false);
        plantProgress.remove(uuid);
        Integer taskId = plantTaskId.remove(uuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private void deployBomb(Player player) {
        UUID uuid = player.getUniqueId();

        ItemStack item = player.getInventory().getItem(3);
        if (item == null || item.getType() != Material.DAYLIGHT_DETECTOR) {
            player.sendMessage(Component.text("❌ 部署失败！", NamedTextColor.RED));
            cancelPlant(player);
            return;
        }

        Location loc = player.getLocation();
        Location below = loc.clone().subtract(0, 1, 0);
        if (below.getBlock().getType() != Material.QUARTZ_BLOCK) {
            player.sendMessage(Component.text("❌ 部署失败！", NamedTextColor.RED));
            cancelPlant(player);
            return;
        }

        player.getInventory().setItem(3, null);

        deployedBombLoc = below.getBlock().getLocation().clone().add(0, 1, 0);
        bombDeployed = true;
        bombTimer = 40;
        bombHolder = null;
        droppedBombLoc = null;

        deployedBombLoc.getBlock().setType(Material.DAYLIGHT_DETECTOR);
        below.getBlock().setType(Material.AIR);

        gameManager.getExSkillManager().addCharge(player, 1);
        gameManager.onBombDeployed(bombTimer);

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.isOnline()) continue;
            Location targetLoc = target.getLocation();
            if (!deployedBombLoc.getWorld().equals(targetLoc.getWorld())) continue;
            double distance = targetLoc.distance(deployedBombLoc);
            float pitch = (float) Math.max(1.0, 2.0 - (distance / 30) * 1.0);
            float volume = (float) Math.max(0.5, 1.0 - (distance / 30) * 0.5);
            target.playSound(deployedBombLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, volume, pitch);
        }

        Bukkit.broadcast(Component.text("💣 炸弹已部署！40秒后引爆！", NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true));

        cancelPlant(player);

        startBombCountdown();
    }

    private void startBombCountdown() {
        if (bombTaskId != -1) {
            Bukkit.getScheduler().cancelTask(bombTaskId);
        }

        bombTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!bombDeployed) {
                Bukkit.getScheduler().cancelTask(bombTaskId);
                bombTaskId = -1;
                return;
            }

            if (bombTimer <= 0) {
                explodeBomb();
                return;
            }

            gameManager.updateBombTimerDisplay(bombTimer);

            if (bombTimer <= 10) {
                Bukkit.broadcast(Component.text("⏰ 炸弹将在 " + bombTimer + " 秒后引爆！", NamedTextColor.RED)
                        .decoration(TextDecoration.BOLD, true));
            }

            bombTimer--;
        }, 0L, 20L);
    }

    private void explodeBomb() {
        if (bombExploded) return;
        bombExploded = true;
        bombDeployed = false;

        if (bombTaskId != -1) {
            Bukkit.getScheduler().cancelTask(bombTaskId);
            bombTaskId = -1;
        }

        Location explosionLoc = deployedBombLoc;

        if (explosionLoc != null) {
            explosionLoc.getWorld().spawnParticle(Particle.EXPLOSION, explosionLoc, 10, 1, 1, 1, 0.5);
            explosionLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, explosionLoc, 5, 0.5, 0.5, 0.5, 0);
            explosionLoc.getWorld().spawnParticle(Particle.SMOKE, explosionLoc, 20, 1, 1, 1, 0.1);
            explosionLoc.getWorld().playSound(explosionLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);

            for (Player player : explosionLoc.getWorld().getPlayers()) {
                if (player.getLocation().distance(explosionLoc) <= 12) {
                    player.damage(9999);
                }
            }

            explosionLoc.getBlock().setType(Material.AIR);

            World world = explosionLoc.getWorld();
            for (int x = -60; x <= 60; x++) {
                for (int y = -20; y <= 20; y++) {
                    for (int z = -60; z <= 60; z++) {
                        Location checkLoc = explosionLoc.clone().add(x, y, z);
                        if (checkLoc.getBlock().getType() == Material.DAYLIGHT_DETECTOR) {
                            checkLoc.getBlock().setType(Material.AIR);
                        }
                    }
                }
            }

            deployedBombLoc = null;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (gameManager.getState() == GameState.ROUND_END || gameManager.getState() == GameState.GAME_OVER) {
                return;
            }
            gameManager.endRoundByBomb(GameManager.TEAM_ATTACKER);
        }, 10L);
    }

    public void onDefuseStart(Player player) {
        if (!bombDeployed || deployedBombLoc == null) return;
        if (isDefusing) {
            player.sendMessage(Component.text("❌ 已有玩家正在拆除！", NamedTextColor.RED));
            return;
        }

        if (player.getLocation().distance(deployedBombLoc) > 3) {
            player.sendMessage(Component.text("❌ 距离炸弹太远！", NamedTextColor.RED));
            return;
        }

        boolean hasShears = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.SHEARS) {
                hasShears = true;
                break;
            }
        }

        if (!hasShears) {
            player.sendMessage(Component.text("❌ 需要拆弹器！", NamedTextColor.RED));
            return;
        }

        isDefusing = true;
        defusingPlayer = player.getUniqueId();

        int savedProgress = defuseProgressMap.getOrDefault(player.getUniqueId(), 0);
        defuseProgress = savedProgress;
        defuseStage = savedProgress >= 3 ? 2 : 1;

        player.sendMessage(Component.text("🔧 开始拆除炸弹...", NamedTextColor.YELLOW));

        broadcastDefuseAlert(player);

        defuseTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!isDefusing || defusingPlayer == null) {
                Bukkit.getScheduler().cancelTask(defuseTaskId);
                defuseTaskId = -1;
                return;
            }

            Player defuser = Bukkit.getPlayer(defusingPlayer);
            if (defuser == null || !defuser.isOnline() || defuser.getLocation().distance(deployedBombLoc) > 3) {
                defuseProgressMap.put(defusingPlayer, defuseProgress);
                isDefusing = false;
                defusingPlayer = null;
                Bukkit.getScheduler().cancelTask(defuseTaskId);
                defuseTaskId = -1;
                if (defuser != null && defuser.isOnline()) {
                    defuser.sendMessage(Component.text("⚠ 拆除中断！进度已保存。", NamedTextColor.YELLOW));
                }
                return;
            }

            defuseProgress++;

            int totalProgress = 6;
            int progressPercent = (defuseProgress * 100) / totalProgress;
            defuser.sendActionBar(Component.text("拆除进度: " + progressPercent + "%", NamedTextColor.GREEN));

            if (defuseProgress == 3) {
                defuseStage = 2;
                defuser.sendMessage(Component.text("✅ 第一阶段完成！", NamedTextColor.GREEN));
            }

            if (defuseProgress >= 6) {
                completeDefuse();
            }
        }, 0L, 20L);
    }

    private void broadcastDefuseAlert(Player defuser) {
        Location defuseLoc = defuser.getLocation();

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.isOnline()) continue;

            Location targetLoc = target.getLocation();
            if (!defuseLoc.getWorld().equals(targetLoc.getWorld())) continue;

            double distance = targetLoc.distance(defuseLoc);
            if (distance > 14) continue;

            float volume = (float) Math.max(0.15, 1.0 - (distance / 14) * 0.85);
            float pitch = (float) Math.max(0.8, 1.6 - (distance / 14) * 0.8);

            target.playSound(defuseLoc, Sound.BLOCK_BEACON_DEACTIVATE, volume, pitch);
            target.playSound(defuseLoc, Sound.BLOCK_NOTE_BLOCK_BASS, volume * 0.5f, pitch * 1.2f);
            target.playSound(defuseLoc, Sound.BLOCK_NOTE_BLOCK_HAT, volume * 0.3f, pitch * 1.5f);

            if (distance < 6) {
                target.playSound(defuseLoc, Sound.BLOCK_NOTE_BLOCK_PLING, volume * 0.6f, pitch + 0.5f);
            }
        }

        defuser.playSound(defuseLoc, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.6f);
        defuser.playSound(defuseLoc, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 1.8f);
    }

    private void completeDefuse() {
        if (defuseTaskId != -1) {
            Bukkit.getScheduler().cancelTask(defuseTaskId);
            defuseTaskId = -1;
        }

        isDefusing = false;
        Player defuser = Bukkit.getPlayer(defusingPlayer);
        defusingPlayer = null;
        defuseStage = 0;
        defuseProgress = 0;
        defuseProgressMap.clear();

        if (deployedBombLoc != null) {
            deployedBombLoc.getBlock().setType(Material.AIR);
            deployedBombLoc.getWorld().playSound(deployedBombLoc, Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 1.0f);
        }

        bombDeployed = false;
        bombExploded = false;
        deployedBombLoc = null;

        if (defuser != null) {
            gameManager.getExSkillManager().addCharge(defuser, 1);
        }

        Bukkit.broadcast(Component.text("✅ 炸弹被拆除！守方获胜！", NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true));

        gameManager.endRoundByBomb(GameManager.TEAM_DEFENDER);
    }

    public void cleanup() {
        if (compassTaskId != -1) {
            Bukkit.getScheduler().cancelTask(compassTaskId);
            compassTaskId = -1;
        }
        if (bombTaskId != -1) {
            Bukkit.getScheduler().cancelTask(bombTaskId);
            bombTaskId = -1;
        }
        if (defuseTaskId != -1) {
            Bukkit.getScheduler().cancelTask(defuseTaskId);
            defuseTaskId = -1;
        }

        for (UUID uuid : plantTaskId.keySet()) {
            Integer taskId = plantTaskId.get(uuid);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
        }
        plantTaskId.clear();
        isPlanting.clear();
        plantProgress.clear();

        clearDeployedBombBlock();

        bombHolder = null;
        droppedBombLoc = null;
        deployedBombLoc = null;
        bombDeployed = false;
        bombExploded = false;
        defuseStage = 0;
        defuseProgress = 0;
        isDefusing = false;
        defusingPlayer = null;
        bombTimer = 40;
        defuseProgressMap.clear();
    }

    public void clearDeployedBombBlock() {
        if (deployedBombLoc != null && deployedBombLoc.getBlock().getType() == Material.DAYLIGHT_DETECTOR) {
            deployedBombLoc.getBlock().setType(Material.AIR);
        }
        deployedBombLoc = null;
    }

    public void onPlayerDeath(Player player) {
        if (isPlanting.getOrDefault(player.getUniqueId(), false)) {
            cancelPlant(player);
        }

        if (bombHolder != null && bombHolder.equals(player.getUniqueId())) {
            ItemStack slot4 = player.getInventory().getItem(3);
            if (slot4 != null && slot4.getType() == Material.DAYLIGHT_DETECTOR &&
                    slot4.hasItemMeta() && slot4.getItemMeta().hasDisplayName() &&
                    slot4.getItemMeta().displayName().toString().contains("炸弹部署器")) {

                ItemStack bomb = new ItemStack(Material.QUARTZ);
                ItemMeta meta = bomb.getItemMeta();
                meta.displayName(Component.text("炸药", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
                meta.lore(List.of(
                        Component.text("前往A/B/C包点部署", NamedTextColor.GRAY),
                        Component.text("在石英块上右键", NamedTextColor.YELLOW),
                        Component.text("然后蹲下4秒部署", NamedTextColor.GOLD),
                        Component.text("拆除需要7秒，分2阶段", NamedTextColor.LIGHT_PURPLE),
                        Component.text("可丢弃，不可被移动", NamedTextColor.DARK_GRAY)
                ));
                meta.setUnbreakable(true);
                bomb.setItemMeta(meta);

                player.getInventory().setItem(3, bomb);
            }
        }
    }

    public boolean isRestrictedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;

        String name = item.getItemMeta().displayName().toString();
        return name.contains("炸药") || name.contains("拆弹器") ||
                name.contains("炸弹部署器") || name.contains("炸药追踪器");
    }

    public boolean isPlanting(Player player) {
        return isPlanting.getOrDefault(player.getUniqueId(), false);
    }

    public int getPlantProgress(Player player) {
        return plantProgress.getOrDefault(player.getUniqueId(), 0);
    }
}