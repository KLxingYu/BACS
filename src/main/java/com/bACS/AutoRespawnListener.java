package com.bACS;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoRespawnListener implements Listener {
    private final GameManager gameManager;

    public AutoRespawnListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // 延迟1tick后处理重生
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    // 强制重生
                    player.spigot().respawn();
                    // 设置为旁观者模式
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }
        }.runTaskLater(BACS.getInstance(), 1L);
    }
}