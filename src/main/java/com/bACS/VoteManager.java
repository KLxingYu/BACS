package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class VoteManager {
    private final BACS plugin;
    private final GameManager gameManager;

    private final Map<UUID, Boolean> votes = new HashMap<>();  // true=同意, false=拒绝
    private boolean votingActive = false;
    private int voteTaskId = -1;
    private int voteSeconds = 0;

    private double currentThreshold = 0.5;

    public VoteManager(BACS plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    /**
     * 开始一次投票
     * @param threshold 拒绝加时的阈值（0.5 或 2/3）
     */
    public void startVote(double threshold) {
        this.votes.clear();
        this.votingActive = true;
        this.voteSeconds = 20;
        this.currentThreshold = threshold;

        gameManager.setState(GameState.VOTE);

        Bukkit.broadcast(Component.text("", NamedTextColor.WHITE));
        Bukkit.broadcast(Component.text("⚖ ⚖ ⚖  加 时 投 票  ⚖ ⚖ ⚖", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        Bukkit.broadcast(Component.text("是否继续加时？", NamedTextColor.YELLOW));
        Bukkit.broadcast(Component.text("/vote agree §7- 同意继续加时", NamedTextColor.GREEN));
        Bukkit.broadcast(Component.text("/vote disagree §7- 拒绝加时（判平局）", NamedTextColor.RED));
        Bukkit.broadcast(Component.text("限时 " + voteSeconds + " 秒", NamedTextColor.GRAY));
        Bukkit.broadcast(Component.text("", NamedTextColor.WHITE));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(
                    "§6§l⚖ 加时投票",
                    "§e输入 /vote agree 或 /vote disagree",
                    10, 60, 20
            );
        }

        // 启动倒计时
        if (voteTaskId != -1) {
            Bukkit.getScheduler().cancelTask(voteTaskId);
            voteTaskId = -1;
        }
        voteTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!votingActive) {
                    cancel();
                    return;
                }
                if (voteSeconds <= 0) {
                    endVote();
                    cancel();
                    return;
                }
                voteSeconds--;
                if (voteSeconds % 5 == 0 && voteSeconds > 0) {
                    Bukkit.broadcast(Component.text("⏳ 投票剩余 " + voteSeconds + " 秒", NamedTextColor.YELLOW));
                }
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    /**
     * 玩家投票
     */
    public void castVote(Player player, boolean agree) {
        if (!votingActive) {
            player.sendMessage(Component.text("❌ 当前不是投票阶段！", NamedTextColor.RED));
            return;
        }

        UUID uuid = player.getUniqueId();
        if (votes.containsKey(uuid)) {
            player.sendMessage(Component.text("❌ 你已经投过票了！", NamedTextColor.RED));
            return;
        }

        votes.put(uuid, agree);
        player.sendMessage(Component.text(agree ? "✅ 你投了同意" : "❌ 你投了拒绝",
                agree ? NamedTextColor.GREEN : NamedTextColor.RED));

        if (votes.size() >= Bukkit.getOnlinePlayers().size()) {
            endVote();
        }
    }

    /**
     * 结束投票
     */
    private void endVote() {
        if (!votingActive) return;
        votingActive = false;

        if (voteTaskId != -1) {
            Bukkit.getScheduler().cancelTask(voteTaskId);
            voteTaskId = -1;
        }

        int totalPlayers = Bukkit.getOnlinePlayers().size();
        int agreeCount = 0;
        int disagreeCount = 0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            Boolean vote = votes.get(p.getUniqueId());
            if (vote == null || !vote) {
                disagreeCount++;
            } else {
                agreeCount++;
            }
        }

        Bukkit.broadcast(Component.text("", NamedTextColor.WHITE));
        Bukkit.broadcast(Component.text("⚖ ⚖ ⚖  投 票 结 果  ⚖ ⚖ ⚖", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        Bukkit.broadcast(Component.text("同意继续加时: §a" + agreeCount + " §7人", NamedTextColor.WHITE));
        Bukkit.broadcast(Component.text("拒绝加时（含未投票）: §c" + disagreeCount + " §7人", NamedTextColor.WHITE));
        Bukkit.broadcast(Component.text("总人数: §f" + totalPlayers, NamedTextColor.WHITE));

        for (Player p : Bukkit.getOnlinePlayers()) {
            Boolean vote = votes.get(p.getUniqueId());
            String voteText;
            if (vote == null) {
                voteText = "§7未投票（视为拒绝）";
            } else if (vote) {
                voteText = "§a同意";
            } else {
                voteText = "§c拒绝";
            }
            p.sendMessage(Component.text("你的投票: " + voteText, NamedTextColor.GRAY));
        }

        boolean tie = false;
        double disagreeRatio = (double) disagreeCount / Math.max(1, totalPlayers);
        if (disagreeRatio >= currentThreshold) {
            tie = true;
            Bukkit.broadcast(Component.text("⚖ 拒绝加时人数 ≥ " + (int)(currentThreshold * 100) + "%，判定比赛平局！", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true));
        } else {
            Bukkit.broadcast(Component.text("⚔ 更多人同意继续加时！进入下一局。", NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true));
        }
        Bukkit.broadcast(Component.text("", NamedTextColor.WHITE));

        gameManager.onVoteFinished(tie);
    }

    /**
     * 强制结束投票（用于游戏重置）
     */
    public void forceEnd() {
        votingActive = false;
        if (voteTaskId != -1) {
            Bukkit.getScheduler().cancelTask(voteTaskId);
            voteTaskId = -1;
        }
        votes.clear();
    }

    public boolean isVotingActive() {
        return votingActive;
    }

    public int getVoteSeconds() {
        return voteSeconds;
    }
}