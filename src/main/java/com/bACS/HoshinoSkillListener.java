package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class HoshinoSkillListener implements Listener {
    private final BACS plugin;
    private final GameManager gameManager;
    private final ShieldManager shieldManager;

    private final Map<UUID, Boolean> hasMarker = new HashMap<>();
    private final Map<UUID, Boolean> markerThrown = new HashMap<>();
    private final Map<UUID, Boolean> exActiveAttack = new HashMap<>();
    private final Map<UUID, Boolean> exActiveDefense = new HashMap<>();
    private final Map<UUID, Item> thrownMarker = new HashMap<>();
    private final Map<UUID, List<Player>> markedPlayers = new HashMap<>();

    private final Map<UUID, Long> lastSpacePress = new HashMap<>();
    private final Map<UUID, Integer> jumpPressCount = new HashMap<>();
    private final Map<UUID, Long> lastJumpTime = new HashMap<>();

    private final Set<UUID> blockingPlayers = new HashSet<>();

    // ★ 持盾速度 = 基础走路速度 × 0.75
    private static final float BLOCKING_WALK_SPEED = 0.2f * 0.75f;

    public HoshinoSkillListener(BACS plugin, GameManager gameManager, ShieldManager shieldManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.shieldManager = shieldManager;
        startBlockingCheck();
    }

    // ============================================================
    // 防御形态
    // ============================================================

    public void onRoundStartDefense(Player player) {
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("hoshino_defense")) return;

        shieldManager.resetRound(player);
        shieldManager.giveNormalShield(player);

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        boolean isMainHandEmpty = mainHand == null || mainHand.getType() == Material.AIR;
        if (isMainHandEmpty) {
            giveShieldItem(player);
        }

        player.sendMessage(Component.text("🛡 你获得了盾牌！耐久 " + ShieldManager.NORMAL_SHIELD_DURABILITY + "（需空手才能举盾）", NamedTextColor.AQUA));
    }

    private void giveShieldItem(Player player) {
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && offHand.getType() != Material.AIR && offHand.getType() != Material.SHIELD) {
            return;
        }

        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemMeta meta = shield.getItemMeta();
        boolean enhanced = shieldManager.isEnhanced(player);
        if (enhanced) {
            meta.displayName(Component.text("🛡 强化盾牌", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.text("耐久: " + shieldManager.getDurability(player) + "/" + shieldManager.getMaxDurability(player), NamedTextColor.GRAY),
                    Component.text("右键举盾 / 左键放盾", NamedTextColor.YELLOW),
                    Component.text("举盾时移速: 基础的75%", NamedTextColor.YELLOW),
                    Component.text("最终伤害减免: 75%", NamedTextColor.GREEN),
                    Component.text("回敬伤害: 20%", NamedTextColor.LIGHT_PURPLE),
                    Component.text("不可取下", NamedTextColor.DARK_GRAY)
            ));
        } else {
            meta.displayName(Component.text("🛡 盾牌", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.text("耐久: " + shieldManager.getDurability(player) + "/" + shieldManager.getMaxDurability(player), NamedTextColor.GRAY),
                    Component.text("右键举盾 / 左键放盾", NamedTextColor.YELLOW),
                    Component.text("举盾时移速: 基础的75%", NamedTextColor.YELLOW),
                    Component.text("最终伤害减免: 85%", NamedTextColor.GREEN),
                    Component.text("不可取下", NamedTextColor.DARK_GRAY)
            ));
        }
        meta.setUnbreakable(true);
        shield.setItemMeta(meta);
        player.getInventory().setItemInOffHand(shield);
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("hoshino_defense")) return;

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && offHand.getType() == Material.SHIELD) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ 盾牌不可取下！", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null) return;
        if (gameManager.getState() != GameState.ROUND_ACTIVE) return;
        if (!gameManager.isPlayerAlive(player)) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        if (kit.getId().equals("hoshino_defense")) {
            boolean hasShieldInHand = item.getType() == Material.SHIELD ||
                    player.getInventory().getItemInOffHand().getType() == Material.SHIELD;

            if (!hasShieldInHand) return;

            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);

                ItemStack mainHand = player.getInventory().getItemInMainHand();
                if (mainHand != null && mainHand.getType() != Material.AIR) {
                    player.sendMessage(Component.text("❌ 需要空手才能举盾！", NamedTextColor.RED));
                    return;
                }

                if (!shieldManager.hasShield(player)) {
                    player.sendMessage(Component.text("❌ 盾牌已损坏！", NamedTextColor.RED));
                    return;
                }
                shieldManager.setBlocking(player, true);
                blockingPlayers.add(player.getUniqueId());
                updateBlockingSpeed(player, true);
                return;
            }

            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
                shieldManager.setBlocking(player, false);
                blockingPlayers.remove(player.getUniqueId());
                updateBlockingSpeed(player, false);
            }
        }

        if (kit.getId().equals("hoshino_attack")) {
            if (item.getType() == Material.IRON_INGOT &&
                    item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().displayName().toString().contains("标记器")) {
                if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    throwMarker(player);
                }
            }
        }
    }

    private void updateBlockingSpeed(Player player, boolean blocking) {
        if (blocking) {
            player.setWalkSpeed(BLOCKING_WALK_SPEED);
        } else {
            player.setWalkSpeed(0.2f);
        }
    }

    public void startBlockingCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    HeroKit kit = gameManager.getPlayerHero(player);
                    if (kit == null || !kit.getId().equals("hoshino_defense")) {
                        if (blockingPlayers.contains(player.getUniqueId())) {
                            shieldManager.setBlocking(player, false);
                            blockingPlayers.remove(player.getUniqueId());
                            player.setWalkSpeed(0.2f);
                        }
                        continue;
                    }

                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    boolean isMainHandEmpty = mainHand == null || mainHand.getType() == Material.AIR;

                    if (!isMainHandEmpty) {
                        ItemStack offHand = player.getInventory().getItemInOffHand();
                        if (offHand != null && offHand.getType() == Material.SHIELD) {
                            player.getInventory().setItemInOffHand(null);
                        }
                        if (shieldManager.isBlocking(player)) {
                            shieldManager.setBlocking(player, false);
                            blockingPlayers.remove(player.getUniqueId());
                            player.setWalkSpeed(0.2f);
                        }
                        continue;
                    }

                    ItemStack offHand = player.getInventory().getItemInOffHand();
                    boolean hasShield = offHand != null && offHand.getType() == Material.SHIELD;
                    if (!hasShield) {
                        giveShieldItem(player);
                        continue;
                    }

                    if (!shieldManager.hasShield(player)) {
                        if (shieldManager.isBlocking(player)) {
                            shieldManager.setBlocking(player, false);
                            blockingPlayers.remove(player.getUniqueId());
                            player.setWalkSpeed(0.2f);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void activateDefenseEX(Player player) {
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("hoshino_defense")) return;

        int charge = gameManager.getExSkillManager().getCharge(player);
        if (charge < 7) {
            player.sendMessage(Component.text("❌ EX充能不足！需要7点", NamedTextColor.RED));
            return;
        }

        int currentDurability = shieldManager.getDurability(player);
        if (!shieldManager.hasShield(player) || currentDurability < 50) {
            player.sendMessage(Component.text("❌ 盾牌耐久不足50或已损坏！", NamedTextColor.RED));
            return;
        }

        gameManager.getExSkillManager().clearCharge(player);
        exActiveDefense.put(player.getUniqueId(), true);

        shieldManager.giveEnhancedShield(player);
        giveShieldItem(player);

        player.sendMessage(Component.text("🛡 强化盾牌已装备！", NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("hoshino_defense")) return;

        if (event.getFrom().getY() < event.getTo().getY() &&
                !player.isOnGround() &&
                event.getFrom().getY() - event.getFrom().getBlockY() < 0.1) {

            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();

            Long lastJump = lastJumpTime.get(uuid);
            if (lastJump != null && now - lastJump < 400) {
                int count = jumpPressCount.getOrDefault(uuid, 1) + 1;
                jumpPressCount.put(uuid, count);

                if (count >= 2) {
                    jumpPressCount.put(uuid, 0);
                    tryDash(player);
                }
            } else {
                jumpPressCount.put(uuid, 1);
            }

            lastJumpTime.put(uuid, now);
        }
    }

    public void tryDash(Player player) {
        UUID uuid = player.getUniqueId();
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("hoshino_defense")) return;
        if (!shieldManager.isEnhanced(player)) return;
        if (!shieldManager.hasShield(player)) {
            player.sendMessage(Component.text("❌ 盾牌已损坏，无法跃进！", NamedTextColor.RED));
            return;
        }

        if (!shieldManager.canDash(player)) {
            long remaining = shieldManager.getDashCooldownRemaining(player) / 1000 + 1;
            player.sendMessage(Component.text("❌ 跃进冷却中: " + remaining + "s", NamedTextColor.RED));
            return;
        }

        performDash(player);
        shieldManager.setDashCooldown(player, 8000);
    }

    private void performDash(Player player) {
        Vector direction = player.getLocation().getDirection().clone().setY(0.4).normalize();
        player.setVelocity(direction.multiply(1.5));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnGround()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 3, true, false));
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        player.sendMessage(Component.text("⚡ 跃进！", NamedTextColor.AQUA));
    }

    public void onPlayerDeath(Player player) {
        UUID uuid = player.getUniqueId();
        exActiveDefense.put(uuid, false);
        exActiveAttack.put(uuid, false);
        shieldManager.setBlocking(player, false);
        blockingPlayers.remove(uuid);
        updateBlockingSpeed(player, false);
    }

    // ============================================================
    // 攻击形态
    // ============================================================

    public void onRoundStartAttack(Player player) {
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("hoshino_attack")) return;

        hasMarker.put(player.getUniqueId(), false);
        markerThrown.put(player.getUniqueId(), false);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false));
    }

    public void activateAttackEX(Player player) {
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("hoshino_attack")) return;

        int charge = gameManager.getExSkillManager().getCharge(player);
        if (charge < 8) {
            player.sendMessage(Component.text("❌ EX充能不足！需要8点", NamedTextColor.RED));
            return;
        }

        gameManager.getExSkillManager().clearCharge(player);
        exActiveAttack.put(player.getUniqueId(), true);
        hasMarker.put(player.getUniqueId(), true);

        ItemStack marker = new ItemStack(Material.IRON_INGOT);
        ItemMeta meta = marker.getItemMeta();
        meta.displayName(Component.text("🎯 标记器", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true));
        meta.lore(List.of(
                Component.text("右键投掷", NamedTextColor.GRAY),
                Component.text("标记区域内敌人", NamedTextColor.YELLOW),
                Component.text("3秒后引爆", NamedTextColor.RED)
        ));
        marker.setItemMeta(meta);

        player.getInventory().setItem(2, marker);

        player.sendMessage(Component.text("🎯 标记器已装备！右键投掷！", NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true));
    }

    private void throwMarker(Player player) {
        UUID uuid = player.getUniqueId();
        if (!hasMarker.getOrDefault(uuid, false)) return;

        ItemStack marker = player.getInventory().getItem(2);
        if (marker == null || marker.getType() != Material.IRON_INGOT) return;

        player.getInventory().setItem(2, null);
        hasMarker.put(uuid, false);
        markerThrown.put(uuid, true);

        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().clone();

        Item item = player.getWorld().dropItem(eyeLoc, new ItemStack(Material.IRON_INGOT));
        Vector velocity = direction.multiply(1.8).add(new Vector(0, 0.25, 0));
        item.setVelocity(velocity);
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setGlowing(true);
        item.setInvulnerable(true);
        item.setCustomName("标记器");
        item.setCustomNameVisible(true);

        player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0f, 1.0f);

        thrownMarker.put(uuid, item);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                if (item.isDead() || !item.isValid() || ticks > 200) {
                    cancel();
                    return;
                }
                if (item.isOnGround() || item.getVelocity().length() < 0.1) {
                    onMarkerLanded(player, item.getLocation());
                    item.remove();
                    thrownMarker.remove(uuid);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void onMarkerLanded(Player owner, Location loc) {
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distance(loc) <= 20) {
                p.sendMessage(Component.text("⚠ 标记器已落地！3秒后爆炸！", NamedTextColor.RED)
                        .decoration(TextDecoration.BOLD, true));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (owner.isOnline()) {
                    explodeMarker(owner, loc);
                }
            }
        }.runTaskLater(plugin, 60L);
    }

    private void explodeMarker(Player owner, Location center) {
        final int[] explosionCount = {3};
        final boolean[] killed = {false};

        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= explosionCount[0]) {
                    cancel();
                    return;
                }
                count++;

                center.getWorld().spawnParticle(Particle.EXPLOSION, center, 5, 1, 1, 1, 0.5);
                center.getWorld().spawnParticle(Particle.SMOKE, center, 30, 3, 3, 3, 0.1);
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);

                for (Player target : center.getWorld().getPlayers()) {
                    if (target.equals(owner)) continue;
                    if (!gameManager.isPlayerAlive(target)) continue;

                    String ownerTeam = gameManager.getPlayerTeam(owner);
                    String targetTeam = gameManager.getPlayerTeam(target);
                    if (ownerTeam != null && targetTeam != null && ownerTeam.equals(targetTeam)) continue;

                    if (target.getLocation().distance(center) <= 15) {
                        target.damage(70, owner);
                        target.sendMessage(Component.text("💥 标记器爆炸！", NamedTextColor.RED));

                        if (!gameManager.isPlayerAlive(target)) {
                            killed[0] = true;
                        }
                    }
                }

                if (killed[0] && explosionCount[0] == 3) {
                    explosionCount[0] = 4;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ============================================================
    // 伤害处理
    // ============================================================

    public double handleShieldDamage(Player player, double damage, Player attacker) {
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("hoshino_defense")) return damage;

        if (!shieldManager.isBlocking(player)) return damage;
        if (!shieldManager.hasShield(player)) return damage;

        boolean enhanced = shieldManager.isEnhanced(player);
        double reduction = enhanced ? WeaponConfig.HOSHINO_ENHANCED_SHIELD_DAMAGE_REDUCTION
                : WeaponConfig.HOSHINO_SHIELD_DAMAGE_REDUCTION;
        double absorbMultiplier = enhanced ? WeaponConfig.HOSHINO_ENHANCED_SHIELD_ABSORB_MULTIPLIER
                : WeaponConfig.HOSHINO_SHIELD_ABSORB_MULTIPLIER;

        double finalDamage = damage * (1 - reduction);
        double absorbed = damage * reduction;
        int durabilityCost = (int) Math.ceil(absorbed * absorbMultiplier);

        shieldManager.consumeDurability(player, durabilityCost);
        updateShieldDisplay(player);

        if (enhanced && attacker != null && attacker.isOnline()) {
            double reflectDamage = finalDamage * 0.20;
            attacker.damage(reflectDamage, player);
        }

        return finalDamage;
    }

    private void updateShieldDisplay(Player player) {
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && offHand.getType() == Material.SHIELD) {
            giveShieldItem(player);
        }
    }

    public void onKill(Player killer) {
        HeroKit kit = gameManager.getPlayerHero(killer);
        if (kit == null || !kit.getId().equals("hoshino_defense")) return;
        shieldManager.onKill(killer);
        updateShieldDisplay(killer);
    }

    // ============================================================
    // 事件处理
    // ============================================================

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null) return;

        if (kit.getId().equals("hoshino_attack")) {
            double damage = event.getDamage();
            event.setDamage(damage * 1.10);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        HeroKit kit = gameManager.getPlayerHero(attacker);
        if (kit == null) return;

        if (kit.getId().equals("hoshino_attack")) {
            double damage = event.getDamage();
            event.setDamage(damage * 1.10);
        }
    }

    // ============================================================
    // 重置
    // ============================================================

    public void resetAll() {
        hasMarker.clear();
        markerThrown.clear();
        exActiveAttack.clear();
        exActiveDefense.clear();
        thrownMarker.clear();
        markedPlayers.clear();
        lastSpacePress.clear();
        jumpPressCount.clear();
        lastJumpTime.clear();
        blockingPlayers.clear();
    }

    public void resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        hasMarker.remove(uuid);
        markerThrown.remove(uuid);
        exActiveAttack.remove(uuid);
        exActiveDefense.remove(uuid);
        thrownMarker.remove(uuid);
        markedPlayers.remove(uuid);
        lastSpacePress.remove(uuid);
        jumpPressCount.remove(uuid);
        lastJumpTime.remove(uuid);
        blockingPlayers.remove(uuid);
    }
}