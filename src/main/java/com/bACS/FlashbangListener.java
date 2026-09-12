package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class FlashbangListener implements Listener {

    private final BACS plugin;
    private final GameManager gameManager;
    private final ShopManager shopManager;

    public FlashbangListener(BACS plugin, GameManager gameManager, ShopManager shopManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.shopManager = shopManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.GOLDEN_CARROT) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().displayName().toString().contains("闪光弹")) return;

        Action action = event.getAction();
        boolean isLeftClick = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        boolean isRightClick = (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK);

        if (!isLeftClick && !isRightClick) return;

        event.setCancelled(true);

        if (gameManager.getState() != GameState.ROUND_ACTIVE) {
            player.sendMessage(Component.text("❌ 只能在回合进行中使用闪光弹！", NamedTextColor.RED));
            return;
        }

        item.setAmount(item.getAmount() - 1);
        double speedMultiplier = isLeftClick ? 2.8 : 1.6;
        throwFlashbang(player, speedMultiplier);
    }

    private void throwFlashbang(Player player, double speedMultiplier) {
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().clone();

        Item flashbangItem = player.getWorld().dropItem(startLoc, new ItemStack(Material.GOLDEN_CARROT));
        Vector velocity = direction.multiply(speedMultiplier).add(new Vector(0, 0.25, 0));
        flashbangItem.setVelocity(velocity);
        flashbangItem.setPickupDelay(Integer.MAX_VALUE);
        flashbangItem.setGlowing(true);
        flashbangItem.setInvulnerable(true);
        flashbangItem.setPersistent(true);

        player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0f, 1.0f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                if (flashbangItem.isDead() || !flashbangItem.isValid()) {
                    cancel();
                    return;
                }

                if (ticks >= 46) {
                    explodeFlashbang(flashbangItem);
                    flashbangItem.remove();
                    cancel();
                    return;
                }

                Location loc = flashbangItem.getLocation();
                loc.getWorld().spawnParticle(Particle.FLAME, loc, 10, 0.2, 0.2, 0.2, 0.01);
                loc.getWorld().spawnParticle(Particle.GLOW, loc, 5, 0.1, 0.1, 0.1, 0.01);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void explodeFlashbang(Item flashbangItem) {
        Location loc = flashbangItem.getLocation();
        World world = loc.getWorld();

        world.spawnParticle(Particle.EXPLOSION, loc, 5, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3, 0.5, 0.5, 0.5, 0);
        world.spawnParticle(Particle.SMOKE, loc, 20, 1, 1, 1, 0.1);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.0f);

        for (Player player : world.getPlayers()) {
            double distance = player.getLocation().distance(loc);
            boolean hasLineOfSight = hasLineOfSight(player, loc);
            boolean isFacing = isFacing(player, loc);

            if (distance <= 10.0) {
                if (hasLineOfSight && isFacing) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, true, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2, true, false));
                    player.sendMessage(Component.text("💥 被闪光弹致盲！", NamedTextColor.RED));
                } else if (hasLineOfSight && !isFacing) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, true, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, true, false));
                    player.sendMessage(Component.text("💥 被闪光弹余光影响！", NamedTextColor.GOLD));
                } else if (distance <= 5.0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, true, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 1, true, false));
                    player.sendMessage(Component.text("💥 被闪光弹余波影响！", NamedTextColor.YELLOW));
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, true, false));
                }
            }
        }
    }

    private boolean hasLineOfSight(Player player, Location target) {
        Location eye = player.getEyeLocation();
        Vector direction = target.toVector().subtract(eye.toVector());
        double maxDistance = direction.length();
        direction.normalize();

        RayTraceResult result = player.getWorld().rayTraceBlocks(
                eye,
                direction,
                maxDistance,
                FluidCollisionMode.NEVER,
                true,
                block -> !block.isPassable() && block.getType().isOccluding()
        );

        return result == null || result.getHitBlock() == null;
    }

    private boolean isFacing(Player player, Location target) {
        Vector toTarget = target.toVector().subtract(player.getEyeLocation().toVector()).normalize();
        Vector playerDirection = player.getEyeLocation().getDirection();
        double dot = toTarget.dot(playerDirection);
        return dot > 0.5;
    }
}