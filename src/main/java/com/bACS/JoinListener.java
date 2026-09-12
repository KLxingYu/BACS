package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
    private final BACS plugin;
    public JoinListener(BACS plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GameManager manager = plugin.getGameManager();
        World voidWorld = plugin.getVoidWorld();

        if (voidWorld != null) {
            player.teleport(new Location(voidWorld, 0, 101, 0));
            player.sendMessage(Component.text("🏔️ 欢迎来到虚空世界！", NamedTextColor.GOLD));
            createSafePlatform(player, voidWorld);
        }

        event.joinMessage(Component.text("▶ ", NamedTextColor.GRAY)
                .append(player.displayName())
                .append(Component.text(" 加入了游戏！", NamedTextColor.GREEN)));

        manager.updateScoreboard();

        GameState state = manager.getState();
        if (state == GameState.WAITING || state == GameState.COUNTDOWN) {
            manager.checkAndStartCountdown();
        } else if (state == GameState.PREPARE) {
            // 允许玩家选择阵营
            manager.openTeamMenu(player);
            player.sendMessage(Component.text("⚠ 请选择阵营！", NamedTextColor.YELLOW));
        } else if (state == GameState.ROUND_ACTIVE || state == GameState.ROUND_END || state == GameState.GAME_OVER) {
            // 游戏进行中，加入为观众
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            player.sendMessage(Component.text("⚠ 游戏正在进行中，你已进入观战模式！", NamedTextColor.YELLOW));
            String team = manager.getPlayerTeam(player);
            if (team != null) {
                if (team.equals(GameManager.TEAM_ATTACKER)) {
                    player.teleport(manager.getAttackerSpawnLocation());
                } else {
                    player.teleport(manager.getDefenderSpawnLocation());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GameManager manager = plugin.getGameManager();
        manager.handlePlayerDeath(player);
        event.setDeathMessage(null);
    }

    private void createSafePlatform(Player player, World world) {
        Location loc = player.getLocation();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                Location blockLoc = new Location(world,
                        loc.getBlockX() + x,
                        loc.getBlockY() - 1,
                        loc.getBlockZ() + z
                );
                if (blockLoc.getBlock().isEmpty()) {
                    blockLoc.getBlock().setType(org.bukkit.Material.GLASS);
                }
            }
        }
    }
}