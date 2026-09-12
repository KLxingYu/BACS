package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class YuukaSkillListener implements Listener {
    private final BACS plugin;
    private final GameManager gameManager;

    // 被动护甲恢复
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private final Map<UUID, Long> passiveCooldownEnd = new HashMap<>();
    private final Map<UUID, Long> lastArmorRegenTime = new HashMap<>();
    private static final long NO_DAMAGE_TIME = 8000; // 8秒未受伤
    private static final long REGEN_INTERVAL = 3000; // 每3秒
    private static final long PASSIVE_COOLDOWN = 10000; // 10秒冷却
    private static final int REGEN_AMOUNT = 3; // 静止恢复3点
    private static final int REGEN_AMOUNT_SPRINT = 2; // 疾跑恢复2点

    // EX技能
    private final Map<UUID, Boolean> exActive = new HashMap<>();
    private final Map<UUID, Boolean> exPrepared = new HashMap<>();
    private final Map<UUID, Integer> sneakLeftClickCount = new HashMap<>();
    private final Map<UUID, Long> lastSneakLeftClickTime = new HashMap<>();

    public YuukaSkillListener(BACS plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;

        // 启动护甲恢复检测任务
        startArmorRegenTask();
    }

    /**
     * 护甲恢复检测任务
     */
    private void startArmorRegenTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    tickArmorRegen(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 每秒检测
    }

    /**
     * 护甲恢复逻辑
     */
    public void tickArmorRegen(Player player) {
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("yuuka")) return;
        if (gameManager.getState() != GameState.ROUND_ACTIVE) return;
        if (!gameManager.isPlayerAlive(player)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // 检查EX技能激活（200护甲期间不恢复）
        if (exActive.getOrDefault(uuid, false)) return;

        // 检查被动冷却
        long cooldownEnd = passiveCooldownEnd.getOrDefault(uuid, 0L);
        if (now < cooldownEnd) return;

        // 检查是否8秒未受伤
        long lastDamage = lastDamageTime.getOrDefault(uuid, 0L);
        if (now - lastDamage < NO_DAMAGE_TIME) return;

        // 检查护甲是否已满
        int currentArmor = gameManager.getPlayerCurrentArmor(player);
        int maxArmor = kit.getMaxArmor() + gameManager.getPlayerExtraMaxArmor(player);
        if (currentArmor >= maxArmor) return;

        // 检查恢复间隔
        long lastRegen = lastArmorRegenTime.getOrDefault(uuid, 0L);
        if (now - lastRegen < REGEN_INTERVAL) return;

        // 确定恢复量
        int regenAmount = player.isSprinting() ? REGEN_AMOUNT_SPRINT : REGEN_AMOUNT;

        // 恢复护甲
        int newArmor = Math.min(maxArmor, currentArmor + regenAmount);
        gameManager.setPlayerCurrentArmor(player, newArmor);
        lastArmorRegenTime.put(uuid, now);

        // 如果护甲满了，进入冷却
        if (newArmor >= maxArmor) {
            passiveCooldownEnd.put(uuid, now + PASSIVE_COOLDOWN);
        }
    }

    /**
     * 玩家受伤时调用
     */
    public void onPlayerDamaged(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        lastDamageTime.put(uuid, now);

        // 如果护甲恢复到一半以上，进入冷却
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit != null && kit.getId().equals("yuuka")) {
            passiveCooldownEnd.put(uuid, now + PASSIVE_COOLDOWN);
        }
    }

    /**
     * 闪避检测
     * 18%闪避率，AL1S和凯伊无法闪避但减伤10%
     */
    public boolean tryDodge(Player player, Player attacker, double damage) {
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("yuuka")) return false;

        HeroKit attackerKit = gameManager.getPlayerHero(attacker);

        // AL1S和凯伊的攻击无法闪避但减伤10%
        if (attackerKit != null &&
                (attackerKit.getId().equals("al1s") || attackerKit.getId().equals("kayi"))) {
            return false; // 不闪避，由DamageListener处理减伤
        }

        // 18%闪避率
        if (Math.random() < 0.18) {
            player.sendMessage(Component.text("✨ 闪避成功！", NamedTextColor.AQUA));
            return true;
        }

        return false;
    }

    /**
     * 护盾只吸收50%伤害
     */
    public double getArmorAbsorptionMultiplier(Player player) {
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit != null && kit.getId().equals("yuuka")) {
            return 0.5; // 只吸收50%
        }
        return 0.8; // 默认80%
    }

    /**
     * EX技能触发
     */
    public void triggerEX(Player player) {
        UUID uuid = player.getUniqueId();

        if (exActive.getOrDefault(uuid, false)) {
            // 取消EX技能
            exActive.put(uuid, false);
            gameManager.setPlayerCurrentArmor(player,
                    Math.min(gameManager.getPlayerCurrentArmor(player),
                            gameManager.getPlayerHero(player).getMaxArmor() + gameManager.getPlayerExtraMaxArmor(player)));
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.sendMessage(Component.text("💙 EX技能已取消", NamedTextColor.BLUE));
            return;
        }

        if (gameManager.getExSkillManager().getCharge(player) < 7) {
            player.sendMessage(Component.text("❌ EX技能充能不足！", NamedTextColor.RED));
            return;
        }

        // 消耗7点充能
        gameManager.getExSkillManager().clearCharge(player);

        // 激活EX
        exActive.put(uuid, true);
        gameManager.setPlayerCurrentArmor(player, 200);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 2, true, false));

        player.sendMessage(Component.text("💙 EX技能激活！获得200护甲！", NamedTextColor.BLUE)
                .decoration(TextDecoration.BOLD, true));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 1.0f);
    }

    /**
     * 检查EX技能状态
     */
    public boolean isExActive(Player player) {
        return exActive.getOrDefault(player.getUniqueId(), false);
    }

    /**
     * 蹲下左键检测（EX技能触发/取消）
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("yuuka")) return;
        if (gameManager.getState() != GameState.ROUND_ACTIVE) return;
        if (!gameManager.isPlayerAlive(player)) return;

        ItemStack item = event.getItem();
        if (item == null) return;
        if (item.getType() != kit.getWeaponMaterial()) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().displayName().toString().contains(kit.getWeaponName())) return;

        // 蹲下左键
        if (player.isSneaking() && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
            event.setCancelled(true);
            triggerEX(player);
        }
    }

    /**
     * 受伤事件处理
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("yuuka")) return;

        onPlayerDamaged(player);
    }

    /**
     * 被玩家攻击事件处理
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("yuuka")) return;

        HeroKit attackerKit = gameManager.getPlayerHero(attacker);

        // AL1S和凯伊的攻击：无法闪避但减伤10%
        if (attackerKit != null &&
                (attackerKit.getId().equals("al1s") || attackerKit.getId().equals("kayi"))) {
            event.setDamage(event.getDamage() * 0.9);
            return;
        }

        // 尝试闪避
        if (tryDodge(player, attacker, event.getDamage())) {
            event.setCancelled(true);
        }
    }

    /**
     * 玩家死亡处理
     */
    public void onPlayerDeath(Player player) {
        UUID uuid = player.getUniqueId();
        exActive.put(uuid, false);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    /**
     * 回合开始处理
     */
    public void onRoundStart() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            HeroKit kit = gameManager.getPlayerHero(player);
            if (kit != null && kit.getId().equals("yuuka")) {
                exActive.put(player.getUniqueId(), false);
                player.removePotionEffect(PotionEffectType.SLOWNESS);
            }
        }
    }

    /**
     * 重置所有状态
     */
    public void resetAll() {
        lastDamageTime.clear();
        passiveCooldownEnd.clear();
        lastArmorRegenTime.clear();
        exActive.clear();
        exPrepared.clear();
        sneakLeftClickCount.clear();
        lastSneakLeftClickTime.clear();
    }
}