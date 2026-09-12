package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class DamageListener implements Listener {
    private final GameManager gameManager;

    public DamageListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * 判断攻击者是否手持超新星·光之剑
     */
    private boolean isAl1sSupernova(Player attacker) {
        HeroKit attackerKit = gameManager.getPlayerHero(attacker);
        if (attackerKit == null || !attackerKit.getId().equals("al1s")) return false;
        ItemStack hand = attacker.getInventory().getItemInMainHand();
        return hand != null && hand.hasItemMeta() && hand.getItemMeta().hasDisplayName() &&
                hand.getItemMeta().displayName().toString().contains("超新星·光之剑");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) return;

        if (gameManager.getState() != GameState.ROUND_ACTIVE) {
            event.setCancelled(true);
            return;
        }

        if (!gameManager.isPlayerAlive(player)) {
            event.setCancelled(true);
            return;
        }

        // 取消受伤冷却
        player.setMaximumNoDamageTicks(0);

        // ===== 稳定值机制（仅对玩家攻击有效） =====
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent ee = (EntityDamageByEntityEvent) event;
            if (ee.getDamager() instanceof Player attacker) {
                if (gameManager.isPlayerAlive(attacker)) {
                    BACS plugin = BACS.getInstance();
                    if (plugin != null) {
                        StabilityManager sm = plugin.getStabilityManager();
                        if (sm != null) {
                            int effective = sm.getEffectiveStability(attacker);
                            double multiplier = sm.getDamageMultiplier(effective);
                            double newDamage = event.getDamage() * multiplier;
                            event.setDamage(newDamage);
                        }
                    }
                }
            }
        }

        IzumiSkillListener izumi = BACS.getInstance().getIzumiSkillListener();
        if (izumi != null && izumi.isInvincible(player)) {
            event.setCancelled(true);
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                event.getDamage() >= 999) {
            player.setHealth(0);
            return;
        }

        gameManager.getShopManager().onPlayerDamaged(player);

        YuukaSkillListener yuuka = BACS.getInstance().getYuukaSkillListener();
        if (yuuka != null) {
            yuuka.onPlayerDamaged(player);
        }

        double damage = event.getDamage();

        // ===== 星野防御形态护盾减伤 =====
        HeroKit playerKit = gameManager.getPlayerHero(player);
        if (playerKit != null && playerKit.getId().equals("hoshino_defense")) {
            Player attacker = null;
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent ee = (EntityDamageByEntityEvent) event;
                if (ee.getDamager() instanceof Player) {
                    attacker = (Player) ee.getDamager();
                }
            }
            if (BACS.getInstance().getHoshinoSkillListener() != null) {
                damage = BACS.getInstance().getHoshinoSkillListener()
                        .handleShieldDamage(player, damage, attacker);
            }
        }

        // 泉恢复期间受伤倍数
        if (izumi != null && izumi.getHealingMultiplier(player) > 1.0) {
            damage *= izumi.getHealingMultiplier(player);
        }

        if (izumi != null && izumi.absorbDamage(player, damage)) {
            event.setCancelled(true);
            return;
        }

        ShopManager shop = gameManager.getShopManager();

        int reductionCount = shop.getDamageReductionCount(player);
        if (reductionCount > 0) {
            damage *= (1 - reductionCount * 0.0095);
        }

        EXSkillManager exSkillManager = gameManager.getExSkillManager();

        if (exSkillManager != null && exSkillManager.getSpecialShield(player) > 0) {
            if (exSkillManager.consumeSpecialShield(player, damage)) {
                event.setCancelled(true);
                return;
            }
        }

        if (exSkillManager != null && exSkillManager.getTempShield(player) > 0) {
            if (exSkillManager.consumeTempShield(player, damage)) {
                event.setCancelled(true);
                return;
            }
        }

        if (shop.hasBodyArmor(player) && shop.getBodyArmorDurability(player) > 0) {
            double reduced = damage * 0.15;
            damage -= reduced;
            shop.setBodyArmorDurability(player, shop.getBodyArmorDurability(player) - (int)Math.ceil(reduced));
        }

        if (shop.hasHelmet(player) && shop.getHelmetDurability(player) > 0) {
            double reduced = damage * 0.05;
            damage -= reduced;
            shop.setHelmetDurability(player, shop.getHelmetDurability(player) - (int)Math.round(reduced * 1.5));
        }

        if (player.hasPotionEffect(PotionEffectType.GLOWING)) {
            damage *= 1.15;
        }

        // ★ AL1S 超新星·光之剑：无视 30% 护甲减伤
        boolean al1sIgnoreArmor = false;
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent ee = (EntityDamageByEntityEvent) event;
            if (ee.getDamager() instanceof Player attacker) {
                if (isAl1sSupernova(attacker)) {
                    al1sIgnoreArmor = true;
                }
            }
        }

        if (shop.hasRegenArmor(player)) {
            double absorbed = damage * 0.88;
            double remaining = damage * 0.12;
            int currentArmor = gameManager.getPlayerCurrentArmor(player);
            if (currentArmor > 0) {
                int armorAbsorb = (int) Math.min(currentArmor, absorbed);
                gameManager.setPlayerCurrentArmor(player, currentArmor - armorAbsorb);
                event.setDamage(remaining);
                return;
            }
        }

        if (shop.hasHeavyArmor(player)) {
            int currentArmor = gameManager.getPlayerCurrentArmor(player);
            if (currentArmor > 0) {
                double absorptionMultiplier = 0.8;
                HeroKit kit = gameManager.getPlayerHero(player);
                if (kit != null && kit.getId().equals("yuuka")) {
                    absorptionMultiplier = 0.5;
                }
                // ★ AL1S 超新星无视 30% 护甲
                if (al1sIgnoreArmor) {
                    absorptionMultiplier *= 0.7;
                }
                double absorbed = damage * absorptionMultiplier;
                double remaining = damage * (1 - absorptionMultiplier);
                int armorAbsorb = (int) Math.min(currentArmor, absorbed);
                gameManager.setPlayerCurrentArmor(player, currentArmor - armorAbsorb);
                event.setDamage(remaining);
                return;
            }
        }

        int armor = gameManager.getPlayerCurrentArmor(player);
        if (armor > 0) {
            double absorptionMultiplier = 0.8;
            HeroKit kit = gameManager.getPlayerHero(player);
            if (kit != null && kit.getId().equals("yuuka")) {
                absorptionMultiplier = 0.5;
            }
            // ★ AL1S 超新星无视 30% 护甲
            if (al1sIgnoreArmor) {
                absorptionMultiplier *= 0.7;
            }
            double absorbed = damage * absorptionMultiplier;
            double remaining = damage * (1 - absorptionMultiplier);
            int armorAbsorb = (int) Math.min(armor, absorbed);
            gameManager.setPlayerCurrentArmor(player, armor - armorAbsorb);
            event.setDamage(remaining);
        } else {
            event.setDamage(damage);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        event.getDrops().clear();
        event.setKeepInventory(true);
        gameManager.getShopManager().onPlayerDeath(player);

        Player killer = player.getKiller();
        if (killer != null) {
            MikaSkillListener mikaListener = BACS.getInstance().getMikaSkillListener();
            if (mikaListener != null) {
                mikaListener.onKill(killer, player);
                mikaListener.onBulletTimeKill(killer);
            }

            if (BACS.getInstance().getCoinManager() != null) {
                BACS.getInstance().getCoinManager().onKill(killer);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.spigot().respawn();
                    player.setGameMode(GameMode.SPECTATOR);
                    gameManager.handlePlayerDeath(player);
                }
            }
        }.runTaskLater(BACS.getInstance(), 1L);
    }
}