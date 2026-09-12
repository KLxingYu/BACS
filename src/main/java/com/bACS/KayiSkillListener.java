package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class KayiSkillListener implements Listener {
    private final BACS plugin;
    private final GameManager gameManager;
    private final Map<UUID, Integer> smallSkillCount = new HashMap<>();
    private final Map<UUID, Long> lastUseTime = new HashMap<>();
    private final Map<UUID, Boolean> shopPurchasedThisRound = new HashMap<>();
    private final Map<UUID, Boolean> prepareInitialized = new HashMap<>();

    private static final long SKILL_COOLDOWN = 5000;

    public KayiSkillListener(BACS plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    public int getSmallSkillCount(Player player) {
        return smallSkillCount.getOrDefault(player.getUniqueId(), 0);
    }

    public void onPrepareStart(Player player) {
        UUID uuid = player.getUniqueId();
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("kayi")) return;

        if (prepareInitialized.getOrDefault(uuid, false)) return;

        int current = smallSkillCount.getOrDefault(uuid, 0);
        if (current <= 0) {
            smallSkillCount.put(uuid, 1);
        } else if (current == 1) {
            smallSkillCount.put(uuid, 2);
        }

        prepareInitialized.put(uuid, true);
        shopPurchasedThisRound.put(uuid, false);
    }

    public void onRoundStart(Player player) {
        prepareInitialized.put(player.getUniqueId(), false);
    }

    public void onRoundEnd(Player player) {
        shopPurchasedThisRound.put(player.getUniqueId(), false);
    }

    public boolean buySmallSkill(Player player) {
        UUID uuid = player.getUniqueId();

        if (shopPurchasedThisRound.getOrDefault(uuid, false)) {
            player.sendMessage(Component.text("❌ 本回合已从商店购买过！", NamedTextColor.RED));
            return false;
        }
        int count = smallSkillCount.getOrDefault(uuid, 0);
        if (count >= 2) {
            player.sendMessage(Component.text("❌ 治疗之光次数已达上限(2次)！", NamedTextColor.RED));
            return false;
        }

        shopPurchasedThisRound.put(uuid, true);
        smallSkillCount.put(uuid, count + 1);
        player.sendMessage(Component.text("✅ 购买成功！治疗之光次数+1", NamedTextColor.GREEN));
        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;
        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null || !kit.getId().equals("kayi")) return;

        if (item.getType() != Material.DIAMOND_SWORD) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().displayName().toString().contains("军用匕首")) return;

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        event.setCancelled(true);

        if (gameManager.getState() != GameState.ROUND_ACTIVE) {
            player.sendMessage(Component.text("❌ 只能在回合进行中使用！", NamedTextColor.RED));
            return;
        }

        UUID uuid = player.getUniqueId();
        int count = smallSkillCount.getOrDefault(uuid, 0);
        if (count <= 0) {
            player.sendMessage(Component.text("❌ 没有治疗之光次数！", NamedTextColor.RED));
            return;
        }

        long lastUse = lastUseTime.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < SKILL_COOLDOWN) {
            long remaining = (SKILL_COOLDOWN - (System.currentTimeMillis() - lastUse)) / 1000 + 1;
            player.sendMessage(Component.text("❌ 冷却中: " + remaining + "秒", NamedTextColor.RED));
            return;
        }

        useHealSkill(player);
        lastUseTime.put(uuid, System.currentTimeMillis());
        smallSkillCount.put(uuid, count - 1);
    }

    private void useHealSkill(Player user) {
        String userTeam = gameManager.getPlayerTeam(user);
        if (userTeam == null) return;

        int healedCount = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.isOnline()) continue;
            if (target.getLocation().distance(user.getLocation()) > 10) continue;

            String targetTeam = gameManager.getPlayerTeam(target);
            if (targetTeam == null || !targetTeam.equals(userTeam)) continue;

            HeroKit targetKit = gameManager.getPlayerHero(target);
            double healMultiplier = 1.0;
            if (targetKit != null && targetKit.getId().equals("al1s")) {
                healMultiplier = 1.2;
            }

            double healAmount = 30 * healMultiplier;
            double maxHealth = target.getMaxHealth();
            double currentHealth = target.getHealth();
            target.setHealth(Math.min(maxHealth, currentHealth + healAmount));

            int armorHeal = 0;
            if (targetKit != null) {
                armorHeal = (int)(10 * healMultiplier);
                int currentArmor = gameManager.getPlayerCurrentArmor(target);
                int maxArmor = targetKit.getMaxArmor() + gameManager.getPlayerExtraMaxArmor(target);
                gameManager.setPlayerCurrentArmor(target, Math.min(maxArmor, currentArmor + armorHeal));
            }

            target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1, true, false));

            target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
            target.getWorld().spawnParticle(Particle.GLOW, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.01);

            target.sendMessage(Component.text("✨ 被治疗之光治愈！+" + (int)healAmount + "生命 +" + armorHeal + "护甲", NamedTextColor.GREEN));
            healedCount++;
        }

        user.sendMessage(Component.text("✨ 治疗之光释放！治愈了 " + healedCount + " 名队友", NamedTextColor.AQUA));
        user.getWorld().playSound(user.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
    }

    public void resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        smallSkillCount.remove(uuid);
        lastUseTime.remove(uuid);
        shopPurchasedThisRound.remove(uuid);
        prepareInitialized.remove(uuid);
    }

    public void resetAll() {
        smallSkillCount.clear();
        lastUseTime.clear();
        shopPurchasedThisRound.clear();
        prepareInitialized.clear();
    }
}