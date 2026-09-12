package com.bACS;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class NetworkManager implements PluginMessageListener {
    private final BACS plugin;
    private static final String CHANNEL = "bacsmod:kill_sound";

    public NetworkManager(BACS plugin) {
        this.plugin = plugin;
        // 注册消息通道
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    /**
     * 发送击杀音效数据到客户端模组
     * @param player 要发送到的玩家
     * @param streak 当前连杀数
     * @param pitch 音调 (0.5 ~ 2.0)
     * @param volume 音量 (0.1 ~ 1.0)
     * @param isSpecial 是否特殊音效
     * @param melodyType 旋律类型
     */
    public void sendKillSound(Player player, int streak, float pitch, float volume, boolean isSpecial, String melodyType) {
        if (player == null || !player.isOnline()) return;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        // 写入数据
        out.writeInt(streak);          // 连杀数
        out.writeFloat(pitch);         // 音调
        out.writeFloat(volume);        // 音量
        out.writeBoolean(isSpecial);   // 是否特殊音效
        out.writeUTF(melodyType != null ? melodyType : ""); // 旋律类型
        out.writeUTF(player.getName()); // 玩家名称

        // 发送消息
        player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
    }

    /**
     * 发送击杀音效（简化版）
     */
    public void sendKillSound(Player player, int streak) {
        // 根据连杀数自动计算音调和类型
        boolean isSpecial = streak >= 5;
        float pitch = 0.5f + (streak - 1) * 0.1f;
        float volume = 0.8f;
        String melodyType = isSpecial ? "ace" : "";

        sendKillSound(player, streak, pitch, volume, isSpecial, melodyType);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        // 接收来自客户端的消息（如果需要）
        if (!channel.equals(CHANNEL)) return;
        // 处理客户端发来的消息...
    }

    /**
     * 关闭时清理
     */
    public void cleanup() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL);
    }
}