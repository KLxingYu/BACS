package com.bACS;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockProtectListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // 管理员不受限制
        if (player.hasPermission("bacs.admin")) return;

        // 玩家不能破坏任何方块
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // 管理员不受限制
        if (player.hasPermission("bacs.admin")) return;

        // 玩家不能放置任何方块
        event.setCancelled(true);
    }
}