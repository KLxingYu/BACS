package com.bACS;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scoreboard.*;

import java.util.*;

public class GameManager {
    public static final String TEAM_ATTACKER = "ATTACKER";
    public static final String TEAM_DEFENDER = "DEFENDER";

    private static final double ATTACKER_X1 = 230.5, ATTACKER_X2 = 234.5, ATTACKER_Y = 81, ATTACKER_Z1 = 32.5, ATTACKER_Z2 = 34.5;
    private static final double DEFENDER_X1 = 160.5, DEFENDER_X2 = 162.0, DEFENDER_Y = 74, DEFENDER_Z1 = 9.5, DEFENDER_Z2 = 16.5;

    private static final double SHOP_DEFENDER_X1 = 159.5;
    private static final double SHOP_DEFENDER_X2 = 166.5;
    private static final double SHOP_DEFENDER_Y1 = 74;
    private static final double SHOP_DEFENDER_Y2 = 76;
    private static final double SHOP_DEFENDER_Z1 = 9.5;
    private static final double SHOP_DEFENDER_Z2 = 16.5;

    private static final double SHOP_ATTACKER_X1 = 228.0;
    private static final double SHOP_ATTACKER_X2 = 236.5;
    private static final double SHOP_ATTACKER_Y1 = 81;
    private static final double SHOP_ATTACKER_Y2 = 84;
    private static final double SHOP_ATTACKER_Z1 = 30.0;
    private static final double SHOP_ATTACKER_Z2 = 36.5;

    public static final Component SHOP_ITEM_NAME = Component.text("商店", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true);

    private static final int PREPARE_TIME_FIRST = 45;
    private static final int PREPARE_TIME_NORMAL = 20;
    private static final int HERO_SELECT_TIME = 25;

    // ★ 攻防转换
    private static final int HALF_TIME_ROUND = 9;            // 第 9 回合开始攻防互换
    private static final int HALF_TIME_WARNING_ROUND = 8;    // 第 8 回合结束提示

    // ★ 加时赛
    private static final int OVERTIME_START_ROUND = 19;      // 加时第 1 局 = 回合 19
    private static final int OVERTIME_ECONOMY = 5000;        // 加时每局经济 set 5000

    public static final List<HeroKit> HERO_KITS = new ArrayList<>();

    static {
        // AL1S
        HERO_KITS.add(new HeroKit.Builder()
                .id("al1s")
                .name("AL1S")
                .displayName(Component.text("AL1S", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true))
                .maxHealth(80)
                .maxArmor(100)
                .armorDamageReduction(0.8)
                .armorIgnore(0.2)
                .canPenetrate(true)
                .canDamageTeammates(false)
                .weaponMaterial(Material.DIAMOND_HOE)
                .weaponName("超新星·光之剑")
                .chargeTime(0)
                .laserDamage(195)
                .laserArmorIgnore(0.2)
                .canPenetrateBlocks(true)
                .blockDamageReduction(0.785)
                .magazineSize(5)
                .maxMagazines(1)
                .initialReserveAmmo(10)
                .maxReserveAmmo(10)
                .exSkillMaxCharge(8)
                .meleeWeaponMaterial(Material.GOLDEN_SWORD)
                .meleeWeaponName("匕首")
                .meleeDamage(15)
                .meleeCritDamage(25)
                .meleeBackstabDamage(150)
                .meleeBackstabCritDamage(200)
                .meleeAttackSpeed(2.0)
                .meleeSpeedBoost(0.03)
                .weaponSpeedReduction(0.20)
                .description("狙击", "透视")
                .build()
        );

        // 黑子
        HERO_KITS.add(new HeroKit.Builder()
                .id("kuroko")
                .name("黑子")
                .displayName(Component.text("黑子", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.BOLD, true))
                .maxHealth(100)
                .maxArmor(50)
                .armorDamageReduction(0.5)
                .armorIgnore(0.0)
                .canPenetrate(false)
                .canDamageTeammates(false)
                .weaponMaterial(Material.IRON_HOE)
                .weaponName("α·突击步枪")
                .chargeTime(0)
                .laserDamage(25)
                .laserArmorIgnore(0.0)
                .canPenetrateBlocks(true)
                .blockDamageReduction(0.44)
                .magazineSize(30)
                .maxMagazines(2)
                .initialReserveAmmo(90)
                .maxReserveAmmo(90)
                .exSkillMaxCharge(9)
                .meleeWeaponMaterial(Material.IRON_SWORD)
                .meleeWeaponName("长刀")
                .meleeDamage(30)
                .meleeCritDamage(45)
                .meleeBackstabDamage(220)
                .meleeBackstabCritDamage(300)
                .meleeAttackSpeed(0.65)
                .meleeSpeedBoost(0.02)
                .weaponSpeedReduction(0.08)
                .description("均衡", "突击")
                .build()
        );

        // 凯伊
        HERO_KITS.add(new HeroKit.Builder()
                .id("kayi")
                .name("凯伊")
                .displayName(Component.text("凯伊", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.BOLD, true))
                .maxHealth(90)
                .maxArmor(75)
                .armorDamageReduction(0.8)
                .armorIgnore(0.0)
                .canPenetrate(false)
                .canDamageTeammates(false)
                .weaponMaterial(Material.DIAMOND_HOE)
                .weaponName("改装·光之剑")
                .chargeTime(0)
                .laserDamage(65)
                .laserArmorIgnore(0.0)
                .canPenetrateBlocks(true)
                .blockDamageReduction(0.85)
                .magazineSize(10)
                .maxMagazines(2)
                .initialReserveAmmo(25)
                .maxReserveAmmo(25)
                .exSkillMaxCharge(10)
                .meleeWeaponMaterial(Material.DIAMOND_SWORD)
                .meleeWeaponName("军用匕首")
                .meleeDamage(25)
                .meleeCritDamage(35)
                .meleeBackstabDamage(200)
                .meleeBackstabCritDamage(250)
                .meleeAttackSpeed(2.0)
                .meleeSpeedBoost(0.02)
                .weaponSpeedReduction(0.15)
                .description("辅助", "治疗")
                .build()
        );

        // 泉
        HERO_KITS.add(new HeroKit.Builder()
                .id("izumi")
                .name("泉")
                .displayName(Component.text("泉", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true))
                .maxHealth(100)
                .maxArmor(50)
                .armorDamageReduction(0.5)
                .armorIgnore(0.0)
                .canPenetrate(false)
                .canDamageTeammates(false)
                .weaponMaterial(Material.NETHERITE_SHOVEL)
                .weaponName("α·奇兵")
                .chargeTime(0)
                .laserDamage(19)
                .laserArmorIgnore(0.0)
                .canPenetrateBlocks(true)
                .blockDamageReduction(0.8)
                .magazineSize(26)
                .maxMagazines(4)
                .initialReserveAmmo(130)
                .maxReserveAmmo(130)
                .exSkillMaxCharge(7)
                .meleeWeaponMaterial(Material.NETHERITE_PICKAXE)
                .meleeWeaponName("合金稿")
                .meleeDamage(30)
                .meleeCritDamage(45)
                .meleeBackstabDamage(150)
                .meleeBackstabCritDamage(200)
                .meleeAttackSpeed(1.5)
                .meleeSpeedBoost(0.0)
                .weaponSpeedReduction(0.0)
                .description("突击", "回复")
                .build()
        );

        // 未花
        HERO_KITS.add(new HeroKit.Builder()
                .id("mika")
                .name("未花")
                .displayName(Component.text("未花", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.BOLD, true))
                .maxHealth(130)
                .maxArmor(30)
                .armorDamageReduction(0.5)
                .armorIgnore(0.0)
                .canPenetrate(true)
                .canDamageTeammates(false)
                .weaponMaterial(Material.IRON_HOE)
                .weaponName("Quis ut Deus")
                .chargeTime(0)
                .laserDamage(20)
                .laserArmorIgnore(0.0)
                .canPenetrateBlocks(true)
                .blockDamageReduction(0.7)
                .magazineSize(25)
                .maxMagazines(2)
                .initialReserveAmmo(75)
                .maxReserveAmmo(75)
                .exSkillMaxCharge(8)
                .meleeWeaponMaterial(Material.IRON_SWORD)
                .meleeWeaponName("圣三一之剑")
                .meleeDamage(25)
                .meleeCritDamage(35)
                .meleeBackstabDamage(150)
                .meleeBackstabCritDamage(200)
                .meleeAttackSpeed(1.5)
                .meleeSpeedBoost(0.02)
                .weaponSpeedReduction(0.05)
                .description("穿透", "全攻击爆头")
                .build()
        );

        // 优香
        HERO_KITS.add(new HeroKit.Builder()
                .id("yuuka")
                .name("优香")
                .displayName(Component.text("优香", NamedTextColor.BLUE).decoration(TextDecoration.BOLD, true))
                .maxHealth(150)
                .maxArmor(50)
                .armorDamageReduction(0.5)
                .armorIgnore(0.0)
                .canPenetrate(false)
                .canDamageTeammates(false)
                .weaponMaterial(Material.IRON_SHOVEL)
                .weaponName("逻辑与理性")
                .chargeTime(0)
                .laserDamage(16)
                .laserArmorIgnore(0.0)
                .canPenetrateBlocks(true)
                .blockDamageReduction(0.5)
                .magazineSize(30)
                .maxMagazines(3)
                .initialReserveAmmo(120)
                .maxReserveAmmo(120)
                .exSkillMaxCharge(7)
                .meleeWeaponMaterial(Material.IRON_SWORD)
                .meleeWeaponName("计算之刃")
                .meleeDamage(20)
                .meleeCritDamage(30)
                .meleeBackstabDamage(120)
                .meleeBackstabCritDamage(180)
                .meleeAttackSpeed(1.0)
                .meleeSpeedBoost(0.01)
                .weaponSpeedReduction(0.03)
                .description("护盾", "闪避")
                .build()
        );

        // 芹奈
        HERO_KITS.add(new HeroKit.Builder()
                .id("serina")
                .name("芹奈")
                .displayName(Component.text("芹奈", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true))
                .maxHealth(100)
                .maxArmor(50)
                .armorDamageReduction(0.5)
                .armorIgnore(0.0)
                .canPenetrate(false)
                .canDamageTeammates(false)
                .weaponMaterial(Material.IRON_HOE)
                .weaponName("神名")
                .chargeTime(0)
                .laserDamage(35)
                .laserArmorIgnore(0.0)
                .canPenetrateBlocks(false)
                .blockDamageReduction(0.0)
                .magazineSize(6)
                .maxMagazines(3)
                .initialReserveAmmo(15)
                .maxReserveAmmo(15)
                .exSkillMaxCharge(7)
                .meleeWeaponMaterial(Material.GOLDEN_SWORD)
                .meleeWeaponName("治疗之杖")
                .meleeDamage(0)
                .meleeCritDamage(0)
                .meleeBackstabDamage(0)
                .meleeBackstabCritDamage(0)
                .meleeAttackSpeed(1.0)
                .meleeSpeedBoost(0.0)
                .weaponSpeedReduction(0.0)
                .description("治疗", "复活")
                .build()
        );

        // 拉洛芙
        HERO_KITS.add(new HeroKit.Builder()
                .id("rakufu")
                .name("拉洛芙")
                .displayName(Component.text("拉洛芙", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.BOLD, true))
                .maxHealth(100)
                .maxArmor(50)
                .armorDamageReduction(0.5)
                .armorIgnore(0.0)
                .canPenetrate(false)
                .canDamageTeammates(false)
                .weaponMaterial(Material.IRON_HOE)
                .weaponName("破晓")
                .chargeTime(0)
                .laserDamage(22)
                .laserArmorIgnore(0.0)
                .canPenetrateBlocks(true)
                .blockDamageReduction(0.6)
                .magazineSize(50)
                .maxMagazines(3)
                .initialReserveAmmo(150)
                .maxReserveAmmo(150)
                .exSkillMaxCharge(5)
                .meleeWeaponMaterial(Material.IRON_SWORD)
                .meleeWeaponName("战术匕首")
                .meleeDamage(15)
                .meleeCritDamage(25)
                .meleeBackstabDamage(100)
                .meleeBackstabCritDamage(150)
                .meleeAttackSpeed(1.5)
                .meleeSpeedBoost(0.0)
                .weaponSpeedReduction(0.30)
                .description("探测", "不灭")
                .build()
        );

        // ===== 星野（防御形态）—— 无主武器 =====
        HERO_KITS.add(new HeroKit.Builder()
                .id("hoshino_defense")
                .name("星野·防御")
                .displayName(Component.text("星野·防御", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true))
                .maxHealth(120)
                .maxArmor(40)
                .armorDamageReduction(0.3)
                .armorIgnore(0.0)
                .canPenetrate(false)
                .canDamageTeammates(false)
                .weaponMaterial(Material.AIR)
                .weaponName("")
                .chargeTime(0)
                .laserDamage(0)
                .laserArmorIgnore(0.0)
                .canPenetrateBlocks(false)
                .blockDamageReduction(0.0)
                .magazineSize(0)
                .maxMagazines(0)
                .initialReserveAmmo(0)
                .maxReserveAmmo(0)
                .exSkillMaxCharge(7)
                .meleeWeaponMaterial(Material.IRON_SWORD)
                .meleeWeaponName("防卫匕首")
                .meleeDamage(20)
                .meleeCritDamage(30)
                .meleeBackstabDamage(100)
                .meleeBackstabCritDamage(150)
                .meleeAttackSpeed(1.2)
                .meleeSpeedBoost(0.0)
                .weaponSpeedReduction(0.10)
                .description("坦克", "护盾")
                .build()
        );

        // ===== 星野（攻击形态）—— 无主武器 =====
        HERO_KITS.add(new HeroKit.Builder()
                .id("hoshino_attack")
                .name("星野·攻击")
                .displayName(Component.text("星野·攻击", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
                .maxHealth(80)
                .maxArmor(50)
                .armorDamageReduction(0.5)
                .armorIgnore(0.0)
                .canPenetrate(false)
                .canDamageTeammates(false)
                .weaponMaterial(Material.AIR)
                .weaponName("")
                .chargeTime(0)
                .laserDamage(0)
                .laserArmorIgnore(0.0)
                .canPenetrateBlocks(false)
                .blockDamageReduction(0.0)
                .magazineSize(0)
                .maxMagazines(0)
                .initialReserveAmmo(0)
                .maxReserveAmmo(0)
                .exSkillMaxCharge(8)
                .meleeWeaponMaterial(Material.DIAMOND_SWORD)
                .meleeWeaponName("突击剑")
                .meleeDamage(25)
                .meleeCritDamage(35)
                .meleeBackstabDamage(120)
                .meleeBackstabCritDamage(180)
                .meleeAttackSpeed(1.5)
                .meleeSpeedBoost(0.03)
                .weaponSpeedReduction(0.0)
                .description("爆发", "标记")
                .build()
        );
    }

    private final BACS plugin;
    private GameState state = GameState.WAITING;
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Map<UUID, Boolean> playerAlive = new HashMap<>();
    private final Map<UUID, String> playerHeroes = new HashMap<>();
    private final Map<UUID, Integer> playerCurrentArmor = new HashMap<>();
    private final Map<UUID, Integer> playerExtraMaxArmor = new HashMap<>();
    private final Set<UUID> spectators = new HashSet<>();
    private final Map<UUID, Location> frozenPlayers = new HashMap<>();
    private final Map<UUID, Integer> freezeTaskIds = new HashMap<>();
    private int countdownTaskId = -1;
    private int countdownSeconds = 30;
    private BossBar countdownBossBar;
    private World voidWorld;
    private BombManager bombManager;
    private SoulFireListener soulFireListener;
    private CreditManager creditManager;
    private ShopManager shopManager;
    private AmmoManager ammoManager;
    private EXSkillManager exSkillManager;
    private YuukaSkillListener yuukaSkillListener;

    private int prepareTaskId = -1;
    private int prepareSeconds = 0;
    private int currentPrepareTime = 0;
    private BossBar prepareBossBar;
    private boolean isFirstRound = true;

    private int heroSelectTaskId = -1;
    private int heroSelectSeconds = HERO_SELECT_TIME;
    private BossBar heroSelectBossBar;
    private boolean allHeroesSelected = false;

    private int attackerScore = 0;
    private int defenderScore = 0;
    private int attackerAlive = 0;
    private int defenderAlive = 0;
    private int roundNumber = 0;
    private boolean isOvertime = false;

    // ★ 攻防转换
    private boolean halfTimeSwapped = false;

    // ★ 加时赛
    private int overtimeRound = 0;   // 加时赛第几局（1~7）

    private int roundEndTaskId = -1;
    private int roundEndSeconds = 6;

    private int healthDisplayTaskId = -1;

    private BossBar roundTimerBossBar;
    private int roundTimerTaskId = -1;
    private int roundTimerSeconds = 160;
    private boolean bombDeployedInRound = false;

    private final Map<UUID, Integer> roundKills = new HashMap<>();

    public GameManager(BACS plugin) {
        this.plugin = plugin;
        this.voidWorld = plugin.getVoidWorld();
        this.ammoManager = new AmmoManager();
        this.bombManager = new BombManager(plugin, this);
        this.soulFireListener = new SoulFireListener(plugin, this, ammoManager);
        this.creditManager = new CreditManager();
        this.shopManager = new ShopManager(plugin, this, creditManager);
        this.exSkillManager = new EXSkillManager();
        this.yuukaSkillListener = new YuukaSkillListener(plugin, this);
        setupScoreboardTeams();
        updateScoreboard();
        startHealthDisplay();
    }

    public BombManager getBombManager() { return bombManager; }
    public SoulFireListener getSoulFireListener() { return soulFireListener; }
    public CreditManager getCreditManager() { return creditManager; }
    public ShopManager getShopManager() { return shopManager; }
    public AmmoManager getAmmoManager() { return ammoManager; }
    public EXSkillManager getExSkillManager() { return exSkillManager; }
    public YuukaSkillListener getYuukaSkillListener() { return yuukaSkillListener; }

    public int getPlayerExtraMaxArmor(Player player) {
        return playerExtraMaxArmor.getOrDefault(player.getUniqueId(), 0);
    }

    public void addPlayerExtraMaxArmor(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        playerExtraMaxArmor.put(uuid, playerExtraMaxArmor.getOrDefault(uuid, 0) + amount);
    }

    public Map<UUID, Boolean> getPlayerAlive() { return playerAlive; }
    public Set<UUID> getSpectators() { return spectators; }
    public Map<UUID, String> getPlayerTeams() { return playerTeams; }

    private void setupScoreboardTeams() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team attacker = board.getTeam(TEAM_ATTACKER);
        if (attacker != null) attacker.unregister();
        Team defender = board.getTeam(TEAM_DEFENDER);
        if (defender != null) defender.unregister();

        attacker = board.registerNewTeam(TEAM_ATTACKER);
        attacker.color(NamedTextColor.RED);
        attacker.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        attacker.setDisplayName("§c攻方");

        defender = board.registerNewTeam(TEAM_DEFENDER);
        defender.color(NamedTextColor.BLUE);
        defender.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        defender.setDisplayName("§9守方");
    }

    // ============================================================
    // 攻防转换
    // ============================================================

    /**
     * ★ 交换所有玩家的队伍
     */
    private void swapTeams() {
        for (Map.Entry<UUID, String> entry : playerTeams.entrySet()) {
            String oldTeam = entry.getValue();
            String newTeam = oldTeam.equals(TEAM_ATTACKER) ? TEAM_DEFENDER : TEAM_ATTACKER;
            entry.setValue(newTeam);

            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;

            Team oldScoreboardTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(oldTeam);
            if (oldScoreboardTeam != null) oldScoreboardTeam.removeEntry(p.getName());
            Team newScoreboardTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(newTeam);
            if (newScoreboardTeam != null) newScoreboardTeam.addEntry(p.getName());
        }
        halfTimeSwapped = !halfTimeSwapped;
    }

    // ============================================================
    // 显示
    // ============================================================

    private void updateWeaponDisplay(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null) return;

        ShopManager shop = getShopManager();
        AmmoManager ammo = getAmmoManager();
        HeroKit kit = getPlayerHero(player);

        boolean isPistol = mainHand.getType() == Material.WOODEN_AXE &&
                mainHand.hasItemMeta() && mainHand.getItemMeta().hasDisplayName() &&
                mainHand.getItemMeta().displayName().toString().contains("制式手枪");
        if (isPistol) {
            int ammoCount = shop.getPistolAmmo(player);
            int reserve = shop.getPistolReserve(player);
            player.sendActionBar(Component.text("🔫 制式手枪 | 子弹: " + ammoCount + "/12 | 备用: " + reserve, NamedTextColor.YELLOW));
            return;
        }

        boolean isSpiritGun = mainHand.getType() == Material.IRON_AXE &&
                mainHand.hasItemMeta() && mainHand.getItemMeta().hasDisplayName() &&
                mainHand.getItemMeta().displayName().toString().contains("灵异");
        if (isSpiritGun) {
            int ammoCount = shop.getSpiritGunAmmo(player);
            int reserve = shop.getSpiritGunReserve(player);
            player.sendActionBar(Component.text("👻 灵异 | 子弹: " + ammoCount + "/15 | 备用: " + reserve, NamedTextColor.YELLOW));
            return;
        }

        boolean isVerdict = mainHand.getType() == Material.IRON_AXE &&
                mainHand.hasItemMeta() && mainHand.getItemMeta().hasDisplayName() &&
                mainHand.getItemMeta().displayName().toString().contains("裁决");
        if (isVerdict) {
            int ammoCount = shop.getVerdictAmmo(player);
            int reserve = shop.getVerdictReserve(player);
            player.sendActionBar(Component.text("⚖ 裁决 | 子弹: " + ammoCount + "/6 | 备用: " + reserve, NamedTextColor.YELLOW));
            return;
        }

        boolean isRage = mainHand.getType() == Material.GOLDEN_AXE &&
                mainHand.hasItemMeta() && mainHand.getItemMeta().hasDisplayName() &&
                mainHand.getItemMeta().displayName().toString().contains("怒焰");
        if (isRage) {
            int ammoCount = shop.getRageAmmo(player);
            int reserve = shop.getRageReserve(player);
            player.sendActionBar(Component.text("🔥 怒焰 | 子弹: " + ammoCount + "/" + WeaponConfig.RAGE_MAX_AMMO + " | 备用: " + reserve, NamedTextColor.YELLOW));
            return;
        }

        boolean isHandCannon = mainHand.getType() == Material.NETHERITE_AXE &&
                mainHand.hasItemMeta() && mainHand.getItemMeta().hasDisplayName() &&
                mainHand.getItemMeta().displayName().toString().contains("手炮");
        if (isHandCannon) {
            int ammoCount = shop.getHandCannonAmmo(player);
            int reserve = shop.getHandCannonReserve(player);
            player.sendActionBar(Component.text("💥 手炮 | 子弹: " + ammoCount + "/" + WeaponConfig.HANDCANNON_MAX_AMMO + " | 备用: " + reserve, NamedTextColor.YELLOW));
            return;
        }

        if (kit != null && kit.getWeaponMaterial() != Material.AIR &&
                mainHand.getType() == kit.getWeaponMaterial() &&
                mainHand.hasItemMeta() && mainHand.getItemMeta().hasDisplayName() &&
                mainHand.getItemMeta().displayName().toString().contains(kit.getWeaponName())) {
            int bullets = ammo.getMagazineAmmo(player.getUniqueId());
            int reserve = ammo.getReserveAmmo(player.getUniqueId());
            int exCharge = getExSkillManager().getCharge(player);
            int exMaxCharge = kit.getExSkillMaxCharge();
            player.sendActionBar(Component.text("🔫 " + kit.getWeaponName() + " | 子弹: " + bullets + "/" + kit.getMagazineSize() + " | 备用: " + reserve + " | ⚡ " + exCharge + "/" + exMaxCharge, NamedTextColor.YELLOW));
            return;
        }

        updateHealthDisplay(player);
    }

    private void startHealthDisplay() {
        if (healthDisplayTaskId != -1) {
            Bukkit.getScheduler().cancelTask(healthDisplayTaskId);
            healthDisplayTaskId = -1;
        }

        healthDisplayTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateWeaponDisplay(player);
                    shopManager.tickArmorRegen(player);
                    yuukaSkillListener.tickArmorRegen(player);
                }
            }
        }, 0L, 2L);
    }

    private void updateHealthDisplay(Player player) {
        HeroKit kit = getPlayerHero(player);
        if (kit == null) return;

        double currentHealth = player.getHealth();
        double maxHealth = player.getMaxHealth();
        int currentArmor = getPlayerCurrentArmor(player);
        int maxArmor = kit.getMaxArmor() + getPlayerExtraMaxArmor(player);
        int exCharge = exSkillManager.getCharge(player);
        int exMaxCharge = kit.getExSkillMaxCharge();
        int tempShield = exSkillManager.getTempShield(player);
        int specialShield = exSkillManager.getSpecialShield(player);

        int currentCredits = creditManager.getCredits(player.getUniqueId());
        int nextRoundMinCredits = creditManager.getNextRoundMinCredits(player.getUniqueId());

        Component display = Component.empty()
                .append(Component.text("❤ ", NamedTextColor.RED))
                .append(Component.text((int)currentHealth + "/" + (int)maxHealth, NamedTextColor.RED))
                .append(Component.text("  ", NamedTextColor.WHITE))
                .append(Component.text("🛡 ", NamedTextColor.AQUA))
                .append(Component.text(currentArmor + "/" + maxArmor, NamedTextColor.AQUA))
                .append(Component.text("  ", NamedTextColor.WHITE));

        if (kit.getId().equals("hoshino_defense")) {
            ShieldManager sm = BACS.getInstance().getShieldManager();
            if (sm != null && sm.hasShield(player)) {
                display = display.append(Component.text("🛡", NamedTextColor.YELLOW))
                        .append(Component.text(sm.getDurability(player) + "/" + sm.getMaxDurability(player), NamedTextColor.YELLOW))
                        .append(Component.text("  ", NamedTextColor.WHITE));
            }
        }

        if (kit.getId().equals("kuroko") && shopManager.hasRegenArmor(player)) {
            int regenPool = shopManager.getRegenPool(player);
            display = display.append(Component.text("💧", NamedTextColor.GREEN))
                    .append(Component.text(regenPool, NamedTextColor.GREEN))
                    .append(Component.text("  ", NamedTextColor.WHITE));
        }

        if (tempShield > 0) {
            display = display.append(Component.text("✨", NamedTextColor.GOLD))
                    .append(Component.text(String.valueOf(tempShield), NamedTextColor.GOLD))
                    .append(Component.text("  ", NamedTextColor.WHITE));
        }

        if (specialShield > 0) {
            display = display.append(Component.text("🛡", NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text(String.valueOf(specialShield), NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text("  ", NamedTextColor.WHITE));
        }

        int bullets = ammoManager.getMagazineAmmo(player.getUniqueId());
        int reserve = ammoManager.getReserveAmmo(player.getUniqueId());
        display = display.append(Component.text("🔫", NamedTextColor.YELLOW))
                .append(Component.text(bullets + "/" + kit.getMagazineSize(), NamedTextColor.YELLOW))
                .append(Component.text(" 📦", NamedTextColor.YELLOW))
                .append(Component.text(String.valueOf(reserve), NamedTextColor.YELLOW))
                .append(Component.text("  ", NamedTextColor.WHITE));

        display = display.append(Component.text("⚡", NamedTextColor.GOLD))
                .append(Component.text(exCharge + "/" + exMaxCharge, NamedTextColor.GOLD))
                .append(Component.text("  ", NamedTextColor.WHITE))
                .append(Component.text("⭐ ", NamedTextColor.GOLD))
                .append(Component.text(currentCredits + " (+" + (nextRoundMinCredits - currentCredits) + ")", NamedTextColor.GOLD));

        player.sendActionBar(display);
    }

    public void stopHealthDisplay() {
        if (healthDisplayTaskId != -1) {
            Bukkit.getScheduler().cancelTask(healthDisplayTaskId);
            healthDisplayTaskId = -1;
        }
    }

    public int getPlayerCurrentArmor(Player player) {
        return playerCurrentArmor.getOrDefault(player.getUniqueId(), 0);
    }

    public void setPlayerCurrentArmor(Player player, int armor) {
        if (armor < 0) armor = 0;
        playerCurrentArmor.put(player.getUniqueId(), armor);
    }

    public void resetAllArmor() {
        for (UUID uuid : playerTeams.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                HeroKit kit = getPlayerHero(p);
                if (kit != null) {
                    int maxArmor = kit.getMaxArmor() + getPlayerExtraMaxArmor(p);
                    playerCurrentArmor.put(uuid, maxArmor);
                }
            }
        }
    }

    public Location getAttackerSpawnLocation() {
        if (voidWorld == null) {
            voidWorld = plugin.getVoidWorld();
            if (voidWorld == null) return null;
        }
        Random random = new Random();
        double x = ATTACKER_X1 + (ATTACKER_X2 - ATTACKER_X1) * random.nextDouble();
        double z = ATTACKER_Z1 + (ATTACKER_Z2 - ATTACKER_Z1) * random.nextDouble();
        return new Location(voidWorld, x, ATTACKER_Y, z);
    }

    public Location getDefenderSpawnLocation() {
        if (voidWorld == null) {
            voidWorld = plugin.getVoidWorld();
            if (voidWorld == null) return null;
        }
        Random random = new Random();
        double x = DEFENDER_X1 + (DEFENDER_X2 - DEFENDER_X1) * random.nextDouble();
        double z = DEFENDER_Z1 + (DEFENDER_Z2 - DEFENDER_Z1) * random.nextDouble();
        return new Location(voidWorld, x, DEFENDER_Y, z);
    }

    private void teleportSafely(Player player, Location location) {
        if (location == null) {
            if (voidWorld != null) {
                player.teleport(new Location(voidWorld, 0, 102, 0));
            }
            return;
        }
        player.teleport(location);
    }

    private void freezePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        frozenPlayers.put(uuid, player.getLocation().clone());
        player.setWalkSpeed(0f);
        player.setFlySpeed(0f);

        if (freezeTaskIds.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(freezeTaskIds.get(uuid));
        }

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (frozenPlayers.containsKey(uuid) && player.isOnline()) {
                Location frozenLoc = frozenPlayers.get(uuid);
                if (player.getLocation().distance(frozenLoc) > 0.1) {
                    player.teleport(frozenLoc);
                }
            }
        }, 0L, 1L);

        freezeTaskIds.put(uuid, taskId);
    }

    private void unfreezePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        frozenPlayers.remove(uuid);
        if (freezeTaskIds.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(freezeTaskIds.get(uuid));
            freezeTaskIds.remove(uuid);
        }
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
    }

    private void unfreezeAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            unfreezePlayer(player);
        }
    }

    public boolean isPlayerInShopArea(Player player) {
        if (state != GameState.PREPARE) return false;
        Location loc = player.getLocation();
        String team = playerTeams.get(player.getUniqueId());
        if (team == null) return false;
        if (team.equals(TEAM_ATTACKER)) {
            return loc.getX() >= SHOP_ATTACKER_X1 && loc.getX() <= SHOP_ATTACKER_X2 &&
                    loc.getY() >= SHOP_ATTACKER_Y1 && loc.getY() <= SHOP_ATTACKER_Y2 &&
                    loc.getZ() >= SHOP_ATTACKER_Z1 && loc.getZ() <= SHOP_ATTACKER_Z2;
        } else {
            return loc.getX() >= SHOP_DEFENDER_X1 && loc.getX() <= SHOP_DEFENDER_X2 &&
                    loc.getY() >= SHOP_DEFENDER_Y1 && loc.getY() <= SHOP_DEFENDER_Y2 &&
                    loc.getZ() >= SHOP_DEFENDER_Z1 && loc.getZ() <= SHOP_DEFENDER_Z2;
        }
    }

    public void giveShopItem(Player player) {
        removeShopItem(player);
        ItemStack shopItem = new ItemStack(Material.GHAST_TEAR);
        ItemMeta meta = shopItem.getItemMeta();
        meta.displayName(SHOP_ITEM_NAME);
        meta.lore(List.of(
                Component.text("右键打开商店", NamedTextColor.GRAY),
                Component.text("只能在准备阶段使用", NamedTextColor.DARK_GRAY)
        ));
        meta.setUnbreakable(true);
        shopItem.setItemMeta(meta);
        player.getInventory().setItem(8, shopItem);
    }

    public void removeShopItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.GHAST_TEAR) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.displayName().equals(SHOP_ITEM_NAME)) {
                    player.getInventory().remove(item);
                }
            }
        }
        player.getInventory().setItem(8, null);
    }

    public void giveTeamArmor(Player player) {
        String team = playerTeams.get(player.getUniqueId());
        if (team == null) return;

        boolean isAttacker = team.equals(TEAM_ATTACKER);
        Color armorColor = isAttacker ? Color.RED : Color.BLUE;
        String teamName = isAttacker ? "攻方" : "守方";

        ItemStack helmet = createLeatherArmor(Material.LEATHER_HELMET, armorColor, teamName);
        ItemStack boots = createLeatherArmor(Material.LEATHER_BOOTS, armorColor, teamName);

        player.getInventory().setHelmet(helmet);
        player.getInventory().setBoots(boots);
    }

    private ItemStack createLeatherArmor(Material material, Color color, String teamName) {
        ItemStack armor = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
        meta.setColor(color);
        NamedTextColor textColor = color == Color.RED ? NamedTextColor.RED : NamedTextColor.BLUE;
        meta.displayName(Component.text(teamName + " 盔甲", textColor).decoration(TextDecoration.BOLD, true));
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        meta.setUnbreakable(true);
        armor.setItemMeta(meta);
        return armor;
    }

    public void removeTeamArmor(Player player) {
        player.getInventory().setHelmet(null);
        player.getInventory().setBoots(null);
    }

    public GameState getState() { return state; }
    public void setState(GameState state) {
        this.state = state;
        updateScoreboard();
    }

    public void updateScoreboard() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerScoreboard(player);
        }
    }

    private void updatePlayerScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        Objective obj = board.registerNewObjective("game", "dummy",
                Component.text("§6§l⚔ 战场信息").decoration(TextDecoration.BOLD, true));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        String teamName = getPlayerTeam(player);
        String teamDisplay = teamName == null ? "§7未选择" : (teamName.equals(TEAM_ATTACKER) ? "§c攻方" : "§9守方");

        HeroKit kit = getPlayerHero(player);
        String heroDisplay = kit == null ? "§7未选择" : "§a" + kit.getName();

        int line = 0;
        obj.getScore("§7 ").setScore(line++);
        obj.getScore("§6▶ 阶段: §f" + getStateDisplayText()).setScore(line++);
        obj.getScore("§7  ").setScore(line++);
        obj.getScore("§c攻方 §f" + attackerScore + " §7- §9" + defenderScore + " §f守方").setScore(line++);
        if (isOvertime) {
            obj.getScore("§6§l⚡ 加时赛 第" + overtimeRound + "局").setScore(line++);
        }
        obj.getScore("§7   ").setScore(line++);
        obj.getScore("§7队伍: " + teamDisplay).setScore(line++);
        obj.getScore("§7英雄: " + heroDisplay).setScore(line++);
        obj.getScore("§7    ").setScore(line++);
        obj.getScore("§c§l攻方存活:").setScore(line++);
        obj.getScore(getTeamStatusDisplay(TEAM_ATTACKER)).setScore(line++);
        obj.getScore("§7     ").setScore(line++);
        obj.getScore("§9§l守方存活:").setScore(line++);
        obj.getScore(getTeamStatusDisplay(TEAM_DEFENDER)).setScore(line++);
        obj.getScore("§7      ").setScore(line++);
        if (state != GameState.WAITING && state != GameState.COUNTDOWN && state != GameState.HERO_SELECT) {
            obj.getScore("§7第 §f" + roundNumber + " §7回合").setScore(line++);
        }

        player.setScoreboard(board);
    }

    private String getTeamStatusDisplay(String team) {
        int total = 0;
        int alive = 0;
        for (Map.Entry<UUID, String> entry : playerTeams.entrySet()) {
            if (entry.getValue().equals(team)) {
                total++;
                if (playerAlive.getOrDefault(entry.getKey(), false)) {
                    alive++;
                }
            }
        }

        StringBuilder display = new StringBuilder();
        for (int i = 0; i < total; i++) {
            display.append(i < alive ? "§a●" : "§c●");
        }
        return display.toString();
    }

    private String getStateDisplayText() {
        switch (state) {
            case WAITING: return "§7等待中";
            case COUNTDOWN: return "§e倒计时 §f" + countdownSeconds + "§es";
            case HERO_SELECT: return "§d英雄选择 §f" + heroSelectSeconds + "§ds";
            case PREPARE: return "§a准备阶段 §f" + prepareSeconds + "§as";
            case ROUND_ACTIVE: return "§c回合进行中";
            case ROUND_END: return "§6回合结束 §f" + roundEndSeconds + "§6s";
            case VOTE: return "§6投票中";
            case GAME_OVER: return "§4游戏结束";
            default: return "§7未知";
        }
    }

    public HeroKit getPlayerHero(Player player) {
        String heroId = playerHeroes.get(player.getUniqueId());
        if (heroId == null) return null;
        for (HeroKit kit : HERO_KITS) {
            if (kit.getId().equals(heroId)) return kit;
        }
        return null;
    }

    public HeroKit getHeroKitById(String id) {
        for (HeroKit kit : HERO_KITS) {
            if (kit.getId().equals(id)) return kit;
        }
        return null;
    }

    public int getPlayerArmor(Player player) {
        HeroKit kit = getPlayerHero(player);
        if (kit == null) return 0;
        return kit.getMaxArmor() + getPlayerExtraMaxArmor(player);
    }

    private ItemStack createWeapon(HeroKit kit) {
        if (kit.getWeaponMaterial() == Material.AIR) return null;

        ItemStack weapon = new ItemStack(kit.getWeaponMaterial());
        ItemMeta meta = weapon.getItemMeta();
        NamedTextColor color;
        switch (kit.getId()) {
            case "al1s": color = NamedTextColor.AQUA; break;
            case "kuroko": color = NamedTextColor.GOLD; break;
            case "kayi": color = NamedTextColor.LIGHT_PURPLE; break;
            case "izumi": color = NamedTextColor.GREEN; break;
            case "mika": color = NamedTextColor.LIGHT_PURPLE; break;
            case "yuuka": color = NamedTextColor.BLUE; break;
            case "serina": color = NamedTextColor.WHITE; break;
            case "rakufu": color = NamedTextColor.LIGHT_PURPLE; break;
            default: color = NamedTextColor.WHITE;
        }

        if (kit.getId().equals("serina")) {
            meta.displayName(Component.text("神名", NamedTextColor.WHITE));
            meta.lore(List.of(
                    Component.text("右键发射15发霰弹", NamedTextColor.GRAY),
                    Component.text("伤害: 头19/胸13/四肢9", NamedTextColor.RED),
                    Component.text("最大射程: 25格", NamedTextColor.YELLOW),
                    Component.text("穿透1个方块", NamedTextColor.GOLD),
                    Component.text("弹夹: 6发 | 备用: 15发", NamedTextColor.YELLOW),
                    Component.text("蹲下右键触发EX技能", NamedTextColor.GOLD),
                    Component.text("无法取下", NamedTextColor.DARK_GRAY)
            ));
        } else if (kit.getId().equals("rakufu")) {
            meta.displayName(Component.text("破晓", NamedTextColor.LIGHT_PURPLE));
            meta.lore(List.of(
                    Component.text("伤害: 头52/胸22/四肢18", NamedTextColor.RED),
                    Component.text("射速: 8.9发/秒", NamedTextColor.YELLOW),
                    Component.text("弹夹: 50发 | 备用: 150发", NamedTextColor.YELLOW),
                    Component.text("穿透3个方块", NamedTextColor.GOLD),
                    Component.text("蹲下时伤害+30%，穿透4方块", NamedTextColor.GRAY),
                    Component.text("无法取下", NamedTextColor.DARK_GRAY)
            ));
        } else if (kit.getId().equals("al1s")) {
            meta.displayName(Component.text("超新星·光之剑", NamedTextColor.AQUA));
            meta.lore(List.of(
                    Component.text("伤害: 头400/胸275/四肢250", NamedTextColor.RED),
                    Component.text("弹夹: 5发 | 备用: 10发", NamedTextColor.YELLOW),
                    Component.text("蹲下蓄力：最多+30%伤害", NamedTextColor.GOLD),
                    Component.text("无视30%护甲减伤", NamedTextColor.LIGHT_PURPLE),
                    Component.text("无法取下", NamedTextColor.DARK_GRAY)
            ));
        } else {
            meta.displayName(Component.text(kit.getWeaponName(), color));
            meta.lore(List.of(
                    Component.text("左键发射", NamedTextColor.GRAY),
                    Component.text("伤害: " + kit.getLaserDamage(), NamedTextColor.RED),
                    Component.text("弹夹: " + kit.getMagazineSize() + "发 | 备用: " + kit.getInitialReserveAmmo() + "发", NamedTextColor.YELLOW),
                    Component.text("蹲下右键触发EX技能", NamedTextColor.GOLD),
                    Component.text("无法取下", NamedTextColor.DARK_GRAY)
            ));
        }

        meta.setUnbreakable(true);
        weapon.setItemMeta(meta);
        return weapon;
    }

    public void applyHeroStats(Player player, HeroKit kit) {
        player.setMaxHealth(kit.getMaxHealth());
        player.setHealth(kit.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.getInventory().clear();

        int maxArmor = kit.getMaxArmor() + getPlayerExtraMaxArmor(player);
        playerCurrentArmor.put(player.getUniqueId(), maxArmor);

        // 副武器：制式手枪
        ItemStack pistol = new ItemStack(Material.WOODEN_AXE);
        ItemMeta pistolMeta = pistol.getItemMeta();
        pistolMeta.displayName(Component.text("制式手枪", NamedTextColor.GRAY));
        pistolMeta.setUnbreakable(true);
        pistol.setItemMeta(pistolMeta);
        player.getInventory().setItem(1, pistol);

        // 近战武器
        ItemStack meleeWeapon = new ItemStack(kit.getMeleeWeaponMaterial());
        ItemMeta meleeMeta = meleeWeapon.getItemMeta();

        if (kit.getId().equals("serina")) {
            meleeMeta.displayName(Component.text("治疗之杖", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
            meleeMeta.lore(List.of(
                    Component.text("蹲下丢弃触发治疗", NamedTextColor.GRAY),
                    Component.text("攻击队友治疗20生命", NamedTextColor.GRAY),
                    Component.text("并给予5秒生命恢复3", NamedTextColor.GRAY),
                    Component.text("无法取下", NamedTextColor.DARK_GRAY)
            ));
            meleeMeta.setUnbreakable(true);
            meleeWeapon.setItemMeta(meleeMeta);
            player.getInventory().setItem(2, meleeWeapon);
        } else if (kit.getId().equals("mika")) {
            meleeMeta.displayName(Component.text(kit.getMeleeWeaponName(), NamedTextColor.LIGHT_PURPLE));
            meleeMeta.lore(List.of(
                    Component.text("伤害: " + kit.getMeleeDamage(), NamedTextColor.RED),
                    Component.text("背刺: " + kit.getMeleeBackstabDamage(), NamedTextColor.DARK_RED),
                    Component.text("蹲下左键触发小技能", NamedTextColor.GOLD),
                    Component.text("无法取下", NamedTextColor.DARK_GRAY)
            ));
            meleeMeta.setUnbreakable(true);
            meleeWeapon.setItemMeta(meleeMeta);
            player.getInventory().setItem(2, meleeWeapon);
        } else if (kit.getId().equals("kayi")) {
            meleeMeta.displayName(Component.text(kit.getMeleeWeaponName(), NamedTextColor.LIGHT_PURPLE));
            meleeMeta.lore(List.of(
                    Component.text("伤害: " + kit.getMeleeDamage(), NamedTextColor.RED),
                    Component.text("背刺: " + kit.getMeleeBackstabDamage(), NamedTextColor.DARK_RED),
                    Component.text("右键释放治疗之光", NamedTextColor.GOLD),
                    Component.text("无法取下", NamedTextColor.DARK_GRAY)
            ));
            meleeMeta.setUnbreakable(true);
            meleeWeapon.setItemMeta(meleeMeta);
            player.getInventory().setItem(2, meleeWeapon);
        } else if (kit.getId().equals("izumi")) {
            meleeMeta.displayName(Component.text(kit.getMeleeWeaponName(), NamedTextColor.GREEN));
            meleeMeta.lore(List.of(
                    Component.text("伤害: " + kit.getMeleeDamage(), NamedTextColor.RED),
                    Component.text("背刺: " + kit.getMeleeBackstabDamage(), NamedTextColor.DARK_RED),
                    Component.text("丢弃近战触发技能1", NamedTextColor.GOLD),
                    Component.text("无法取下", NamedTextColor.DARK_GRAY)
            ));
            meleeMeta.setUnbreakable(true);
            meleeWeapon.setItemMeta(meleeMeta);
            player.getInventory().setItem(2, meleeWeapon);
        } else if (kit.getId().equals("yuuka")) {
            meleeMeta.displayName(Component.text(kit.getMeleeWeaponName(), NamedTextColor.BLUE));
            meleeMeta.lore(List.of(
                    Component.text("伤害: " + kit.getMeleeDamage(), NamedTextColor.RED),
                    Component.text("背刺: " + kit.getMeleeBackstabDamage(), NamedTextColor.DARK_RED),
                    Component.text("无法取下", NamedTextColor.DARK_GRAY)
            ));
            meleeMeta.setUnbreakable(true);
            meleeWeapon.setItemMeta(meleeMeta);
            player.getInventory().setItem(2, meleeWeapon);
        } else {
            meleeMeta.displayName(Component.text(kit.getMeleeWeaponName(), NamedTextColor.GOLD));
            meleeMeta.lore(List.of(
                    Component.text("伤害: " + kit.getMeleeDamage(), NamedTextColor.RED),
                    Component.text("背刺: " + kit.getMeleeBackstabDamage(), NamedTextColor.DARK_RED),
                    Component.text("无法取下", NamedTextColor.DARK_GRAY)
            ));
            meleeMeta.setUnbreakable(true);
            meleeWeapon.setItemMeta(meleeMeta);
            player.getInventory().setItem(2, meleeWeapon);
        }

        // 第六格：EX技能物品
        ItemStack exItem = new ItemStack(Material.NETHERITE_BLOCK);
        ItemMeta exMeta = exItem.getItemMeta();
        exMeta.displayName(Component.text("⚡ EX技能", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        exMeta.lore(List.of(
                Component.text("右键触发EX技能", NamedTextColor.GRAY),
                Component.text("左键触发小技能1", NamedTextColor.GRAY),
                Component.text("无法取下", NamedTextColor.DARK_GRAY)
        ));
        exMeta.setUnbreakable(true);
        exItem.setItemMeta(exMeta);
        player.getInventory().setItem(5, exItem);

        // 弹药初始化
        ammoManager.resetRound(player.getUniqueId(), kit.getMagazineSize(), kit.getInitialReserveAmmo());

        // 芹奈特殊
        if (kit.getId().equals("serina")) {
            getExSkillManager().clearCharge(player);
            getExSkillManager().addCharge(player, 1);
        }

        // 星野防御形态
        if (kit.getId().equals("hoshino_defense")) {
            if (BACS.getInstance().getHoshinoSkillListener() != null) {
                BACS.getInstance().getHoshinoSkillListener().onRoundStartDefense(player);
            }
        } else if (kit.getId().equals("hoshino_attack")) {
            if (BACS.getInstance().getHoshinoSkillListener() != null) {
                BACS.getInstance().getHoshinoSkillListener().onRoundStartAttack(player);
            }
        }

        // 拉洛芙
        if (kit.getId().equals("rakufu")) {
            if (BACS.getInstance().getRakuFuSkillListener() != null) {
                BACS.getInstance().getRakuFuSkillListener().onRoundStart(player);
            }
        }

        // 芹奈被动
        if (BACS.getInstance().getSerinaSkillListener() != null) {
            BACS.getInstance().getSerinaSkillListener().onTeamUpdate(player);
        }
    }

    // ============================================================
    // 英雄选择
    // ============================================================

    public void startHeroSelectPhase() {
        if (heroSelectTaskId != -1) {
            Bukkit.getScheduler().cancelTask(heroSelectTaskId);
            heroSelectTaskId = -1;
        }

        state = GameState.HERO_SELECT;
        heroSelectSeconds = HERO_SELECT_TIME;
        allHeroesSelected = false;
        playerHeroes.clear();

        heroSelectBossBar = BossBar.bossBar(
                Component.text("🎯 英雄选择: " + heroSelectSeconds + "s", NamedTextColor.GOLD),
                1.0f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS
        );
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(heroSelectBossBar);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            openHeroSelectMenu(player);
        }

        updateScoreboard();

        heroSelectTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (state != GameState.HERO_SELECT) {
                    Bukkit.getScheduler().cancelTask(heroSelectTaskId);
                    heroSelectTaskId = -1;
                    return;
                }

                if (heroSelectSeconds <= 0 || allHeroesSelected) {
                    Bukkit.getScheduler().cancelTask(heroSelectTaskId);
                    heroSelectTaskId = -1;
                    if (heroSelectBossBar != null) {
                        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(heroSelectBossBar);
                        heroSelectBossBar = null;
                    }
                    assignRandomHeroes();
                    startPreparePhase();
                    return;
                }

                if (heroSelectBossBar != null) {
                    float progress = (float) heroSelectSeconds / HERO_SELECT_TIME;
                    heroSelectBossBar.progress(Math.max(0, Math.min(1, progress)));
                    heroSelectBossBar.name(Component.text("🎯 英雄选择: " + heroSelectSeconds + "s", NamedTextColor.GOLD));
                }

                updateScoreboard();
                heroSelectSeconds--;
            }
        }, 0L, 20L);
    }

    private void assignRandomHeroes() {
        Random random = new Random();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!playerHeroes.containsKey(player.getUniqueId())) {
                List<HeroKit> availableKits = new ArrayList<>();
                for (HeroKit kit : HERO_KITS) {
                    if (kit.getId().equals("yuuka")) {
                        if (BACS.getInstance().getCoinManager() != null &&
                                BACS.getInstance().getCoinManager().hasYuukaKit(player)) {
                            availableKits.add(kit);
                        }
                    } else {
                        availableKits.add(kit);
                    }
                }
                HeroKit kit = availableKits.get(random.nextInt(availableKits.size()));
                playerHeroes.put(player.getUniqueId(), kit.getId());
                applyHeroStats(player, kit);
            }
        }
    }

    public void openHeroSelectMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("🎯 选择英雄", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));

        List<String> heroOrder = Arrays.asList(
                "al1s", "kuroko", "kayi", "izumi", "mika", "yuuka", "serina", "rakufu",
                "hoshino_defense", "hoshino_attack"
        );

        int slot = 0;
        for (String heroId : heroOrder) {
            HeroKit kit = getHeroKitById(heroId);
            if (kit == null) continue;

            if (kit.getId().equals("yuuka")) {
                CoinManager coinManager = BACS.getInstance().getCoinManager();
                if (coinManager != null && !coinManager.hasYuukaKit(player)) {
                    ItemStack barrier = new ItemStack(Material.BARRIER);
                    ItemMeta barrierMeta = barrier.getItemMeta();
                    barrierMeta.displayName(Component.text("优香", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
                    barrierMeta.lore(List.of(
                            Component.text("❌ 未购买！", NamedTextColor.RED),
                            Component.text("需要花费8000硬币购买", NamedTextColor.GOLD),
                            Component.text("使用 /buykits 购买", NamedTextColor.YELLOW)
                    ));
                    barrier.setItemMeta(barrierMeta);
                    inv.setItem(slot, barrier);
                    slot++;
                    continue;
                }
            }

            inv.setItem(slot, kit.getMenuItem());
            slot++;
        }

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.text(" ", NamedTextColor.DARK_GRAY));
        glass.setItemMeta(glassMeta);
        for (int i = slot; i < 54; i++) {
            inv.setItem(i, glass);
        }

        player.openInventory(inv);
    }

    public void selectHero(Player player, String heroId) {
        if (state != GameState.HERO_SELECT) return;

        HeroKit selectedKit = null;
        for (HeroKit kit : HERO_KITS) {
            if (kit.getId().equals(heroId)) {
                selectedKit = kit;
                break;
            }
        }

        if (selectedKit == null) return;

        if (selectedKit.getId().equals("yuuka")) {
            if (BACS.getInstance().getCoinManager() != null &&
                    !BACS.getInstance().getCoinManager().hasYuukaKit(player)) {
                player.sendMessage(Component.text("❌ 你需要花费8000硬币购买优香套件！", NamedTextColor.RED));
                return;
            }
        }

        if (selectedKit.getId().equals("kayi")) {
            String playerTeam = playerTeams.get(player.getUniqueId());
            if (playerTeam != null) {
                for (UUID uuid : playerHeroes.keySet()) {
                    if (playerHeroes.get(uuid).equals("kayi") &&
                            playerTeams.get(uuid) != null &&
                            playerTeams.get(uuid).equals(playerTeam) &&
                            !uuid.equals(player.getUniqueId())) {
                        player.sendMessage(Component.text("❌ 你的阵营已有一名凯伊！", NamedTextColor.RED));
                        return;
                    }
                }
            }
        }

        playerHeroes.put(player.getUniqueId(), heroId);
        player.closeInventory();
        applyHeroStats(player, selectedKit);

        int total = Bukkit.getOnlinePlayers().size();
        if (playerHeroes.size() == total && total >= 1) {
            allHeroesSelected = true;
        }
    }

    public void forceStartGame() {
        if (state == GameState.ROUND_ACTIVE || state == GameState.ROUND_END) return;

        cancelCountdown();
        if (prepareTaskId != -1) { Bukkit.getScheduler().cancelTask(prepareTaskId); prepareTaskId = -1; }
        if (heroSelectTaskId != -1) { Bukkit.getScheduler().cancelTask(heroSelectTaskId); heroSelectTaskId = -1; }
        if (prepareBossBar != null) { for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(prepareBossBar); prepareBossBar = null; }
        if (heroSelectBossBar != null) { for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(heroSelectBossBar); heroSelectBossBar = null; }

        assignRandomTeams();

        for (Player player : Bukkit.getOnlinePlayers()) {
            playerAlive.put(player.getUniqueId(), true);
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
        }
        spectators.clear();
        playerHeroes.clear();

        attackerScore = 0;
        defenderScore = 0;
        roundNumber = 0;
        isOvertime = false;
        halfTimeSwapped = false;
        overtimeRound = 0;
        isFirstRound = true;
        currentPrepareTime = 0;

        startHeroSelectPhase();
    }

    public void assignRandomTeams() {
        List<Player> unassigned = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!playerTeams.containsKey(player.getUniqueId())) {
                unassigned.add(player);
            }
        }

        Collections.shuffle(unassigned, new Random());

        int attackerCount = 0;
        int defenderCount = 0;
        for (String team : playerTeams.values()) {
            if (team.equals(TEAM_ATTACKER)) attackerCount++;
            else if (team.equals(TEAM_DEFENDER)) defenderCount++;
        }

        for (Player player : unassigned) {
            String team;
            if (attackerCount <= defenderCount) {
                team = TEAM_ATTACKER;
                attackerCount++;
            } else {
                team = TEAM_DEFENDER;
                defenderCount++;
            }

            playerTeams.put(player.getUniqueId(), team);
            Team scoreboardTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(team);
            if (scoreboardTeam != null) scoreboardTeam.addEntry(player.getName());
        }
    }

    // ============================================================
    // 准备阶段
    // ============================================================

    public void startPreparePhase() {
        if (prepareTaskId != -1) { Bukkit.getScheduler().cancelTask(prepareTaskId); prepareTaskId = -1; }

        shopManager.resetRoundPurchases();

        List<Player> newPlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!playerTeams.containsKey(player.getUniqueId())) {
                newPlayers.add(player);
            }
        }

        int attackerCount = 0;
        int defenderCount = 0;
        for (String team : playerTeams.values()) {
            if (team.equals(TEAM_ATTACKER)) attackerCount++;
            else if (team.equals(TEAM_DEFENDER)) defenderCount++;
        }

        for (Player player : newPlayers) {
            String team;
            if (attackerCount <= defenderCount) {
                team = TEAM_ATTACKER;
                attackerCount++;
            } else {
                team = TEAM_DEFENDER;
                defenderCount++;
            }
            playerTeams.put(player.getUniqueId(), team);
            playerAlive.put(player.getUniqueId(), true);

            Team scoreboardTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(team);
            if (scoreboardTeam != null) scoreboardTeam.addEntry(player.getName());

            if (!playerHeroes.containsKey(player.getUniqueId())) {
                Random random = new Random();
                List<HeroKit> availableKits = new ArrayList<>();
                for (HeroKit kit : HERO_KITS) {
                    if (kit.getId().equals("yuuka")) {
                        if (BACS.getInstance().getCoinManager() != null &&
                                BACS.getInstance().getCoinManager().hasYuukaKit(player)) {
                            availableKits.add(kit);
                        }
                    } else {
                        availableKits.add(kit);
                    }
                }
                HeroKit kit = availableKits.get(random.nextInt(availableKits.size()));
                playerHeroes.put(player.getUniqueId(), kit.getId());
                applyHeroStats(player, kit);
            }
        }

        resetAllArmor();

        state = GameState.PREPARE;
        currentPrepareTime = isFirstRound ? PREPARE_TIME_FIRST : PREPARE_TIME_NORMAL;
        prepareSeconds = currentPrepareTime;
        isFirstRound = false;

        prepareBossBar = BossBar.bossBar(
                Component.text("⏳ 准备时间: " + prepareSeconds + "s", NamedTextColor.GOLD),
                1.0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS
        );
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(prepareBossBar);
        }

        for (Map.Entry<UUID, String> entry : playerTeams.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;

            HeroKit kit = getPlayerHero(p);
            if (kit != null && kit.getId().equals("kayi")) {
                if (BACS.getInstance().getKayiSkillListener() != null) {
                    BACS.getInstance().getKayiSkillListener().onPrepareStart(p);
                }
            }
        }

        for (Map.Entry<UUID, String> entry : playerTeams.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;

            Location spawnLoc = entry.getValue().equals(TEAM_ATTACKER) ? getAttackerSpawnLocation() : getDefenderSpawnLocation();
            teleportSafely(p, spawnLoc);

            HeroKit kit = getPlayerHero(p);
            if (kit != null) {
                applyHeroStats(p, kit);
                soulFireListener.resetAmmo(p, kit);
            }

            // ★ 上回合存活且拥有主武器 -> 准备阶段重新给
            String equippedId = shopManager.getEquippedMainWeapon(p);
            if (equippedId != null) {
                HeroKit weaponKit = getHeroKitById(equippedId);
                if (weaponKit != null && weaponKit.getWeaponMaterial() != Material.AIR) {
                    ItemStack weapon = createWeapon(weaponKit);
                    if (weapon != null) {
                        p.getInventory().setItem(0, weapon);
                        soulFireListener.resetAmmo(p, weaponKit);
                    }
                }
            }

            // ★ 加时赛：经济 set 5000，EX 充能 set max-2
            if (isOvertime) {
                creditManager.setCredits(p.getUniqueId(), OVERTIME_ECONOMY);
                if (kit != null) {
                    int exMax = kit.getExSkillMaxCharge();
                    exSkillManager.setCharge(p, Math.max(0, exMax - 2));
                }
            }

            p.setGameMode(GameMode.SURVIVAL);
            p.setFoodLevel(20);
            p.setSaturation(20);

            giveTeamArmor(p);
            giveShopItem(p);
            freezePlayer(p);
        }

        updateScoreboard();

        prepareTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (state != GameState.PREPARE) { Bukkit.getScheduler().cancelTask(prepareTaskId); prepareTaskId = -1; return; }
                if (prepareSeconds <= 0) {
                    Bukkit.getScheduler().cancelTask(prepareTaskId);
                    prepareTaskId = -1;
                    if (prepareBossBar != null) { for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(prepareBossBar); prepareBossBar = null; }
                    unfreezeAllPlayers();
                    startRound();
                    return;
                }
                if (prepareBossBar != null) {
                    float progress = (float) prepareSeconds / (float) currentPrepareTime;
                    prepareBossBar.progress(Math.max(0, Math.min(1, progress)));
                    prepareBossBar.name(Component.text("⏳ 准备时间: " + prepareSeconds + "s", NamedTextColor.GOLD));
                }
                updateScoreboard();
                prepareSeconds--;
            }
        }, 0L, 20L);
    }

    // ============================================================
    // 回合开始
    // ============================================================

    public void startRound() {
        roundNumber++;

        // ★ 攻防转换：第 9 回合开始（常规赛）
        if (roundNumber == HALF_TIME_ROUND && !halfTimeSwapped) {
            swapTeams();
            Bukkit.broadcast(Component.text("⚔ 攻守已互换！", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true));
        }

        // ★ 加时赛开始：回合 19
        if (roundNumber == OVERTIME_START_ROUND) {
            isOvertime = true;
            overtimeRound = 1;
            Bukkit.broadcast(Component.text("⚡ ⚡ ⚡  加 时 赛 开 始  ⚡ ⚡ ⚡", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true));
        }

        // ★ 加时赛每局攻防转换（第 2 局开始，回合 20 起）
        if (isOvertime && roundNumber > OVERTIME_START_ROUND) {
            overtimeRound = roundNumber - 18;
            swapTeams();
            Bukkit.broadcast(Component.text("⚔ 攻守已互换！（加时赛第 " + overtimeRound + " 局）", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true));
        }

        state = GameState.ROUND_ACTIVE;
        roundTimerSeconds = 160;
        bombDeployedInRound = false;

        roundKills.clear();

        exSkillManager.onRoundStart();
        yuukaSkillListener.onRoundStart();

        roundTimerBossBar = BossBar.bossBar(
                Component.text("⏱ 回合时间: " + formatTime(roundTimerSeconds), NamedTextColor.GREEN),
                1.0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS
        );
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(roundTimerBossBar);
        }

        if (roundTimerTaskId != -1) { Bukkit.getScheduler().cancelTask(roundTimerTaskId); }
        roundTimerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (state != GameState.ROUND_ACTIVE) {
                cleanupRoundTimerBossBar();
                return;
            }
            if (!bombDeployedInRound) {
                roundTimerSeconds--;
                if (roundTimerSeconds <= 0) {
                    endRoundByBomb(TEAM_DEFENDER);
                    return;
                }
                if (roundTimerBossBar != null) {
                    float progress = (float) roundTimerSeconds / 160.0f;
                    roundTimerBossBar.progress(Math.max(0, Math.min(1, progress)));
                    if (roundTimerSeconds <= 30) {
                        roundTimerBossBar.color(BossBar.Color.RED);
                    } else if (roundTimerSeconds <= 60) {
                        roundTimerBossBar.color(BossBar.Color.YELLOW);
                    }
                    roundTimerBossBar.name(Component.text("⏱ 回合时间: " + formatTime(roundTimerSeconds),
                            roundTimerSeconds <= 30 ? NamedTextColor.RED : NamedTextColor.GREEN));
                }
            }
        }, 0L, 20L);

        resetAllArmor();

        for (UUID uuid : playerTeams.keySet()) {
            playerAlive.put(uuid, true);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && !isOvertime) { exSkillManager.addCharge(p, 1); }
        }
        spectators.clear();
        unfreezeAllPlayers();

        for (Map.Entry<UUID, String> entry : playerTeams.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;

            HeroKit kit = getPlayerHero(p);
            if (kit != null) {
                p.setMaxHealth(kit.getMaxHealth());
                p.setHealth(kit.getMaxHealth());
                int maxArmor = kit.getMaxArmor() + getPlayerExtraMaxArmor(p);
                playerCurrentArmor.put(p.getUniqueId(), maxArmor);
                soulFireListener.resetAmmo(p, kit);
            }

            p.setGameMode(GameMode.SURVIVAL);
            p.setFoodLevel(20);
            p.setSaturation(20);

            if (kit != null) {
                String equippedId = shopManager.getEquippedMainWeapon(p);
                HeroKit weaponKit = null;
                if (equippedId != null) {
                    weaponKit = getHeroKitById(equippedId);
                }
                if (weaponKit != null && weaponKit.getWeaponMaterial() != Material.AIR) {
                    ItemStack weapon = createWeapon(weaponKit);
                    if (weapon != null) {
                        p.getInventory().setItem(0, weapon);
                        soulFireListener.resetAmmo(p, weaponKit);
                    }
                }
            }

            if (!shopManager.hasPistol(p) && !shopManager.hasSpiritGun(p) && !shopManager.hasVerdict(p)
                    && !shopManager.hasRage(p) && !shopManager.hasHandCannon(p)) {
                ItemStack pistol = new ItemStack(Material.WOODEN_AXE);
                ItemMeta pistolMeta = pistol.getItemMeta();
                pistolMeta.displayName(Component.text("制式手枪", NamedTextColor.GRAY));
                pistolMeta.setUnbreakable(true);
                pistol.setItemMeta(pistolMeta);
                p.getInventory().setItem(1, pistol);
                shopManager.setHasPistol(p, true);
                shopManager.setPistolAmmo(p, 12);
            }
        }

        bombManager.giveRoundItems();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (getPlayerHero(player) != null && getPlayerHero(player).getId().equals("kayi")) {
                if (BACS.getInstance().getKayiSkillListener() != null) {
                    BACS.getInstance().getKayiSkillListener().onRoundStart(player);
                }
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (BACS.getInstance().getSerinaSkillListener() != null) {
                BACS.getInstance().getSerinaSkillListener().onTeamUpdate(player);
            }
        }

        updateAliveCount();
        updateScoreboard();
    }

    public void onBombDeployed(int bombTimer) {
        bombDeployedInRound = true;
        if (roundTimerBossBar != null) {
            roundTimerBossBar.color(BossBar.Color.RED);
            roundTimerBossBar.name(Component.text("💣 炸药已部署！爆炸时间: " + formatTime(bombTimer), NamedTextColor.RED));
        }
    }

    public void updateBombTimerDisplay(int bombTimer) {
        if (roundTimerBossBar != null && bombDeployedInRound) {
            roundTimerBossBar.name(Component.text("💣 炸药已部署！爆炸时间: " + formatTime(bombTimer), NamedTextColor.RED));
            float progress = (float) bombTimer / 40.0f;
            roundTimerBossBar.progress(Math.max(0, Math.min(1, progress)));
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    private void cleanupRoundTimerBossBar() {
        if (roundTimerTaskId != -1) { Bukkit.getScheduler().cancelTask(roundTimerTaskId); roundTimerTaskId = -1; }
        if (roundTimerBossBar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(roundTimerBossBar);
            roundTimerBossBar = null;
        }
    }

    // ============================================================
    // 回合连杀音效
    // ============================================================

    private void updateRoundKill(Player killer) {
        if (killer == null) return;
        UUID uuid = killer.getUniqueId();

        int streak = roundKills.getOrDefault(uuid, 0) + 1;
        roundKills.put(uuid, streak);

        playKillSound(killer, streak);

        if (streak >= 2) {
            String color = streak >= 10 ? "§c" : streak >= 5 ? "§6" : "§e";
            String emoji = streak >= 10 ? "💀" : streak >= 5 ? "🔥" : "⚔";
            Bukkit.broadcast(Component.text(color + emoji + " " + killer.getName() + " " + streak + " 连杀！", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true));
        }
    }

    private void playKillSound(Player killer, int streak) {
        if (killer == null) return;
        if (streak <= 4) playNormalKillSound(killer, streak);
        else if (streak == 5) playAceKillSound(killer, 5);
        else if (streak == 6) playSixKillSound(killer);
        else playAceKillSound(killer, Math.min(streak, 10));
    }

    private void playNormalKillSound(Player killer, int streak) {
        Location loc = killer.getLocation();
        float pitch = 0.5f + (streak - 1) * 0.15f;
        float volume = 1.8f;

        killer.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, volume, pitch);
        killer.playSound(loc, Sound.BLOCK_GLASS_BREAK, volume * 0.7f, pitch + 0.2f);

        if (streak >= 3) killer.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, volume * 0.6f, pitch + 0.3f);
        if (streak >= 4) killer.playSound(loc, Sound.BLOCK_GLASS_PLACE, volume * 0.5f, pitch + 0.4f);
    }

    private void playAceKillSound(Player killer, int streak) {
        Location loc = killer.getLocation();
        float basePitch = 0.5f + (streak - 5) * 0.03f;
        float volume = 1.8f;
        if (streak >= 10) volume = 2.0f;
        else if (streak >= 7) volume = 1.9f;

        float[][] melody = {
                {0.5f, 0.5f}, {0.6f, 0.55f}, {0.7f, 0.6f}, {0.8f, 0.65f},
                {1.0f, 0.8f}, {1.1f, 0.85f}, {1.2f, 0.9f}, {1.3f, 0.9f},
                {1.1f, 0.7f}, {0.9f, 0.6f}, {0.7f, 0.5f}, {0.5f, 0.4f}
        };

        for (int i = 0; i < melody.length; i++) {
            final int index = i;
            final float pitch = melody[i][0] + basePitch - 0.5f;
            final float vol = melody[i][1] * volume;
            final int delay = i * 4;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                killer.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, vol, pitch);
                killer.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, vol * 0.5f, pitch + 0.3f);
                if (index >= 4 && index <= 7) killer.playSound(loc, Sound.BLOCK_GLASS_BREAK, vol * 0.4f, pitch + 0.2f);
                if (index >= melody.length - 3) killer.playSound(loc, Sound.BLOCK_GLASS_PLACE, vol * 0.3f, pitch - 0.1f);
            }, delay);
        }
    }

    private void playSixKillSound(Player killer) {
        Location loc = killer.getLocation();
        float volume = 1.9f;

        float[][] melody = {
                {0.6f, 0.5f}, {0.7f, 0.55f}, {0.8f, 0.6f}, {0.9f, 0.65f},
                {1.1f, 0.8f}, {1.2f, 0.85f}, {1.3f, 0.9f}, {1.4f, 0.9f},
                {1.2f, 0.7f}, {1.0f, 0.6f}, {0.8f, 0.5f}, {0.6f, 0.4f}
        };

        for (int i = 0; i < melody.length; i++) {
            final int index = i;
            final float pitch = melody[i][0];
            final float vol = melody[i][1] * volume;
            final int delay = i * 4;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                killer.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, vol, pitch);
                killer.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, vol * 0.5f, pitch + 0.3f);
                if (index % 2 == 0) killer.playSound(loc, Sound.BLOCK_GLASS_BREAK, vol * 0.3f, pitch + 0.4f);
                if (index >= melody.length - 3) killer.playSound(loc, Sound.BLOCK_GLASS_PLACE, vol * 0.5f, pitch + 0.5f);
            }, delay);
        }
    }

    // ============================================================
    // 玩家死亡处理
    // ============================================================

    public void handlePlayerDeath(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerAlive.containsKey(uuid)) return;
        if (!playerAlive.get(uuid)) return;

        playerAlive.put(uuid, false);
        spectators.add(uuid);

        player.getInventory().setHelmet(null);
        player.getInventory().setBoots(null);
        shopManager.onPlayerDeath(player);
        player.setGameMode(GameMode.SPECTATOR);

        Player killer = player.getKiller();
        if (killer != null) {
            creditManager.onKill(killer.getUniqueId());
            exSkillManager.addCharge(killer, 1);
            exSkillManager.onKill(killer);
            BACS.getInstance().getCoinManager().onKill(killer);
            if (BACS.getInstance().getIzumiSkillListener() != null) {
                BACS.getInstance().getIzumiSkillListener().onKill(killer, player);
            }
            if (BACS.getInstance().getMikaSkillListener() != null) {
                BACS.getInstance().getMikaSkillListener().onKill(killer, player);
                BACS.getInstance().getMikaSkillListener().onBulletTimeKill(killer);
            }
            if (BACS.getInstance().getHoshinoSkillListener() != null) {
                BACS.getInstance().getHoshinoSkillListener().onKill(killer);
            }

            updateRoundKill(killer);
        }

        exSkillManager.onPlayerDeath(player);
        yuukaSkillListener.onPlayerDeath(player);

        if (BACS.getInstance().getSerinaSkillListener() != null) {
            BACS.getInstance().getSerinaSkillListener().onPlayerDeath(player);
        }
        if (BACS.getInstance().getRakuFuSkillListener() != null) {
            BACS.getInstance().getRakuFuSkillListener().onPlayerDeath(player);
        }
        if (BACS.getInstance().getHoshinoSkillListener() != null) {
            BACS.getInstance().getHoshinoSkillListener().onPlayerDeath(player);
        }

        updateAliveCount();
        updateScoreboard();

        if (bombManager.isBombDeployed()) {
            if (defenderAlive == 0 && state == GameState.ROUND_ACTIVE) {
                endRoundByBomb(GameManager.TEAM_ATTACKER);
            }
            return;
        }

        checkRoundEnd();
    }

    private void updateAliveCount() {
        attackerAlive = 0;
        defenderAlive = 0;
        for (Map.Entry<UUID, String> entry : playerTeams.entrySet()) {
            if (playerAlive.getOrDefault(entry.getKey(), false)) {
                if (entry.getValue().equals(TEAM_ATTACKER)) attackerAlive++;
                else defenderAlive++;
            }
        }
    }

    private void checkRoundEnd() {
        if (state != GameState.ROUND_ACTIVE) return;

        if (bombManager.isBombDeployed()) {
            if (defenderAlive == 0) {
                endRoundByBomb(GameManager.TEAM_ATTACKER);
            }
            return;
        }

        if (attackerAlive == 0 || defenderAlive == 0) {
            endRound();
        }
    }

    // ============================================================
    // 回合结束
    // ============================================================

    private void endRound() {
        if (state == GameState.ROUND_END) return;
        cleanupRoundTimerBossBar();
        state = GameState.ROUND_END;
        roundEndSeconds = 6;
        bombManager.clearDeployedBombBlock();

        String winner = null;

        if (bombManager.isBombDeployed()) {
            if (defenderAlive == 0) winner = TEAM_ATTACKER;
            else winner = TEAM_DEFENDER;
        } else {
            if (attackerAlive > 0 && defenderAlive == 0) winner = TEAM_ATTACKER;
            else if (defenderAlive > 0 && attackerAlive == 0) winner = TEAM_DEFENDER;
        }

        if (winner == null) {
            startRoundEndCountdown();
            return;
        }

        // ★ 第 8 回合结束提示攻防转换
        broadcastHalfTimeWarning();

        if (winner.equals(TEAM_ATTACKER)) {
            attackerScore++;
            Bukkit.broadcast(Component.text("🏆 攻方赢得本回合！", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
            if (!isOvertime) {
                for (UUID uuid : playerTeams.keySet()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) continue;
                    if (playerTeams.get(uuid).equals(TEAM_ATTACKER)) {
                        creditManager.onRoundWin(uuid);
                        BACS.getInstance().getCoinManager().onRoundWin(p);
                    } else {
                        creditManager.onRoundLose(uuid);
                        BACS.getInstance().getCoinManager().onRoundLose(p);
                    }
                }
            }
        } else {
            defenderScore++;
            Bukkit.broadcast(Component.text("🏆 守方赢得本回合！", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
            if (!isOvertime) {
                for (UUID uuid : playerTeams.keySet()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) continue;
                    if (playerTeams.get(uuid).equals(TEAM_DEFENDER)) {
                        creditManager.onRoundWin(uuid);
                        BACS.getInstance().getCoinManager().onRoundWin(p);
                    } else {
                        creditManager.onRoundLose(uuid);
                        BACS.getInstance().getCoinManager().onRoundLose(p);
                    }
                }
            }
        }

        BACS.getInstance().getCoinManager().resetRoundKills();

        if (checkGameWin()) return;

        // ★ 加时赛投票检查
        if (isOvertime && checkOvertimeVote()) return;

        updateScoreboard();
        startRoundEndCountdown();
    }

    public void endRoundByBomb(String winnerTeam) {
        if (state == GameState.ROUND_END || state == GameState.GAME_OVER) return;
        if (state != GameState.ROUND_ACTIVE) return;

        cleanupRoundTimerBossBar();
        state = GameState.ROUND_END;
        roundEndSeconds = 6;
        bombManager.clearDeployedBombBlock();

        if (roundEndTaskId != -1) {
            Bukkit.getScheduler().cancelTask(roundEndTaskId);
            roundEndTaskId = -1;
        }

        // ★ 第 8 回合结束提示攻防转换
        broadcastHalfTimeWarning();

        if (winnerTeam.equals(TEAM_ATTACKER)) {
            attackerScore++;
            Bukkit.broadcast(Component.text("💣 炸弹引爆！攻方获胜！", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
            if (!isOvertime) {
                for (UUID uuid : playerTeams.keySet()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) continue;
                    if (playerTeams.get(uuid).equals(TEAM_ATTACKER)) {
                        creditManager.onRoundWin(uuid);
                        BACS.getInstance().getCoinManager().onRoundWin(p);
                    } else {
                        creditManager.onRoundLose(uuid);
                        BACS.getInstance().getCoinManager().onRoundLose(p);
                    }
                }
            }
        } else {
            defenderScore++;
            Bukkit.broadcast(Component.text("✅ 守方获胜！", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
            if (!isOvertime) {
                for (UUID uuid : playerTeams.keySet()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) continue;
                    if (playerTeams.get(uuid).equals(TEAM_DEFENDER)) {
                        creditManager.onRoundWin(uuid);
                        BACS.getInstance().getCoinManager().onRoundWin(p);
                    } else {
                        creditManager.onRoundLose(uuid);
                        BACS.getInstance().getCoinManager().onRoundLose(p);
                    }
                }
            }
        }

        BACS.getInstance().getCoinManager().resetRoundKills();

        if (checkGameWin()) return;

        // ★ 加时赛投票检查
        if (isOvertime && checkOvertimeVote()) return;

        updateScoreboard();
        startRoundEndCountdown();
    }

    /**
     * ★ 第 8 回合结束提示攻防转换
     */
    private void broadcastHalfTimeWarning() {
        if (roundNumber == HALF_TIME_WARNING_ROUND && !halfTimeSwapped) {
            Bukkit.broadcast(Component.text("", NamedTextColor.WHITE));
            Bukkit.broadcast(Component.text("⚔ ⚔ ⚔  攻 防 转 换  ⚔ ⚔ ⚔", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true));
            Bukkit.broadcast(Component.text("下一回合攻守互换！", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.BOLD, true));
            Bukkit.broadcast(Component.text("", NamedTextColor.WHITE));

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle("§6§l⚔ 攻防转换", "§e下一回合攻守互换", 10, 60, 20);
            }
        }
    }

    /**
     * ★ 加时赛投票检查
     * @return true 表示触发了投票（结束流程交给 VoteManager）
     */
    private boolean checkOvertimeVote() {
        // 第 3 局结束 -> 投票（阈值 0.5）
        if (overtimeRound == 3) {
            plugin.getVoteManager().startVote(0.5);
            return true;
        }
        // 第 5 局结束 -> 投票（阈值 2/3）
        if (overtimeRound == 5) {
            plugin.getVoteManager().startVote(2.0 / 3.0);
            return true;
        }
        // 第 7 局结束 -> 直接平局
        if (overtimeRound == 7) {
            endGameTie();
            return true;
        }
        return false;
    }

    /**
     * ★ 投票结束回调
     */
    public void onVoteFinished(boolean tie) {
        if (tie) {
            endGameTie();
        } else {
            // 继续比赛 -> 进入准备阶段
            startPreparePhase();
        }
    }

    private boolean checkGameWin() {
        if (!isOvertime) {
            if (attackerScore >= 10) { endGame(TEAM_ATTACKER); return true; }
            if (defenderScore >= 10) { endGame(TEAM_DEFENDER); return true; }
            if (attackerScore == 9 && defenderScore == 9) {
                isOvertime = true;
                updateScoreboard();
                // 不立刻开始加时，等这一轮 endRound 结束
            }
        } else {
            // ★ 加时赛：领先 2 分即胜
            if (attackerScore - defenderScore >= 2) { endGame(TEAM_ATTACKER); return true; }
            if (defenderScore - attackerScore >= 2) { endGame(TEAM_DEFENDER); return true; }
        }
        return false;
    }

    private void endGame(String winner) {
        state = GameState.GAME_OVER;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SPECTATOR);

            String team = getPlayerTeam(player);
            if (team != null && team.equals(winner)) {
                BACS.getInstance().getCoinManager().onMatchWin(player);
            } else if (team != null) {
                BACS.getInstance().getCoinManager().onMatchLose(player);
            }

            BACS.getInstance().getCoinManager().showMatchSummary(player);
        }
        updateScoreboard();
    }

    /**
     * ★ 平局：每人 +500 硬币，然后回到等待状态
     */
    private void endGameTie() {
        state = GameState.GAME_OVER;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SPECTATOR);
            BACS.getInstance().getCoinManager().addCoins(player, 500);
            player.sendMessage(Component.text("⚖ 比赛平局！获得 500 硬币。", NamedTextColor.GOLD));
            BACS.getInstance().getCoinManager().showMatchSummary(player);
        }
        Bukkit.broadcast(Component.text("⚖ ⚖ ⚖  比 赛 平 局  ⚖ ⚖ ⚖", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        updateScoreboard();

        // 回到等待状态
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            performReset();
        }, 100L);
    }

    private void startRoundEndCountdown() {
        if (roundEndTaskId != -1) Bukkit.getScheduler().cancelTask(roundEndTaskId);
        roundEndTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (state != GameState.ROUND_END) { Bukkit.getScheduler().cancelTask(roundEndTaskId); roundEndTaskId = -1; return; }
                if (roundEndSeconds <= 0) {
                    Bukkit.getScheduler().cancelTask(roundEndTaskId);
                    roundEndTaskId = -1;
                    resetForNextRound();
                    return;
                }
                updateScoreboard();
                roundEndSeconds--;
            }
        }, 0L, 20L);
    }

    private void resetForNextRound() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item) {
                    entity.remove();
                }
            }
        }

        unfreezeAllPlayers();
        bombManager.cleanup();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (getPlayerHero(player) != null && getPlayerHero(player).getId().equals("kayi")) {
                if (BACS.getInstance().getKayiSkillListener() != null) {
                    BACS.getInstance().getKayiSkillListener().onRoundEnd(player);
                }
            }
        }

        for (UUID uuid : spectators) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                p.setHealth(20);
                p.setFoodLevel(20);
                p.setSaturation(20);
                removeShopItem(p);

                String team = playerTeams.get(uuid);
                if (team != null) {
                    Location spawnLoc = team.equals(TEAM_ATTACKER) ? getAttackerSpawnLocation() : getDefenderSpawnLocation();
                    teleportSafely(p, spawnLoc);
                    if (p.getInventory().getHelmet() == null || p.getInventory().getBoots() == null) { giveTeamArmor(p); }
                    HeroKit kit = getPlayerHero(p);
                    if (kit != null) {
                        String equippedId = shopManager.getEquippedMainWeapon(p);
                        HeroKit weaponKit = null;
                        if (equippedId != null) {
                            weaponKit = getHeroKitById(equippedId);
                        }
                        if (weaponKit != null && weaponKit.getWeaponMaterial() != Material.AIR) {
                            ItemStack weapon = createWeapon(weaponKit);
                            if (weapon != null) {
                                p.getInventory().setItem(0, weapon);
                                soulFireListener.resetAmmo(p, weaponKit);
                            }
                        }

                        if (!shopManager.hasPistol(p) && !shopManager.hasSpiritGun(p) && !shopManager.hasVerdict(p)
                                && !shopManager.hasRage(p) && !shopManager.hasHandCannon(p)) {
                            ItemStack pistol = new ItemStack(Material.WOODEN_AXE);
                            ItemMeta pistolMeta = pistol.getItemMeta();
                            pistolMeta.displayName(Component.text("制式手枪", NamedTextColor.GRAY));
                            pistolMeta.setUnbreakable(true);
                            pistol.setItemMeta(pistolMeta);
                            p.getInventory().setItem(1, pistol);
                            shopManager.setHasPistol(p, true);
                            shopManager.setPistolAmmo(p, 12);
                        }
                    }
                }
            }
        }
        spectators.clear();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (BACS.getInstance().getSerinaSkillListener() != null) {
                BACS.getInstance().getSerinaSkillListener().onTeamUpdate(player);
            }
        }

        startPreparePhase();
    }

    public void checkAndStartCountdown() {
        if (state == GameState.WAITING || state == GameState.COUNTDOWN) {
            int playerCount = Bukkit.getOnlinePlayers().size();
            if (playerCount >= 4 && state == GameState.WAITING) startCountdown();
            else if (playerCount < 4 && state == GameState.COUNTDOWN) { cancelCountdown(); }
        }
    }

    private void startCountdown() {
        if (countdownTaskId != -1) return;
        state = GameState.COUNTDOWN;
        countdownSeconds = 30;
        countdownBossBar = BossBar.bossBar(Component.text("⏳ 游戏即将开始...", NamedTextColor.GOLD), 1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        for (Player player : Bukkit.getOnlinePlayers()) player.showBossBar(countdownBossBar);
        countdownTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().size() < 4) { cancelCountdown(); return; }
                if (countdownSeconds <= 0) {
                    Bukkit.getScheduler().cancelTask(countdownTaskId);
                    countdownTaskId = -1;
                    if (countdownBossBar != null) { for (Player player : Bukkit.getOnlinePlayers()) player.hideBossBar(countdownBossBar); countdownBossBar = null; }
                    openTeamMenuForAll();
                    state = GameState.HERO_SELECT;
                    return;
                }
                if (countdownBossBar != null) {
                    countdownBossBar.progress((float) countdownSeconds / 30.0f);
                    countdownBossBar.name(Component.text("⏳ 游戏将在 " + countdownSeconds + " 秒后开始", NamedTextColor.GOLD));
                }
                countdownSeconds--;
            }
        }, 0L, 20L);
    }

    public void cancelCountdown() {
        if (countdownTaskId != -1) { Bukkit.getScheduler().cancelTask(countdownTaskId); countdownTaskId = -1; }
        if (countdownBossBar != null) { for (Player player : Bukkit.getOnlinePlayers()) player.hideBossBar(countdownBossBar); countdownBossBar = null; }
        if (state == GameState.COUNTDOWN) state = GameState.WAITING;
    }

    public void forceStart() {
        if (state == GameState.ROUND_ACTIVE || state == GameState.ROUND_END) return;
        forceStartGame();
    }

    public void openTeamMenuForAll() {
        for (Player player : Bukkit.getOnlinePlayers()) openTeamMenu(player);
    }

    public void openTeamMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("⚔ 选择你的阵营", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));

        ItemStack attackerWool = new ItemStack(Material.RED_WOOL);
        ItemMeta attackerMeta = attackerWool.getItemMeta();
        attackerMeta.displayName(Component.text("🔴 攻方", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
        attackerWool.setItemMeta(attackerMeta);
        inv.setItem(11, attackerWool);

        ItemStack defenderWool = new ItemStack(Material.BLUE_WOOL);
        ItemMeta defenderMeta = defenderWool.getItemMeta();
        defenderMeta.displayName(Component.text("🔵 守方", NamedTextColor.BLUE).decoration(TextDecoration.BOLD, true));
        defenderWool.setItemMeta(defenderMeta);
        inv.setItem(15, defenderWool);

        player.openInventory(inv);
    }

    public void selectTeam(Player player, String team) {
        if (state == GameState.ROUND_ACTIVE || state == GameState.ROUND_END || state == GameState.PREPARE || state == GameState.HERO_SELECT || state == GameState.VOTE) return;

        if (playerTeams.containsKey(player.getUniqueId())) removePlayerFromTeam(player);
        playerTeams.put(player.getUniqueId(), team);
        playerAlive.put(player.getUniqueId(), true);

        Team scoreboardTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(team);
        if (scoreboardTeam != null) scoreboardTeam.addEntry(player.getName());

        player.closeInventory();

        Location spawnLoc = team.equals(TEAM_ATTACKER) ? getAttackerSpawnLocation() : getDefenderSpawnLocation();
        teleportSafely(player, spawnLoc);

        checkAllTeamsSelected();
        updateScoreboard();
    }

    private void checkAllTeamsSelected() {
        if (state != GameState.HERO_SELECT) return;
        int total = Bukkit.getOnlinePlayers().size();
        if (playerTeams.size() == total && total >= 2) {
            startHeroSelectPhase();
        }
    }

    public void removePlayerFromTeam(Player player) {
        String team = playerTeams.remove(player.getUniqueId());
        playerAlive.remove(player.getUniqueId());
        if (team != null) {
            Team scoreboardTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(team);
            if (scoreboardTeam != null) scoreboardTeam.removeEntry(player.getName());
        }
    }

    public String getPlayerTeam(Player player) { return playerTeams.get(player.getUniqueId()); }
    public int getCurrentPlayerCount() { return Bukkit.getOnlinePlayers().size(); }
    public boolean isPlayerAlive(Player player) { return playerAlive.getOrDefault(player.getUniqueId(), false); }

    public void checkForReset() {
        if (state == GameState.WAITING || state == GameState.COUNTDOWN || state == GameState.VOTE) {
            return;
        }

        if (state == GameState.GAME_OVER) {
            performReset();
            return;
        }

        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        if (onlinePlayers == 0) {
            performReset();
            return;
        }

        boolean hasAttacker = false;
        boolean hasDefender = false;
        boolean hasAssignedPlayers = false;

        for (Player player : Bukkit.getOnlinePlayers()) {
            String team = getPlayerTeam(player);
            if (team == null) continue;
            hasAssignedPlayers = true;
            if (team.equals(TEAM_ATTACKER)) hasAttacker = true;
            else if (team.equals(TEAM_DEFENDER)) hasDefender = true;
        }

        if (hasAssignedPlayers && (!hasAttacker || !hasDefender)) {
            performReset();
        }
    }

    private void performReset() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item item) {
                    ItemStack stack = item.getItemStack();
                    if (stack != null && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
                        String name = stack.getItemMeta().displayName().toString();
                        if (name.contains("闪光弹") || name.contains("烟雾弹") || name.contains("炸药")) {
                            entity.remove();
                        }
                    }
                }
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(new Location(voidWorld, 0, 102, 0));
            player.setHealth(20);
            player.setMaxHealth(20);
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
            unfreezePlayer(player);
        }

        resetGame();
    }

    public void resetGame() {
        cancelCountdown();
        cleanupRoundTimerBossBar();
        if (prepareTaskId != -1) { Bukkit.getScheduler().cancelTask(prepareTaskId); prepareTaskId = -1; }
        if (roundEndTaskId != -1) { Bukkit.getScheduler().cancelTask(roundEndTaskId); roundEndTaskId = -1; }
        if (heroSelectTaskId != -1) { Bukkit.getScheduler().cancelTask(heroSelectTaskId); heroSelectTaskId = -1; }
        if (prepareBossBar != null) { for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(prepareBossBar); prepareBossBar = null; }
        if (heroSelectBossBar != null) { for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(heroSelectBossBar); heroSelectBossBar = null; }
        if (healthDisplayTaskId != -1) { Bukkit.getScheduler().cancelTask(healthDisplayTaskId); healthDisplayTaskId = -1; }

        if (plugin.getVoteManager() != null) {
            plugin.getVoteManager().forceEnd();
        }

        bombManager.cleanup();
        unfreezeAllPlayers();
        creditManager.resetAll();
        shopManager.resetAll();
        ammoManager.resetAll();
        exSkillManager.resetAll();
        yuukaSkillListener.resetAll();
        if (BACS.getInstance().getKayiSkillListener() != null) {
            BACS.getInstance().getKayiSkillListener().resetAll();
        }
        if (BACS.getInstance().getIzumiSkillListener() != null) {
            BACS.getInstance().getIzumiSkillListener().resetAll();
        }
        if (BACS.getInstance().getMikaSkillListener() != null) {
            BACS.getInstance().getMikaSkillListener().resetAll();
        }
        if (BACS.getInstance().getSerinaSkillListener() != null) {
            BACS.getInstance().getSerinaSkillListener().resetAll();
        }
        if (BACS.getInstance().getRakuFuSkillListener() != null) {
            BACS.getInstance().getRakuFuSkillListener().resetAll();
        }
        if (BACS.getInstance().getHoshinoSkillListener() != null) {
            BACS.getInstance().getHoshinoSkillListener().resetAll();
        }
        if (BACS.getInstance().getShieldManager() != null) {
            BACS.getInstance().getShieldManager().resetAll();
        }
        if (BACS.getInstance().getStabilityManager() != null) {
            BACS.getInstance().getStabilityManager().resetAll();
        }
        BACS.getInstance().getCoinManager().resetAll();

        roundKills.clear();

        for (Player player : Bukkit.getOnlinePlayers()) {
            removePlayerFromTeam(player);
            player.setGameMode(GameMode.SURVIVAL);
            removeShopItem(player);
            removeTeamArmor(player);
            playerCurrentArmor.remove(player.getUniqueId());
            playerExtraMaxArmor.remove(player.getUniqueId());
            if (voidWorld != null) { player.teleport(new Location(voidWorld, 0, 102, 0)); }
        }
        playerTeams.clear();
        playerAlive.clear();
        playerHeroes.clear();
        spectators.clear();

        attackerScore = 0;
        defenderScore = 0;
        attackerAlive = 0;
        defenderAlive = 0;
        roundNumber = 0;
        isOvertime = false;
        halfTimeSwapped = false;   // ★ 重置
        overtimeRound = 0;          // ★ 重置
        isFirstRound = true;
        currentPrepareTime = 0;
        allHeroesSelected = false;

        state = GameState.WAITING;
        checkAndStartCountdown();
        updateScoreboard();
        startHealthDisplay();
    }

    // ★ 加时赛 getter
    public boolean isOvertime() { return isOvertime; }
    public int getOvertimeRound() { return overtimeRound; }
}