package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.*;
import java.util.*;

public class CoinManager implements Listener {
    private final BACS plugin;
    private final GameManager gameManager;

    // 玩家硬币数据（持久化）
    private final Map<UUID, Integer> playerCoins = new HashMap<>();

    // 整场比赛统计
    private final Map<UUID, Integer> matchCoins = new HashMap<>();
    private final Map<UUID, Integer> matchKills = new HashMap<>();

    // 回合内击杀统计
    private final Map<UUID, Integer> roundKills = new HashMap<>();

    // 优香套件拥有者（持久化）
    private final Set<UUID> yuukaKitOwners = new HashSet<>();

    private File coinFile;
    private File yuukaOwnersFile;

    public CoinManager(BACS plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        loadCoins();
        loadYuukaOwners();
    }

    // ============================================================
    // 硬币加载/保存
    // ============================================================
    private void loadCoins() {
        coinFile = new File(plugin.getDataFolder(), "coins.txt");
        if (!coinFile.exists()) return;

        try (Scanner scanner = new Scanner(coinFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    try {
                        UUID uuid = UUID.fromString(parts[0]);
                        int coins = Integer.parseInt(parts[1]);
                        playerCoins.put(uuid, coins);
                    } catch (Exception e) {}
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("加载硬币数据失败: " + e.getMessage());
        }
    }

    private void saveCoins() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            try (FileWriter writer = new FileWriter(coinFile)) {
                for (Map.Entry<UUID, Integer> entry : playerCoins.entrySet()) {
                    writer.write(entry.getKey().toString() + ":" + entry.getValue() + "\n");
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("保存硬币数据失败: " + e.getMessage());
        }
    }

    // ============================================================
    // 优香套件持久化
    // ============================================================
    private void loadYuukaOwners() {
        yuukaOwnersFile = new File(plugin.getDataFolder(), "yuuka_owners.txt");
        if (!yuukaOwnersFile.exists()) return;

        try (Scanner scanner = new Scanner(yuukaOwnersFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                try {
                    UUID uuid = UUID.fromString(line);
                    yuukaKitOwners.add(uuid);
                } catch (Exception e) {}
            }
        } catch (IOException e) {
            plugin.getLogger().warning("加载优香套件数据失败: " + e.getMessage());
        }
    }

    private void saveYuukaOwners() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            try (FileWriter writer = new FileWriter(yuukaOwnersFile)) {
                for (UUID uuid : yuukaKitOwners) {
                    writer.write(uuid.toString() + "\n");
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("保存优香套件数据失败: " + e.getMessage());
        }
    }

    // ============================================================
    // 硬币操作
    // ============================================================
    public int getCoins(Player player) {
        return playerCoins.getOrDefault(player.getUniqueId(), 0);
    }

    public void setCoins(Player player, int amount) {
        playerCoins.put(player.getUniqueId(), Math.max(0, amount));
        saveCoins();
    }

    public void addCoins(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        int current = playerCoins.getOrDefault(uuid, 0);
        playerCoins.put(uuid, current + amount);
        // 记录本场比赛获得
        matchCoins.put(uuid, matchCoins.getOrDefault(uuid, 0) + amount);
        saveCoins();
    }

    public boolean spendCoins(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        int current = playerCoins.getOrDefault(uuid, 0);
        if (current < amount) return false;
        playerCoins.put(uuid, current - amount);
        saveCoins();
        return true;
    }

    // ============================================================
    // 优香套件
    // ============================================================
    public boolean hasYuukaKit(Player player) {
        return yuukaKitOwners.contains(player.getUniqueId());
    }

    public void buyYuukaKit(Player player) {
        if (getCoins(player) < 8000) {
            player.sendMessage(Component.text("❌ 硬币不足！需要8000硬币", NamedTextColor.RED));
            return;
        }
        // 扣除硬币
        if (!spendCoins(player, 8000)) {
            player.sendMessage(Component.text("❌ 扣款失败！", NamedTextColor.RED));
            return;
        }
        yuukaKitOwners.add(player.getUniqueId());
        saveYuukaOwners();
        player.sendMessage(Component.text("✅ 成功购买优香套件！", NamedTextColor.GREEN));
    }

    // ============================================================
    // 比赛/回合统计
    // ============================================================
    public int getMatchCoins(Player player) {
        return matchCoins.getOrDefault(player.getUniqueId(), 0);
    }

    public void onKill(Player killer) {
        UUID uuid = killer.getUniqueId();
        roundKills.put(uuid, roundKills.getOrDefault(uuid, 0) + 1);
        matchKills.put(uuid, matchKills.getOrDefault(uuid, 0) + 1);
    }

    public void onRoundWin(Player player) {
        UUID uuid = player.getUniqueId();
        int kills = roundKills.getOrDefault(uuid, 0);
        int coins = 100 + (kills * 30);
        addCoins(player, coins);
        player.sendMessage(Component.text("🏆 回合胜利！获得 " + coins + " 硬币（基础100 + 击杀" + kills + "×30）", NamedTextColor.GOLD));
    }

    public void onRoundLose(Player player) {
        UUID uuid = player.getUniqueId();
        int kills = roundKills.getOrDefault(uuid, 0);
        int coins = 20 + (kills * 10);
        addCoins(player, coins);
        player.sendMessage(Component.text("💀 回合失败。获得 " + coins + " 硬币（基础20 + 击杀" + kills + "×10）", NamedTextColor.YELLOW));
    }

    public void resetRoundKills() {
        roundKills.clear();
    }

    public void onMatchWin(Player player) {
        addCoins(player, 1500);
        player.sendMessage(Component.text("🎉 比赛胜利！获得 1500 硬币！", NamedTextColor.GOLD));
    }

    public void onMatchLose(Player player) {
        addCoins(player, 500);
        player.sendMessage(Component.text("💔 比赛失败。获得 500 硬币。", NamedTextColor.YELLOW));
    }

    public void showMatchSummary(Player player) {
        int matchCoin = matchCoins.getOrDefault(player.getUniqueId(), 0);
        int currentCoins = getCoins(player);
        int kills = matchKills.getOrDefault(player.getUniqueId(), 0);

        player.sendMessage(Component.text("", NamedTextColor.WHITE));
        player.sendMessage(Component.text("§6§l========== 比赛结算 ==========", NamedTextColor.GOLD));
        player.sendMessage(Component.text("§e击杀数: §f" + kills, NamedTextColor.YELLOW));
        player.sendMessage(Component.text("§e本场获得硬币: §f" + matchCoin, NamedTextColor.YELLOW));
        player.sendMessage(Component.text("§e目前硬币数量: §f" + currentCoins, NamedTextColor.YELLOW));
        player.sendMessage(Component.text("§6§l================================", NamedTextColor.GOLD));
    }

    public void resetMatchStats() {
        matchCoins.clear();
        matchKills.clear();
        roundKills.clear();
    }

    public void resetAll() {
        saveCoins();
        resetMatchStats();
        // 不重置优香套件拥有者
    }

    // ============================================================
    // 事件监听
    // ============================================================
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!playerCoins.containsKey(uuid)) {
            playerCoins.put(uuid, 0);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        saveCoins();
    }
}