package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SerinaSkillListener implements Listener {
    private final BACS plugin;
    private final GameManager gameManager;

    // 被动：队伍治疗效果增加10%
    private final Map<UUID, Boolean> hasSerinaInTeam = new HashMap<>();

    // 主动技能：治疗之杖
    private final Map<UUID, Boolean> isHealingActive = new HashMap<>();
    private final Map<UUID, Long> healCooldown = new HashMap<>();
    private final Map<UUID, Integer> healTaskId = new HashMap<>();
    private static final long HEAL_COOLDOWN = 8000; // 8秒冷却
    private static final int HEAL_AMOUNT = 20;
    private static final int HEAL_DURATION = 100; // 5秒生命恢复3

    // EX技能：复活
    private final Map<UUID, Boolean> exPrepared = new HashMap<>();
    private final Map<UUID, Long> exCooldown = new HashMap<>();
    private static final long EX_COOLDOWN = 30000; // 30秒冷却
    private static final int EX_REVIVE_RADIUS = 8;
    private static final int EX_PICKUP_RADIUS = 4;

    // 复活状态
    private final Map<UUID, Boolean> isReviving = new HashMap<>();
    private final Map<UUID, Integer> reviveTaskId = new HashMap<>();

    public SerinaSkillListener(BACS plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    // ============================================================
    // 被动技能：队伍治疗效果增加10%
    // ============================================================
    public void onTeamUpdate(Player player) {
        String team = gameManager.getPlayerTeam(player);
        if (team == null) return;

        // 检查队伍中是否有芹奈
        boolean hasSerina = false;
        for (Player p : Bukkit.getOnlinePlayers()) {
            HeroKit kit = gameManager.getPlayerHero(p);
            if (kit != null && kit.getId().equals("serina")) {
                String pTeam = gameManager.getPlayerTeam(p);
                if (pTeam != null && pTeam.equals(team)) {
                    hasSerina = true;
                    break;
                }
            }
        }

        // 更新所有队员的状态
        for (Player p : Bukkit.getOnlinePlayers()) {
            String pTeam = gameManager.getPlayerTeam(p);
            if (pTeam != null && pTeam.equals(team)) {
                hasSerinaInTeam.put(p.getUniqueId(), hasSerina);
            }
        }
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getRegainReason() != EntityRegainHealthEvent.RegainReason.CUSTOM &&
                event.getRegainReason() != EntityRegainHealthEvent.RegainReason.SATIATED &&
                event.getRegainReason() != EntityRegainHealthEvent.RegainReason.REGEN) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (!hasSerinaInTeam.getOrDefault(uuid, false)) return;

        double amount = event.getAmount();
        // 增加10%，但不超过10点
        double bonus = Math.min(amount * 0.1, 10);
        event.setAmount(amount + bonus);
    }

    // ============================================================
    // 主动技能：蹲下丢弃近战武器触发治疗
    // ============================================================
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("serina")) return;

        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() != kit.getMeleeWeaponMaterial()) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().displayName().toString().contains(kit.getMeleeWeaponName())) return;

        // 必须蹲下
        if (!player.isSneaking()) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ 需要蹲下丢弃才能触发治疗！", NamedTextColor.RED));
            return;
        }

        event.setCancelled(true);

        UUID uuid = player.getUniqueId();

        // 检查冷却
        long cooldownEnd = healCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() < cooldownEnd) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
            player.sendMessage(Component.text("❌ 冷却中: " + remaining + "秒", NamedTextColor.RED));
            return;
        }

        // 检查是否已在治疗中
        if (isHealingActive.getOrDefault(uuid, false)) {
            player.sendMessage(Component.text("❌ 已在治疗中！", NamedTextColor.RED));
            return;
        }

        activateHeal(player, kit);
    }

    private void activateHeal(Player player, HeroKit kit) {
        UUID uuid = player.getUniqueId();

        // 给近战武器附魔效果
        ItemStack weapon = player.getInventory().getItem(2);
        if (weapon != null && weapon.getType() == kit.getMeleeWeaponMaterial()) {
            ItemMeta meta = weapon.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("✨ 治疗之杖", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
                meta.lore(List.of(
                        Component.text("攻击队友治疗20生命", NamedTextColor.GRAY),
                        Component.text("并给予5秒生命恢复3", NamedTextColor.GRAY)
                ));
                weapon.setItemMeta(meta);
            }
        }

        isHealingActive.put(uuid, true);
        healCooldown.put(uuid, System.currentTimeMillis() + HEAL_COOLDOWN);

        player.sendMessage(Component.text("✨ 治疗之杖已激活！攻击队友进行治疗", NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true));

        // 5秒后自动取消
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                deactivateHeal(player, kit);
            }
        }.runTaskLater(plugin, 100L).getTaskId();

        healTaskId.put(uuid, taskId);
    }

    private void deactivateHeal(Player player, HeroKit kit) {
        UUID uuid = player.getUniqueId();
        isHealingActive.put(uuid, false);

        // 恢复近战武器
        ItemStack weapon = player.getInventory().getItem(2);
        if (weapon != null && weapon.getType() == kit.getMeleeWeaponMaterial()) {
            ItemMeta meta = weapon.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(kit.getMeleeWeaponName(), NamedTextColor.GOLD));
                meta.lore(List.of(
                        Component.text("伤害: " + kit.getMeleeDamage(), NamedTextColor.RED),
                        Component.text("背刺: " + kit.getMeleeBackstabDamage(), NamedTextColor.DARK_RED),
                        Component.text("蹲下丢弃触发治疗", NamedTextColor.GRAY),
                        Component.text("无法取下", NamedTextColor.DARK_GRAY)
                ));
                weapon.setItemMeta(meta);
            }
        }

        player.sendMessage(Component.text("⏰ 治疗之杖已失效", NamedTextColor.YELLOW));
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        HeroKit kit = gameManager.getPlayerHero(attacker);
        if (kit == null || !kit.getId().equals("serina")) return;

        // 检查是否在治疗状态
        UUID uuid = attacker.getUniqueId();
        if (!isHealingActive.getOrDefault(uuid, false)) return;

        // 检查是否使用近战武器
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType() != kit.getMeleeWeaponMaterial()) return;
        if (!weapon.hasItemMeta() || !weapon.getItemMeta().hasDisplayName()) return;
        if (!weapon.getItemMeta().displayName().toString().contains("治疗之杖")) return;

        // 检查是否是队友
        String attackerTeam = gameManager.getPlayerTeam(attacker);
        String targetTeam = gameManager.getPlayerTeam(target);
        if (attackerTeam == null || targetTeam == null || !attackerTeam.equals(targetTeam)) return;

        // 取消伤害
        event.setCancelled(true);

        // 治疗队友
        double maxHealth = target.getMaxHealth();
        double currentHealth = target.getHealth();
        double newHealth = Math.min(maxHealth, currentHealth + HEAL_AMOUNT);
        target.setHealth(newHealth);

        // 生命恢复3，持续5秒
        target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, HEAL_DURATION, 2, true, false));

        // 特效
        target.getWorld().spawnParticle(org.bukkit.Particle.HEART, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        target.getWorld().spawnParticle(org.bukkit.Particle.GLOW, target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.01);
        target.playSound(target.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);

        target.sendMessage(Component.text("💚 被芹奈治疗了 " + HEAL_AMOUNT + " 生命！", NamedTextColor.GREEN));
        attacker.sendMessage(Component.text("💚 治疗了 " + target.getName() + "！", NamedTextColor.GREEN));

        // 攻击者获得短暂的发光效果
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, true, false));
    }

    // ============================================================
    // EX技能：复活
    // ============================================================
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("serina")) return;
        if (gameManager.getState() != GameState.ROUND_ACTIVE) return;
        if (!gameManager.isPlayerAlive(player)) return;

        ItemStack item = event.getItem();
        if (item == null) return;
        if (item.getType() != kit.getWeaponMaterial()) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().displayName().toString().contains(kit.getWeaponName())) return;

        UUID uuid = player.getUniqueId();

        // 蹲下右键2次触发EX（两次点击在短时间内）
        if (player.isSneaking() && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);

            // 检查EX准备状态
            if (exPrepared.getOrDefault(uuid, false)) {
                // 第二次点击，触发EX
                exPrepared.put(uuid, false);
                triggerEX(player);
            } else {
                // 第一次点击，进入准备状态
                if (gameManager.getExSkillManager().getCharge(player) < kit.getExSkillMaxCharge()) {
                    player.sendMessage(Component.text("❌ EX充能不足！需要 " + kit.getExSkillMaxCharge() + " 点", NamedTextColor.RED));
                    return;
                }

                // 检查冷却
                long cooldownEnd = exCooldown.getOrDefault(uuid, 0L);
                if (System.currentTimeMillis() < cooldownEnd) {
                    long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
                    player.sendMessage(Component.text("❌ EX冷却中: " + remaining + "秒", NamedTextColor.RED));
                    return;
                }

                exPrepared.put(uuid, true);
                player.sendMessage(Component.text("⚡ 再次蹲下右键触发EX技能！", NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));

                // 5秒后取消准备
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (exPrepared.getOrDefault(uuid, false)) {
                            exPrepared.put(uuid, false);
                            player.sendMessage(Component.text("⏰ EX准备超时", NamedTextColor.YELLOW));
                        }
                    }
                }.runTaskLater(plugin, 100L);
            }
        }
    }

    private void triggerEX(Player player) {
        UUID uuid = player.getUniqueId();
        HeroKit kit = gameManager.getPlayerHero(player);

        // 消耗充能
        gameManager.getExSkillManager().clearCharge(player);

        // 查找最近的死亡队友
        Player reviveTarget = findNearestDeadTeammate(player);

        if (reviveTarget == null) {
            player.sendMessage(Component.text("❌ 半径" + EX_REVIVE_RADIUS + "格内没有可复活的队友！", NamedTextColor.RED));
            // 返还一半充能
            gameManager.getExSkillManager().addCharge(player, kit.getExSkillMaxCharge() / 2);
            return;
        }

        // 开始复活
        startRevive(player, reviveTarget);

        // 设置冷却
        exCooldown.put(uuid, System.currentTimeMillis() + EX_COOLDOWN);
    }

    private Player findNearestDeadTeammate(Player player) {
        String team = gameManager.getPlayerTeam(player);
        if (team == null) return null;

        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        // 获取所有死亡的队友（包括不在线但记录中的）
        for (UUID uuid : gameManager.getPlayerTeams().keySet()) {
            Player target = Bukkit.getPlayer(uuid);
            if (target == null) continue;

            String targetTeam = gameManager.getPlayerTeam(target);
            if (targetTeam == null || !targetTeam.equals(team)) continue;

            // 检查是否死亡（不在存活列表中或存活为false）
            if (gameManager.isPlayerAlive(target)) continue;

            // 检查是否在半径内
            double distance = player.getLocation().distance(target.getLocation());
            if (distance <= EX_REVIVE_RADIUS && distance < nearestDistance) {
                nearestDistance = distance;
                nearest = target;
            }
        }

        return nearest;
    }

    private void startRevive(Player reviver, Player target) {
        UUID targetUuid = target.getUniqueId();

        // 设置复活状态
        isReviving.put(targetUuid, true);

        // 复活目标
        target.spigot().respawn();
        target.setGameMode(org.bukkit.GameMode.SURVIVAL);

        // 恢复全部生命
        target.setHealth(target.getMaxHealth());

        // 清除护甲，回复池归零
        target.getInventory().setHelmet(null);
        target.getInventory().setChestplate(null);
        target.getInventory().setLeggings(null);
        target.getInventory().setBoots(null);
        gameManager.setPlayerCurrentArmor(target, 0);
        gameManager.getShopManager().setRegenPool(target, 0);

        // 添加负面效果：2秒失明、无法移动
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, true, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 10, true, false));

        // 复活期间受到的伤害增加50%
        // 通过设置一个标记，在DamageListener中处理

        // 设置玩家为存活
        gameManager.getPlayerAlive().put(targetUuid, true);
        gameManager.getSpectators().remove(targetUuid);

        // 自动拾取附近的主武器
        pickupNearbyWeapon(target);

        // 传送复活者到目标位置
        target.teleport(reviver.getLocation());

        // 特效
        target.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        target.getWorld().spawnParticle(org.bukkit.Particle.HEART, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        target.getWorld().playSound(target.getLocation(), org.bukkit.Sound.ITEM_TOTEM_USE, 2.0f, 1.0f);

        Bukkit.broadcast(Component.text("💫 " + target.getName() + " 被芹奈复活了！", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));

        // 2秒后移除负面效果
        new BukkitRunnable() {
            @Override
            public void run() {
                if (target.isOnline()) {
                    target.removePotionEffect(PotionEffectType.BLINDNESS);
                    target.removePotionEffect(PotionEffectType.SLOWNESS);
                    isReviving.put(targetUuid, false);
                }
            }
        }.runTaskLater(plugin, 40L);
    }

    private void pickupNearbyWeapon(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();

        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (!(entity instanceof org.bukkit.entity.Item item)) continue;

            Location itemLoc = item.getLocation();
            if (itemLoc.distance(loc) <= EX_PICKUP_RADIUS) {
                ItemStack itemStack = item.getItemStack();
                // 检查是否是主武器
                for (HeroKit kit : GameManager.HERO_KITS) {
                    if (itemStack.getType() == kit.getWeaponMaterial() &&
                            itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() &&
                            itemStack.getItemMeta().displayName().toString().contains(kit.getWeaponName())) {
                        // 拾取到背包
                        if (player.getInventory().firstEmpty() != -1) {
                            player.getInventory().addItem(itemStack);
                            item.remove();
                            // 设置弹药
                            gameManager.getAmmoManager().resetRound(player.getUniqueId(), kit.getMagazineSize());
                            for (int i = 0; i < kit.getMaxMagazines(); i++) {
                                gameManager.getAmmoManager().addReserveMagazines(player.getUniqueId(), 1);
                            }
                            player.sendMessage(Component.text("🔫 自动拾取了主武器！", NamedTextColor.GREEN));
                        }
                        break;
                    }
                }
            }
        }
    }

    public boolean isReviving(Player player) {
        return isReviving.getOrDefault(player.getUniqueId(), false);
    }

    public void onPlayerDeath(Player player) {
        UUID uuid = player.getUniqueId();
        isReviving.remove(uuid);
        if (healTaskId.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(healTaskId.get(uuid));
            healTaskId.remove(uuid);
        }
        isHealingActive.put(uuid, false);
        exPrepared.put(uuid, false);
    }

    public void onRoundStart() {
        // 清空状态
        for (Player player : Bukkit.getOnlinePlayers()) {
            isHealingActive.put(player.getUniqueId(), false);
            exPrepared.put(player.getUniqueId(), false);
            isReviving.put(player.getUniqueId(), false);
        }
    }

    public void resetAll() {
        hasSerinaInTeam.clear();
        isHealingActive.clear();
        healCooldown.clear();
        healTaskId.clear();
        exPrepared.clear();
        exCooldown.clear();
        isReviving.clear();
        reviveTaskId.clear();
    }

    // 获取治疗状态供其他类使用
    public boolean isHealingActive(Player player) {
        return isHealingActive.getOrDefault(player.getUniqueId(), false);
    }
}