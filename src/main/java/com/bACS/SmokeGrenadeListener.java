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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class SmokeGrenadeListener implements Listener {
    private final BACS plugin;
    private final GameManager gameManager;

    public SmokeGrenadeListener(BACS plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;
        if (item.getType() != Material.POTATO) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().displayName().toString().contains("烟雾弹")) return;

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        event.setCancelled(true);

        if (gameManager.getState() != GameState.ROUND_ACTIVE) {
            player.sendMessage(Component.text("❌ 只能在回合进行中使用烟雾弹！", NamedTextColor.RED));
            return;
        }

        item.setAmount(item.getAmount() - 1);
        throwSmokeGrenade(player);
    }

    private void throwSmokeGrenade(Player player) {
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().clone();

        Item smokeGrenadeItem = player.getWorld().dropItem(startLoc, new ItemStack(Material.POTATO));
        Vector velocity = direction.multiply(1.8).add(new Vector(0, 0.25, 0));
        smokeGrenadeItem.setVelocity(velocity);
        smokeGrenadeItem.setPickupDelay(Integer.MAX_VALUE);
        smokeGrenadeItem.setGlowing(true);
        smokeGrenadeItem.setInvulnerable(true);

        player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0f, 1.0f);

        new BukkitRunnable() {
            int stationaryTicks = 0;

            @Override
            public void run() {
                if (smokeGrenadeItem.isDead() || !smokeGrenadeItem.isValid()) {
                    cancel();
                    return;
                }

                // 检查是否停止移动
                if (smokeGrenadeItem.getVelocity().length() < 0.1) {
                    stationaryTicks++;
                    if (stationaryTicks >= 10) { // 停止移动0.5秒后
                        createSmokeEffect(smokeGrenadeItem.getLocation());
                        smokeGrenadeItem.remove();
                        cancel();
                    }
                } else {
                    stationaryTicks = 0;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void createSmokeEffect(Location loc) {
        World world = loc.getWorld();
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 1.0f);

        // 生成大量烟雾粒子（5秒）
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks > 100) { // 5秒（100 tick）
                    cancel();
                    return;
                }

                // 生成烟雾粒子
                for (int i = 0; i < 30; i++) {
                    double x = loc.getX() + (Math.random() - 0.5) * 4;
                    double y = loc.getY() + Math.random() * 3;
                    double z = loc.getZ() + (Math.random() - 0.5) * 4;
                    Location particleLoc = new Location(world, x, y, z);
                    world.spawnParticle(Particle.EXPLOSION, particleLoc, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.SMOKE, particleLoc, 3, 1, 1, 1, 0.01);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public static ItemStack createSmokeGrenade() {
        ItemStack smokeGrenade = new ItemStack(Material.POTATO);
        ItemMeta meta = smokeGrenade.getItemMeta();
        meta.displayName(Component.text("💨 烟雾弹", NamedTextColor.GRAY));
        meta.lore(List.of(
                Component.text("右键掷出", NamedTextColor.GRAY),
                Component.text("落地后生成烟雾", NamedTextColor.YELLOW),
                Component.text("阻挡视野", NamedTextColor.DARK_GRAY)
        ));
        meta.setUnbreakable(true);
        smokeGrenade.setItemMeta(meta);
        return smokeGrenade;
    }
}