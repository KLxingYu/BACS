package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ExSkillTriggerListener implements Listener {
    private final BACS plugin;
    private final GameManager gameManager;

    public ExSkillTriggerListener(BACS plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;
        if (item.getType() != Material.NETHERITE_BLOCK) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().displayName().toString().contains("EX技能")) return;

        if (gameManager.getState() != GameState.ROUND_ACTIVE) {
            player.sendMessage(Component.text("❌ 只能在回合进行中使用！", NamedTextColor.RED));
            return;
        }
        if (!gameManager.isPlayerAlive(player)) return;

        HeroKit kit = gameManager.getPlayerHero(player);
        if (kit == null) return;

        // ===== 右键触发EX技能 =====
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            triggerExSkill(player, kit);
            return;
        }

        // ===== 左键触发小技能1 =====
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            triggerSmallSkill(player, kit);
        }
    }

    // ============================================================
    // EX技能触发（根据英雄ID分发）
    // ============================================================
    private void triggerExSkill(Player player, HeroKit kit) {
        String heroId = kit.getId();

        switch (heroId) {
            // ===== 星野·防御 =====
            case "hoshino_defense":
                if (BACS.getInstance().getHoshinoSkillListener() != null) {
                    BACS.getInstance().getHoshinoSkillListener().activateDefenseEX(player);
                }
                break;

            // ===== 星野·攻击 =====
            case "hoshino_attack":
                if (BACS.getInstance().getHoshinoSkillListener() != null) {
                    BACS.getInstance().getHoshinoSkillListener().activateAttackEX(player);
                }
                break;

            // ===== 拉洛芙 =====
            case "rakufu":
                if (BACS.getInstance().getRakuFuSkillListener() != null) {
                    BACS.getInstance().getRakuFuSkillListener().triggerEX(player);
                }
                break;

            // ===== 芹奈 =====
            case "serina":
                // 芹奈EX技能：复活
                if (BACS.getInstance().getSerinaSkillListener() != null) {
                    // 芹奈的EX触发方法（如果有的话）
                    // 如果芹奈的EX是蹲下右键主武器触发，则这里不需要处理
                    // 暂时给出提示
                    player.sendMessage(Component.text("❌ 芹奈的EX技能需要蹲下右键主武器触发！", NamedTextColor.YELLOW));
                }
                break;

            // ===== 凯伊 =====
            case "kayi":
                // 凯伊EX技能：治疗之光
                if (BACS.getInstance().getKayiSkillListener() != null) {
                    // 凯伊的EX触发方法（如果有的话）
                    player.sendMessage(Component.text("❌ 凯伊的EX技能需要使用近战武器右键触发！", NamedTextColor.YELLOW));
                }
                break;

            // ===== 泉 =====
            case "izumi":
                // 泉EX技能
                if (BACS.getInstance().getIzumiSkillListener() != null) {
                    player.sendMessage(Component.text("❌ 泉的EX技能需要使用近战武器蹲下右键触发！", NamedTextColor.YELLOW));
                }
                break;

            // ===== 未花 =====
            case "mika":
                // 未花EX技能：龙息弹
                if (BACS.getInstance().getMikaSkillListener() != null) {
                    player.sendMessage(Component.text("❌ 未花的EX技能需要蹲下丢弃主武器触发！", NamedTextColor.YELLOW));
                }
                break;

            // ===== 优香 =====
            case "yuuka":
                // 优香EX技能：200护甲
                if (BACS.getInstance().getYuukaSkillListener() != null) {
                    BACS.getInstance().getYuukaSkillListener().triggerEX(player);
                }
                break;

            // ===== 黑子 =====
            case "kuroko":
                // 黑子EX技能
                gameManager.getExSkillManager().activateEX(player);
                break;

            // ===== AL1S =====
            case "al1s":
                // AL1S EX技能
                gameManager.getExSkillManager().activateEX(player);
                break;

            default:
                // 默认EX技能
                gameManager.getExSkillManager().activateEX(player);
                break;
        }
    }

    // ============================================================
    // 小技能1触发（根据英雄ID分发）
    // ============================================================
    private void triggerSmallSkill(Player player, HeroKit kit) {
        String heroId = kit.getId();

        switch (heroId) {
            // ===== 未花 =====
            case "mika":
                // 未花小技能：子弹时间
                if (BACS.getInstance().getMikaSkillListener() != null) {
                    BACS.getInstance().getMikaSkillListener().triggerSmallSkill(player);
                }
                break;

            // ===== 泉 =====
            case "izumi":
                // 泉小技能1：无敌+速度
                // 泉的小技能1通过丢弃近战武器触发
                player.sendMessage(Component.text("❌ 泉的小技能需要丢弃近战武器触发！", NamedTextColor.YELLOW));
                break;

            // ===== 凯伊 =====
            case "kayi":
                // 凯伊小技能：治疗之光
                // 凯伊的小技能通过近战武器右键触发
                player.sendMessage(Component.text("❌ 凯伊的小技能需要使用近战武器右键触发！", NamedTextColor.YELLOW));
                break;

            // ===== 优香 =====
            case "yuuka":
                // 优香没有独立小技能（只有EX）
                player.sendMessage(Component.text("❌ 优香没有小技能！", NamedTextColor.RED));
                break;

            // ===== 芹奈 =====
            case "serina":
                // 芹奈小技能：治疗之杖
                // 芹奈的小技能通过蹲下丢弃近战武器触发
                player.sendMessage(Component.text("❌ 芹奈的小技能需要蹲下丢弃近战武器触发！", NamedTextColor.YELLOW));
                break;

            // ===== 拉洛芙 =====
            case "rakufu":
                // 拉洛芙主动技能：探测箭矢
                // 拉洛芙的主动技能通过右键箭矢触发
                player.sendMessage(Component.text("❌ 拉洛芙的主动技能需要右键探测箭矢触发！", NamedTextColor.YELLOW));
                break;

            // ===== 星野·防御 =====
            case "hoshino_defense":
                // 星野防御没有小技能
                player.sendMessage(Component.text("❌ 星野·防御没有小技能！", NamedTextColor.RED));
                break;

            // ===== 星野·攻击 =====
            case "hoshino_attack":
                // 星野攻击没有小技能
                player.sendMessage(Component.text("❌ 星野·攻击没有小技能！", NamedTextColor.RED));
                break;

            default:
                player.sendMessage(Component.text("❌ 该英雄没有小技能！", NamedTextColor.RED));
                break;
        }
    }
}