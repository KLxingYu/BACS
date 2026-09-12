package com.bACS;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopManager {
    private final BACS plugin;
    private final GameManager gameManager;
    private final CreditManager creditManager;

    // ===== 副武器状态 =====
    // 制式手枪
    private final Map<UUID, Boolean> hasPistol = new HashMap<>();
    private final Map<UUID, Integer> pistolAmmo = new HashMap<>();
    private final Map<UUID, Integer> pistolReserve = new HashMap<>();     // ★ 备用弹药
    private static final int PISTOL_MAX_AMMO = 12;
    private static final int PISTOL_INITIAL_RESERVE = 12;
    private static final int PISTOL_PRICE = 100;

    // 灵异
    private final Map<UUID, Boolean> hasSpiritGun = new HashMap<>();
    private final Map<UUID, Integer> spiritGunAmmo = new HashMap<>();
    private final Map<UUID, Integer> spiritGunReserve = new HashMap<>();  // ★ 备用弹药
    private static final int SPIRIT_GUN_MAX_AMMO = 15;
    private static final int SPIRIT_GUN_INITIAL_RESERVE = 30;
    private static final int SPIRIT_GUN_PRICE = 300;

    // 裁决
    private final Map<UUID, Boolean> hasVerdict = new HashMap<>();
    private final Map<UUID, Integer> verdictAmmo = new HashMap<>();
    private final Map<UUID, Integer> verdictReserve = new HashMap<>();   // ★ 备用弹药
    private static final int VERDICT_MAX_AMMO = 6;
    private static final int VERDICT_INITIAL_RESERVE = 12;
    private static final int VERDICT_PRICE = 900;

    // 怒焰
    private final Map<UUID, Boolean> hasRage = new HashMap<>();
    private final Map<UUID, Integer> rageAmmo = new HashMap<>();
    private final Map<UUID, Integer> rageReserve = new HashMap<>();      // ★ 备用弹药

    // 手炮
    private final Map<UUID, Boolean> hasHandCannon = new HashMap<>();
    private final Map<UUID, Integer> handCannonAmmo = new HashMap<>();
    private final Map<UUID, Integer> handCannonReserve = new HashMap<>(); // ★ 备用弹药

    // ===== 主武器状态 =====
    private final Map<UUID, Set<String>> ownedMainWeapons = new HashMap<>();
    private final Map<UUID, String> equippedMainWeapon = new HashMap<>();
    private static final Map<String, Integer> MAIN_WEAPON_PRICES = new HashMap<>();
    static {
        MAIN_WEAPON_PRICES.put("al1s", 4200);
        MAIN_WEAPON_PRICES.put("kuroko", 2000);
        MAIN_WEAPON_PRICES.put("kayi", 3200);
        MAIN_WEAPON_PRICES.put("izumi", 1600);
        MAIN_WEAPON_PRICES.put("mika", 2500);
        MAIN_WEAPON_PRICES.put("yuuka", 1550);
        MAIN_WEAPON_PRICES.put("serina", 1350);
        MAIN_WEAPON_PRICES.put("rakufu", 2200);
    }

    // ===== 防具状态 =====
    private final Map<UUID, Boolean> hasRegenArmor = new HashMap<>();
    private final Map<UUID, Integer> regenPool = new HashMap<>();
    private static final int REGEN_ARMOR_MAX = 75;
    private static final int REGEN_POOL_MAX = 125;
    private static final int REGEN_ARMOR_PRICE = 1000;

    private final Map<UUID, Boolean> hasHeavyArmor = new HashMap<>();
    private static final int HEAVY_ARMOR_MAX = 120;
    private static final double HEAVY_ARMOR_ABSORPTION = 0.66;
    private static final int HEAVY_ARMOR_PRICE = 1250;

    private final Map<UUID, Boolean> hasBodyArmor = new HashMap<>();
    private final Map<UUID, Integer> bodyArmorDurability = new HashMap<>();
    private static final int BODY_ARMOR_MAX = 100;
    private static final int BODY_ARMOR_PRICE = 500;

    private final Map<UUID, Boolean> hasHelmet = new HashMap<>();
    private final Map<UUID, Integer> helmetDurability = new HashMap<>();
    private static final int HELMET_MAX = 80;

    private final Map<UUID, Integer> damageReductionCount = new HashMap<>();
    private static final int DAMAGE_REDUCTION_MAX = 12;
    private static final int DAMAGE_REDUCTION_PRICE = 300;

    private final Map<UUID, Long> sidearmLastShot = new HashMap<>();
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();

    public ShopManager(BACS plugin, GameManager gameManager, CreditManager creditManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.creditManager = creditManager;
    }

    // ============================================================
    // 商店界面
    // ============================================================
    public void openShop(Player player) {
        if (gameManager.getState() != GameState.PREPARE) {
            player.sendMessage(Component.text("❌ 只能在准备阶段打开商店！", NamedTextColor.RED));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("🛒 商店", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));

        UUID uuid = player.getUniqueId();
        int credits = creditManager.getCredits(uuid);

        // ===== 行1: 副武器 (索引 0-8) =====
        inv.setItem(0, createShopItem(Material.WOODEN_AXE,
                "🔫 制式手枪", NamedTextColor.GRAY,
                "价格: " + PISTOL_PRICE + "信用点",
                "伤害: 头45/胸25/四肢20",
                "弹夹: 12发 | 备用: 12发",
                hasPistol.getOrDefault(uuid, false) ? "✅ 已拥有" : "点击购买",
                "信用点: " + credits));

        inv.setItem(1, createShopItem(Material.IRON_AXE,
                "👻 灵异", NamedTextColor.DARK_PURPLE,
                "价格: " + SPIRIT_GUN_PRICE + "信用点",
                "伤害: 头65/胸30/四肢26",
                "弹夹: 15发 | 备用: 30发",
                hasSpiritGun.getOrDefault(uuid, false) ? "✅ 已拥有" : "点击购买",
                "信用点: " + credits));

        inv.setItem(2, createShopItem(Material.IRON_AXE,
                "⚖ 裁决", NamedTextColor.GOLD,
                "价格: " + VERDICT_PRICE + "信用点",
                "伤害: 头120/胸50/四肢45",
                "弹夹: 6发 | 备用: 12发",
                hasVerdict.getOrDefault(uuid, false) ? "✅ 已拥有" : "点击购买",
                "信用点: " + credits));

        inv.setItem(3, createShopItem(Material.GOLDEN_AXE,
                "🔥 怒焰", NamedTextColor.GOLD,
                "价格: " + WeaponConfig.RAGE_PRICE + "信用点",
                "伤害: 头79/胸29/四肢20",
                "24格后衰减30%",
                "穿透1个方块(50%伤害)",
                "弹夹: 15发 | 备用: 30发",
                "射速: 8.2发/秒 | 换弹: 1秒",
                hasRage.getOrDefault(uuid, false) ? "✅ 已拥有" : "点击购买",
                "信用点: " + credits));

        inv.setItem(4, createShopItem(Material.NETHERITE_AXE,
                "💥 手炮", NamedTextColor.RED,
                "价格: " + WeaponConfig.HANDCANNON_PRICE + "信用点",
                "伤害: 头20/胸10/四肢8",
                "每10格衰减20% | 最大40格",
                "每次发射13发弹丸",
                "弹夹: 2发 | 备用: 6发",
                "射速: 6发/秒",
                hasHandCannon.getOrDefault(uuid, false) ? "✅ 已拥有" : "点击购买",
                "信用点: " + credits));

        // ===== 行2: 主武器 (索引 9-17) =====
        Set<String> owned = ownedMainWeapons.getOrDefault(uuid, new HashSet<>());
        String equipped = equippedMainWeapon.getOrDefault(uuid, "");

        HeroKit playerKit = gameManager.getPlayerHero(player);
        boolean isHoshinoDefense = playerKit != null && playerKit.getId().equals("hoshino_defense");

        if (isHoshinoDefense) {
            ItemStack al1sLocked = new ItemStack(Material.DIAMOND_HOE);
            ItemMeta meta = al1sLocked.getItemMeta();
            meta.displayName(Component.text("⚔ 超新星·光之剑", NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.text("价格: 4200信用点", NamedTextColor.GRAY),
                    Component.text("武器: 超新星·光之剑", NamedTextColor.GRAY),
                    Component.text("❌ 防御形态星野无法购买", NamedTextColor.RED),
                    Component.text("信用点: " + credits, NamedTextColor.GRAY)
            ));
            meta.setUnbreakable(true);
            al1sLocked.setItemMeta(meta);
            inv.setItem(9, al1sLocked);
        } else {
            inv.setItem(9, createMainWeaponItem("al1s", "AL1S", NamedTextColor.AQUA,
                    "超新星·光之剑", 4200, owned, equipped, credits));
        }

        inv.setItem(10, createMainWeaponItem("kuroko", "黑子", NamedTextColor.GOLD,
                "α·突击步枪", 2000, owned, equipped, credits));

        inv.setItem(11, createMainWeaponItem("kayi", "凯伊", NamedTextColor.LIGHT_PURPLE,
                "改装·光之剑", 3200, owned, equipped, credits));

        inv.setItem(12, createMainWeaponItem("izumi", "泉", NamedTextColor.GREEN,
                "α·奇兵", 1600, owned, equipped, credits));

        inv.setItem(13, createMainWeaponItem("mika", "未花", NamedTextColor.LIGHT_PURPLE,
                "Quis ut Deus", 2500, owned, equipped, credits));

        inv.setItem(14, createMainWeaponItem("yuuka", "优香", NamedTextColor.BLUE,
                "逻辑与理性", 1550, owned, equipped, credits));

        inv.setItem(15, createMainWeaponItem("serina", "芹奈", NamedTextColor.WHITE,
                "神名", 1350, owned, equipped, credits));

        inv.setItem(16, createMainWeaponItem("rakufu", "拉洛芙", NamedTextColor.LIGHT_PURPLE,
                "破晓", 2200, owned, equipped, credits));

        // ===== 行3: 防具 (索引 18-26) =====
        int armorDur = bodyArmorDurability.getOrDefault(uuid, 0);
        inv.setItem(18, createShopItem(Material.IRON_CHESTPLATE,
                "🛡 防弹衣", NamedTextColor.AQUA,
                "价格: " + BODY_ARMOR_PRICE + "信用点",
                "耐久: " + armorDur + "/" + BODY_ARMOR_MAX,
                hasBodyArmor.getOrDefault(uuid, false) ? "✅ 已拥有" : "点击购买",
                "信用点: " + credits));

        int helmDur = helmetDurability.getOrDefault(uuid, 0);
        inv.setItem(19, createShopItem(Material.DIAMOND_CHESTPLATE,
                "🛡 防弹衣+头盔套装", NamedTextColor.GOLD,
                "价格: 800信用点",
                "护甲耐久: " + armorDur + "/" + BODY_ARMOR_MAX,
                "头盔耐久: " + helmDur + "/" + HELMET_MAX,
                "信用点: " + credits));

        int pool = regenPool.getOrDefault(uuid, 0);
        inv.setItem(20, createShopItem(Material.IRON_CHESTPLATE,
                "💚 回复甲", NamedTextColor.GREEN,
                "价格: " + REGEN_ARMOR_PRICE + "信用点",
                "护甲上限: " + REGEN_ARMOR_MAX + "点",
                "回复池: " + pool + "/" + REGEN_POOL_MAX,
                "吸收所有伤害",
                hasRegenArmor.getOrDefault(uuid, false) ? "✅ 已拥有" : "点击购买",
                "信用点: " + credits));

        inv.setItem(21, createShopItem(Material.DIAMOND_CHESTPLATE,
                "🛡 重甲", NamedTextColor.AQUA,
                "价格: " + HEAVY_ARMOR_PRICE + "信用点",
                "护甲上限: " + HEAVY_ARMOR_MAX + "点",
                "吸收 " + (int)(HEAVY_ARMOR_ABSORPTION * 100) + "% 伤害",
                hasHeavyArmor.getOrDefault(uuid, false) ? "✅ 已拥有" : "点击购买",
                "信用点: " + credits));

        // ===== 行4: 特殊强化 (索引 27-35) =====
        int redCount = damageReductionCount.getOrDefault(uuid, 0);
        inv.setItem(27, createShopItem(Material.NETHER_STAR,
                "🛡 伤害减免强化", NamedTextColor.GOLD,
                "价格: " + DAMAGE_REDUCTION_PRICE + "信用点",
                "已强化: " + redCount + "/" + DAMAGE_REDUCTION_MAX,
                "每级减免0.95%伤害",
                "信用点: " + credits));

        StabilityManager sm = BACS.getInstance().getStabilityManager();
        int currentStability = sm != null ? sm.getStability(player) : 6000;
        inv.setItem(28, createShopItem(Material.AMETHYST_SHARD,
                "📈 稳定值强化", NamedTextColor.LIGHT_PURPLE,
                "价格: 1000信用点",
                "增加750点稳定值",
                "当前: " + currentStability + "/10000",
                "稳定值影响伤害浮动",
                "信用点: " + credits));

        fillGlass(inv);
        player.openInventory(inv);
    }

    private ItemStack createShopItem(Material material, String name, NamedTextColor color, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.BOLD, true));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(Component.text(line, NamedTextColor.GRAY));
        }
        meta.lore(lore);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMainWeaponItem(String heroId, String heroName, NamedTextColor color,
                                           String weaponName, int price, Set<String> owned,
                                           String equipped, int credits) {
        boolean has = owned.contains(heroId);
        boolean isEquipped = equipped.equals(heroId);

        List<String> lore = new ArrayList<>();
        lore.add("价格: " + price + "信用点");
        lore.add("武器: " + weaponName);
        if (has) {
            lore.add(isEquipped ? "✅ 当前装备中" : "点击装备");
        } else {
            lore.add("点击购买");
        }
        lore.add("信用点: " + credits);

        Material mat;
        switch (heroId) {
            case "al1s":
            case "kayi":
                mat = Material.DIAMOND_HOE;
                break;
            case "kuroko":
            case "mika":
                mat = Material.IRON_HOE;
                break;
            case "izumi":
                mat = Material.NETHERITE_SHOVEL;
                break;
            case "yuuka":
                mat = Material.IRON_SHOVEL;
                break;
            case "serina":
            case "rakufu":
                mat = Material.IRON_HOE;
                break;
            default:
                mat = Material.DIAMOND_HOE;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("⚔ " + weaponName, color).decoration(TextDecoration.BOLD, true));
        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(Component.text(line, NamedTextColor.GRAY));
        }
        meta.lore(loreComponents);
        meta.setUnbreakable(true);
        if (isEquipped) {
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private void fillGlass(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        gm.displayName(Component.text(" ", NamedTextColor.DARK_GRAY));
        gm.setUnbreakable(true);
        glass.setItemMeta(gm);

        Set<Integer> used = new HashSet<>(Arrays.asList(
                0, 1, 2, 3, 4,
                9, 10, 11, 12, 13, 14, 15, 16,
                18, 19, 20, 21,
                27, 28
        ));
        for (int i = 0; i < 54; i++) {
            if (!used.contains(i)) inv.setItem(i, glass);
        }
    }

    // ============================================================
    // 商店点击处理
    // ============================================================
    public void handleShopClick(Player player, ItemStack clicked, String displayName) {
        if (displayName.contains("制式手枪")) {
            buyPistol(player);
        } else if (displayName.contains("灵异")) {
            buySpiritGun(player);
        } else if (displayName.contains("裁决")) {
            buyVerdict(player);
        } else if (displayName.contains("怒焰")) {
            buyRage(player);
        } else if (displayName.contains("手炮")) {
            buyHandCannon(player);
        } else if (displayName.contains("AL1S") || displayName.contains("超新星")) {
            HeroKit playerKit = gameManager.getPlayerHero(player);
            if (playerKit != null && playerKit.getId().equals("hoshino_defense")) {
                player.sendMessage(Component.text("❌ 防御形态星野无法购买超新星·光之剑！", NamedTextColor.RED));
                return;
            }
            handleMainWeaponPurchase(player, "al1s");
        } else if (displayName.contains("黑子")) {
            handleMainWeaponPurchase(player, "kuroko");
        } else if (displayName.contains("凯伊")) {
            handleMainWeaponPurchase(player, "kayi");
        } else if (displayName.contains("泉")) {
            handleMainWeaponPurchase(player, "izumi");
        } else if (displayName.contains("未花")) {
            handleMainWeaponPurchase(player, "mika");
        } else if (displayName.contains("优香")) {
            handleMainWeaponPurchase(player, "yuuka");
        } else if (displayName.contains("芹奈") || displayName.contains("神名")) {
            handleMainWeaponPurchase(player, "serina");
        } else if (displayName.contains("拉洛芙") || displayName.contains("破晓")) {
            handleMainWeaponPurchase(player, "rakufu");
        } else if (displayName.contains("防弹衣+头盔套装")) {
            buyFullArmor(player);
        } else if (displayName.contains("防弹衣")) {
            buyBodyArmor(player);
        } else if (displayName.contains("回复甲")) {
            buyRegenArmor(player);
        } else if (displayName.contains("重甲")) {
            buyHeavyArmor(player);
        } else if (displayName.contains("伤害减免强化")) {
            buyDamageReduction(player);
        } else if (displayName.contains("稳定值强化")) {
            buyStabilityUpgrade(player);
        }
    }

    // ============================================================
    // 副武器购买
    // ============================================================
    private void buyPistol(Player player) {
        UUID uuid = player.getUniqueId();
        if (hasPistol.getOrDefault(uuid, false)) {
            player.sendMessage(Component.text("❌ 已拥有制式手枪！", NamedTextColor.RED));
            return;
        }
        if (hasSpiritGun.getOrDefault(uuid, false) || hasVerdict.getOrDefault(uuid, false) ||
                hasRage.getOrDefault(uuid, false) || hasHandCannon.getOrDefault(uuid, false)) {
            player.sendMessage(Component.text("❌ 你已拥有其他副武器！", NamedTextColor.RED));
            return;
        }
        if (!creditManager.spendCredits(uuid, PISTOL_PRICE)) {
            player.sendMessage(Component.text("❌ 信用点不足！", NamedTextColor.RED));
            return;
        }
        hasPistol.put(uuid, true);
        pistolAmmo.put(uuid, PISTOL_MAX_AMMO);
        pistolReserve.put(uuid, PISTOL_INITIAL_RESERVE);
        givePistol(player);
        player.sendMessage(Component.text("✅ 购买成功！", NamedTextColor.GREEN));
        openShop(player);
    }

    private void buySpiritGun(Player player) {
        UUID uuid = player.getUniqueId();
        if (hasSpiritGun.getOrDefault(uuid, false)) {
            player.sendMessage(Component.text("❌ 已拥有灵异！", NamedTextColor.RED));
            return;
        }
        if (hasPistol.getOrDefault(uuid, false) || hasVerdict.getOrDefault(uuid, false) ||
                hasRage.getOrDefault(uuid, false) || hasHandCannon.getOrDefault(uuid, false)) {
            player.sendMessage(Component.text("❌ 你已拥有其他副武器！", NamedTextColor.RED));
            return;
        }
        if (!creditManager.spendCredits(uuid, SPIRIT_GUN_PRICE)) {
            player.sendMessage(Component.text("❌ 信用点不足！", NamedTextColor.RED));
            return;
        }
        hasSpiritGun.put(uuid, true);
        spiritGunAmmo.put(uuid, SPIRIT_GUN_MAX_AMMO);
        spiritGunReserve.put(uuid, SPIRIT_GUN_INITIAL_RESERVE);
        giveSpiritGun(player);
        player.sendMessage(Component.text("✅ 购买成功！", NamedTextColor.GREEN));
        openShop(player);
    }

    private void buyVerdict(Player player) {
        UUID uuid = player.getUniqueId();
        if (hasVerdict.getOrDefault(uuid, false)) {
            player.sendMessage(Component.text("❌ 已拥有裁决！", NamedTextColor.RED));
            return;
        }
        if (hasPistol.getOrDefault(uuid, false) || hasSpiritGun.getOrDefault(uuid, false) ||
                hasRage.getOrDefault(uuid, false) || hasHandCannon.getOrDefault(uuid, false)) {
            player.sendMessage(Component.text("❌ 你已拥有其他副武器！", NamedTextColor.RED));
            return;
        }
        if (!creditManager.spendCredits(uuid, VERDICT_PRICE)) {
            player.sendMessage(Component.text("❌ 信用点不足！", NamedTextColor.RED));
            return;
        }
        hasVerdict.put(uuid, true);
        verdictAmmo.put(uuid, VERDICT_MAX_AMMO);
        verdictReserve.put(uuid, VERDICT_INITIAL_RESERVE);
        giveVerdict(player);
        player.sendMessage(Component.text("✅ 购买成功！", NamedTextColor.GREEN));
        openShop(player);
    }

    private void buyRage(Player player) {
        UUID uuid = player.getUniqueId();
        if (hasRage.getOrDefault(uuid, false)) {
            player.sendMessage(Component.text("❌ 已拥有怒焰！", NamedTextColor.RED));
            return;
        }
        if (hasPistol.getOrDefault(uuid, false) || hasSpiritGun.getOrDefault(uuid, false) ||
                hasVerdict.getOrDefault(uuid, false) || hasHandCannon.getOrDefault(uuid, false)) {
            player.sendMessage(Component.text("❌ 你已拥有其他副武器！", NamedTextColor.RED));
            return;
        }
        if (!creditManager.spendCredits(uuid, WeaponConfig.RAGE_PRICE)) {
            player.sendMessage(Component.text("❌ 信用点不足！", NamedTextColor.RED));
            return;
        }
        hasRage.put(uuid, true);
        rageAmmo.put(uuid, WeaponConfig.RAGE_MAX_AMMO);
        rageReserve.put(uuid, WeaponConfig.RAGE_INITIAL_RESERVE);
        giveRage(player);
        player.sendMessage(Component.text("✅ 购买成功！", NamedTextColor.GREEN));
        openShop(player);
    }

    private void buyHandCannon(Player player) {
        UUID uuid = player.getUniqueId();
        if (hasHandCannon.getOrDefault(uuid, false)) {
            player.sendMessage(Component.text("❌ 已拥有手炮！", NamedTextColor.RED));
            return;
        }
        if (hasPistol.getOrDefault(uuid, false) || hasSpiritGun.getOrDefault(uuid, false) ||
                hasVerdict.getOrDefault(uuid, false) || hasRage.getOrDefault(uuid, false)) {
            player.sendMessage(Component.text("❌ 你已拥有其他副武器！", NamedTextColor.RED));
            return;
        }
        if (!creditManager.spendCredits(uuid, WeaponConfig.HANDCANNON_PRICE)) {
            player.sendMessage(Component.text("❌ 信用点不足！", NamedTextColor.RED));
            return;
        }
        hasHandCannon.put(uuid, true);
        handCannonAmmo.put(uuid, WeaponConfig.HANDCANNON_MAX_AMMO);
        handCannonReserve.put(uuid, WeaponConfig.HANDCANNON_INITIAL_RESERVE);
        giveHandCannon(player);
        player.sendMessage(Component.text("✅ 购买成功！", NamedTextColor.GREEN));
        openShop(player);
    }

    private void givePistol(Player player) {
        ItemStack item = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("制式手枪", NamedTextColor.GRAY));
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        plugin.getWeaponDataManager().writeAmmo(item, PISTOL_MAX_AMMO, PISTOL_INITIAL_RESERVE);
        player.getInventory().setItem(1, item);
    }

    private void giveSpiritGun(Player player) {
        ItemStack item = new ItemStack(Material.IRON_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("灵异", NamedTextColor.DARK_PURPLE));
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        plugin.getWeaponDataManager().writeAmmo(item, SPIRIT_GUN_MAX_AMMO, SPIRIT_GUN_INITIAL_RESERVE);
        player.getInventory().setItem(1, item);
    }

    private void giveVerdict(Player player) {
        ItemStack item = new ItemStack(Material.IRON_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("裁决", NamedTextColor.GOLD));
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        plugin.getWeaponDataManager().writeAmmo(item, VERDICT_MAX_AMMO, VERDICT_INITIAL_RESERVE);
        player.getInventory().setItem(1, item);
    }

    private void giveRage(Player player) {
        ItemStack item = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("怒焰", NamedTextColor.GOLD));
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        plugin.getWeaponDataManager().writeAmmo(item, WeaponConfig.RAGE_MAX_AMMO, WeaponConfig.RAGE_INITIAL_RESERVE);
        player.getInventory().setItem(1, item);
    }

    private void giveHandCannon(Player player) {
        ItemStack item = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("手炮", NamedTextColor.RED));
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        plugin.getWeaponDataManager().writeAmmo(item, WeaponConfig.HANDCANNON_MAX_AMMO, WeaponConfig.HANDCANNON_INITIAL_RESERVE);
        player.getInventory().setItem(1, item);
    }

    // ============================================================
    // 主武器购买
    // ============================================================
    private void handleMainWeaponPurchase(Player player, String heroId) {
        UUID uuid = player.getUniqueId();

        if (heroId.equals("al1s")) {
            HeroKit playerKit = gameManager.getPlayerHero(player);
            if (playerKit != null && playerKit.getId().equals("hoshino_defense")) {
                player.sendMessage(Component.text("❌ 防御形态星野无法购买超新星·光之剑！", NamedTextColor.RED));
                return;
            }
        }

        Set<String> owned = ownedMainWeapons.computeIfAbsent(uuid, k -> new HashSet<>());

        if (owned.contains(heroId)) {
            equipMainWeapon(player, heroId);
        } else {
            int price = MAIN_WEAPON_PRICES.getOrDefault(heroId, 9999);
            if (!creditManager.spendCredits(uuid, price)) {
                player.sendMessage(Component.text("❌ 信用点不足！需要 " + price + " 信用点", NamedTextColor.RED));
                return;
            }
            owned.add(heroId);
            equipMainWeapon(player, heroId);
            player.sendMessage(Component.text("✅ 购买并装备成功！", NamedTextColor.GREEN));
            openShop(player);
        }
    }

    private void equipMainWeapon(Player player, String heroId) {
        UUID uuid = player.getUniqueId();
        equippedMainWeapon.put(uuid, heroId);

        HeroKit kit = getHeroKitById(heroId);
        if (kit == null) return;
        if (kit.getWeaponMaterial() == Material.AIR) return;

        ItemStack weapon = createMainWeaponItemStack(kit);
        player.getInventory().setItem(0, weapon);

        gameManager.getAmmoManager().resetRound(uuid, kit.getMagazineSize(), kit.getInitialReserveAmmo());

        player.sendMessage(Component.text("⚔ 已装备 " + kit.getWeaponName(), NamedTextColor.GREEN));
    }

    private HeroKit getHeroKitById(String id) {
        for (HeroKit kit : GameManager.HERO_KITS) {
            if (kit.getId().equals(id)) return kit;
        }
        return null;
    }

    private ItemStack createMainWeaponItemStack(HeroKit kit) {
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
                    Component.text("可丢弃，不可移动", NamedTextColor.DARK_GRAY)
            ));
        } else if (kit.getId().equals("rakufu")) {
            meta.displayName(Component.text("破晓", NamedTextColor.LIGHT_PURPLE));
            meta.lore(List.of(
                    Component.text("伤害: 头52/胸22/四肢18", NamedTextColor.RED),
                    Component.text("射速: 8.9发/秒", NamedTextColor.YELLOW),
                    Component.text("弹夹: 50发 | 备用: 150发", NamedTextColor.YELLOW),
                    Component.text("穿透1个方块", NamedTextColor.GOLD),
                    Component.text("蹲下时伤害+30%，穿透75%", NamedTextColor.GRAY),
                    Component.text("可丢弃，不可移动", NamedTextColor.DARK_GRAY)
            ));
        } else if (kit.getId().equals("al1s")) {
            meta.displayName(Component.text("超新星·光之剑", NamedTextColor.AQUA));
            meta.lore(List.of(
                    Component.text("伤害: 头400/胸275/四肢250", NamedTextColor.RED),
                    Component.text("弹夹: 5发 | 备用: 10发", NamedTextColor.YELLOW),
                    Component.text("蹲下蓄力：最多+30%伤害", NamedTextColor.GOLD),
                    Component.text("无视30%护甲减伤", NamedTextColor.LIGHT_PURPLE),
                    Component.text("可丢弃，不可移动", NamedTextColor.DARK_GRAY)
            ));
        } else {
            meta.displayName(Component.text(kit.getWeaponName(), color));
            meta.lore(List.of(
                    Component.text("伤害: " + kit.getLaserDamage(), NamedTextColor.RED),
                    Component.text("弹夹: " + kit.getMagazineSize() + "发 | 备用: " + kit.getInitialReserveAmmo() + "发", NamedTextColor.YELLOW),
                    Component.text("可丢弃，不可移动", NamedTextColor.DARK_GRAY)
            ));
        }

        meta.setUnbreakable(true);
        weapon.setItemMeta(meta);
        return weapon;
    }

    // ============================================================
    // 防具购买
    // ============================================================
    private void buyBodyArmor(Player player) {
        UUID uuid = player.getUniqueId();
        if (hasBodyArmor.getOrDefault(uuid, false) && bodyArmorDurability.getOrDefault(uuid, 0) >= BODY_ARMOR_MAX) {
            player.sendMessage(Component.text("❌ 防弹衣耐久已满！", NamedTextColor.RED));
            return;
        }
        if (!creditManager.spendCredits(uuid, BODY_ARMOR_PRICE)) {
            player.sendMessage(Component.text("❌ 信用点不足！", NamedTextColor.RED));
            return;
        }
        hasBodyArmor.put(uuid, true);
        bodyArmorDurability.put(uuid, BODY_ARMOR_MAX);
        giveBodyArmor(player);
        player.sendMessage(Component.text("✅ 购买成功！", NamedTextColor.GREEN));
        openShop(player);
    }

    private void buyFullArmor(Player player) {
        UUID uuid = player.getUniqueId();
        int price = 800;
        if (!creditManager.spendCredits(uuid, price)) {
            player.sendMessage(Component.text("❌ 信用点不足！", NamedTextColor.RED));
            return;
        }
        hasBodyArmor.put(uuid, true);
        hasHelmet.put(uuid, true);
        bodyArmorDurability.put(uuid, BODY_ARMOR_MAX);
        helmetDurability.put(uuid, HELMET_MAX);
        giveBodyArmor(player);
        giveHelmet(player);
        player.sendMessage(Component.text("✅ 购买成功！", NamedTextColor.GREEN));
        openShop(player);
    }

    private void buyRegenArmor(Player player) {
        UUID uuid = player.getUniqueId();

        if (hasRegenArmor.getOrDefault(uuid, false)) {
            int currentPool = regenPool.getOrDefault(uuid, 0);
            if (currentPool >= REGEN_POOL_MAX) {
                player.sendMessage(Component.text("❌ 回复池已满！", NamedTextColor.RED));
                return;
            }
            if (!creditManager.spendCredits(uuid, REGEN_ARMOR_PRICE / 2)) {
                player.sendMessage(Component.text("❌ 信用点不足！补充需要 " + (REGEN_ARMOR_PRICE / 2) + " 信用点", NamedTextColor.RED));
                return;
            }
            regenPool.put(uuid, REGEN_POOL_MAX);
            gameManager.setPlayerCurrentArmor(player, REGEN_ARMOR_MAX);
            player.sendMessage(Component.text("✅ 回复池已补充至 " + REGEN_POOL_MAX + "！", NamedTextColor.GREEN));
            openShop(player);
            return;
        }

        if (!creditManager.spendCredits(uuid, REGEN_ARMOR_PRICE)) {
            player.sendMessage(Component.text("❌ 信用点不足！", NamedTextColor.RED));
            return;
        }
        hasRegenArmor.put(uuid, true);
        regenPool.put(uuid, REGEN_POOL_MAX);
        gameManager.setPlayerCurrentArmor(player, REGEN_ARMOR_MAX);
        player.sendMessage(Component.text("✅ 回复甲购买成功！护甲上限 " + REGEN_ARMOR_MAX + "，回复池 " + REGEN_POOL_MAX, NamedTextColor.GREEN));
        openShop(player);
    }

    private void buyHeavyArmor(Player player) {
        UUID uuid = player.getUniqueId();

        if (hasHeavyArmor.getOrDefault(uuid, false)) {
            int currentArmor = gameManager.getPlayerCurrentArmor(player);
            if (currentArmor >= HEAVY_ARMOR_MAX) {
                player.sendMessage(Component.text("❌ 重甲护甲已满！", NamedTextColor.RED));
                return;
            }
            if (!creditManager.spendCredits(uuid, HEAVY_ARMOR_PRICE / 2)) {
                player.sendMessage(Component.text("❌ 信用点不足！补充需要 " + (HEAVY_ARMOR_PRICE / 2) + " 信用点", NamedTextColor.RED));
                return;
            }
            gameManager.setPlayerCurrentArmor(player, HEAVY_ARMOR_MAX);
            player.sendMessage(Component.text("✅ 重甲护甲已补充至 " + HEAVY_ARMOR_MAX + "！", NamedTextColor.GREEN));
            openShop(player);
            return;
        }

        if (!creditManager.spendCredits(uuid, HEAVY_ARMOR_PRICE)) {
            player.sendMessage(Component.text("❌ 信用点不足！", NamedTextColor.RED));
            return;
        }
        hasHeavyArmor.put(uuid, true);
        gameManager.setPlayerCurrentArmor(player, HEAVY_ARMOR_MAX);
        player.sendMessage(Component.text("✅ 重甲购买成功！护甲上限 " + HEAVY_ARMOR_MAX + "，吸收 " + (int)(HEAVY_ARMOR_ABSORPTION * 100) + "% 伤害", NamedTextColor.GREEN));
        openShop(player);
    }

    private void buyDamageReduction(Player player) {
        UUID uuid = player.getUniqueId();
        int current = damageReductionCount.getOrDefault(uuid, 0);
        if (current >= DAMAGE_REDUCTION_MAX) {
            player.sendMessage(Component.text("❌ 已达上限！", NamedTextColor.RED));
            return;
        }
        if (!creditManager.spendCredits(uuid, DAMAGE_REDUCTION_PRICE)) {
            player.sendMessage(Component.text("❌ 信用点不足！", NamedTextColor.RED));
            return;
        }
        damageReductionCount.put(uuid, current + 1);
        player.sendMessage(Component.text("✅ 强化成功！当前减免 " + (current + 1) * 0.95 + "%", NamedTextColor.GREEN));
        openShop(player);
    }

    private void buyStabilityUpgrade(Player player) {
        UUID uuid = player.getUniqueId();
        StabilityManager sm = BACS.getInstance().getStabilityManager();
        if (sm == null) {
            player.sendMessage(Component.text("❌ 稳定值系统未初始化！", NamedTextColor.RED));
            return;
        }
        int current = sm.getStability(player);
        if (current >= 10000) {
            player.sendMessage(Component.text("❌ 稳定值已达上限！", NamedTextColor.RED));
            return;
        }
        if (!creditManager.spendCredits(uuid, 1000)) {
            player.sendMessage(Component.text("❌ 信用点不足！需要1000信用点", NamedTextColor.RED));
            return;
        }
        sm.addStability(player, 750);
        player.sendMessage(Component.text("✅ 稳定值已增加750！当前: " + sm.getStability(player) + "/10000", NamedTextColor.GREEN));
        openShop(player);
    }

    private void giveBodyArmor(Player player) {
        String team = gameManager.getPlayerTeam(player);
        Material mat = team != null && team.equals(GameManager.TEAM_ATTACKER)
                ? Material.CHAINMAIL_CHESTPLATE : Material.IRON_CHESTPLATE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("防弹衣", NamedTextColor.AQUA));
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        player.getInventory().setChestplate(item);
    }

    private void giveHelmet(Player player) {
        String team = gameManager.getPlayerTeam(player);
        Material mat = team != null && team.equals(GameManager.TEAM_ATTACKER)
                ? Material.CHAINMAIL_HELMET : Material.IRON_HELMET;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("防弹头盔", NamedTextColor.AQUA));
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        player.getInventory().setHelmet(item);
    }

    // ============================================================
    // 状态查询
    // ============================================================
    public boolean hasPistol(Player player) { return hasPistol.getOrDefault(player.getUniqueId(), false); }
    public boolean hasSpiritGun(Player player) { return hasSpiritGun.getOrDefault(player.getUniqueId(), false); }
    public boolean hasVerdict(Player player) { return hasVerdict.getOrDefault(player.getUniqueId(), false); }
    public boolean hasRage(Player player) { return hasRage.getOrDefault(player.getUniqueId(), false); }
    public boolean hasHandCannon(Player player) { return hasHandCannon.getOrDefault(player.getUniqueId(), false); }

    public int getPistolAmmo(Player player) { return pistolAmmo.getOrDefault(player.getUniqueId(), 0); }
    public void setPistolAmmo(Player player, int ammo) { pistolAmmo.put(player.getUniqueId(), Math.max(0, ammo)); }
    public int getPistolReserve(Player player) { return pistolReserve.getOrDefault(player.getUniqueId(), 0); }
    public void setPistolReserve(Player player, int reserve) { pistolReserve.put(player.getUniqueId(), Math.max(0, reserve)); }

    public int getSpiritGunAmmo(Player player) { return spiritGunAmmo.getOrDefault(player.getUniqueId(), 0); }
    public void setSpiritGunAmmo(Player player, int ammo) { spiritGunAmmo.put(player.getUniqueId(), Math.max(0, ammo)); }
    public int getSpiritGunReserve(Player player) { return spiritGunReserve.getOrDefault(player.getUniqueId(), 0); }
    public void setSpiritGunReserve(Player player, int reserve) { spiritGunReserve.put(player.getUniqueId(), Math.max(0, reserve)); }

    public int getVerdictAmmo(Player player) { return verdictAmmo.getOrDefault(player.getUniqueId(), 0); }
    public void setVerdictAmmo(Player player, int ammo) { verdictAmmo.put(player.getUniqueId(), Math.max(0, ammo)); }
    public int getVerdictReserve(Player player) { return verdictReserve.getOrDefault(player.getUniqueId(), 0); }
    public void setVerdictReserve(Player player, int reserve) { verdictReserve.put(player.getUniqueId(), Math.max(0, reserve)); }

    public int getRageAmmo(Player player) { return rageAmmo.getOrDefault(player.getUniqueId(), 0); }
    public void setRageAmmo(Player player, int ammo) { rageAmmo.put(player.getUniqueId(), Math.max(0, ammo)); }
    public int getRageReserve(Player player) { return rageReserve.getOrDefault(player.getUniqueId(), 0); }
    public void setRageReserve(Player player, int reserve) { rageReserve.put(player.getUniqueId(), Math.max(0, reserve)); }

    public int getHandCannonAmmo(Player player) { return handCannonAmmo.getOrDefault(player.getUniqueId(), 0); }
    public void setHandCannonAmmo(Player player, int ammo) { handCannonAmmo.put(player.getUniqueId(), Math.max(0, ammo)); }
    public int getHandCannonReserve(Player player) { return handCannonReserve.getOrDefault(player.getUniqueId(), 0); }
    public void setHandCannonReserve(Player player, int reserve) { handCannonReserve.put(player.getUniqueId(), Math.max(0, reserve)); }

    public boolean canShootSidearm(UUID uuid, long interval) {
        long last = sidearmLastShot.getOrDefault(uuid, 0L);
        return System.currentTimeMillis() - last >= interval;
    }
    public void setLastSidearmShot(UUID uuid, long time) { sidearmLastShot.put(uuid, time); }

    // ============================================================
    // 防具状态
    // ============================================================
    public boolean hasBodyArmor(Player player) { return hasBodyArmor.getOrDefault(player.getUniqueId(), false); }
    public boolean hasHelmet(Player player) { return hasHelmet.getOrDefault(player.getUniqueId(), false); }
    public int getBodyArmorDurability(Player player) { return bodyArmorDurability.getOrDefault(player.getUniqueId(), 0); }
    public int getHelmetDurability(Player player) { return helmetDurability.getOrDefault(player.getUniqueId(), 0); }
    public void setBodyArmorDurability(Player player, int d) {
        UUID uuid = player.getUniqueId();
        bodyArmorDurability.put(uuid, Math.max(0, d));
        if (d <= 0) {
            hasBodyArmor.put(uuid, false);
            player.getInventory().setChestplate(null);
        }
    }
    public void setHelmetDurability(Player player, int d) {
        UUID uuid = player.getUniqueId();
        helmetDurability.put(uuid, Math.max(0, d));
        if (d <= 0) {
            hasHelmet.put(uuid, false);
            player.getInventory().setHelmet(null);
        }
    }

    public boolean hasRegenArmor(Player player) { return hasRegenArmor.getOrDefault(player.getUniqueId(), false); }
    public int getRegenPool(Player player) { return regenPool.getOrDefault(player.getUniqueId(), 0); }
    public void setRegenPool(Player player, int pool) { regenPool.put(player.getUniqueId(), Math.min(REGEN_POOL_MAX, Math.max(0, pool))); }
    public int getRegenArmorMax() { return REGEN_ARMOR_MAX; }

    public boolean hasHeavyArmor(Player player) { return hasHeavyArmor.getOrDefault(player.getUniqueId(), false); }
    public int getHeavyArmorMax() { return HEAVY_ARMOR_MAX; }
    public double getHeavyArmorAbsorption() { return HEAVY_ARMOR_ABSORPTION; }

    public int getDamageReductionCount(Player player) { return damageReductionCount.getOrDefault(player.getUniqueId(), 0); }

    // ============================================================
    // 主武器状态
    // ============================================================
    public boolean hasMainWeapon(Player player, String heroId) {
        return ownedMainWeapons.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(heroId);
    }

    public String getEquippedMainWeapon(Player player) {
        return equippedMainWeapon.getOrDefault(player.getUniqueId(), null);
    }

    public void setEquippedMainWeapon(Player player, String heroId) {
        if (heroId == null) {
            equippedMainWeapon.remove(player.getUniqueId());
        } else {
            equippedMainWeapon.put(player.getUniqueId(), heroId);
        }
    }

    public Set<String> getOwnedMainWeapons(Player player) {
        return new HashSet<>(ownedMainWeapons.getOrDefault(player.getUniqueId(), new HashSet<>()));
    }

    // ============================================================
    // 受伤/死亡处理
    // ============================================================
    public void onPlayerDamaged(Player player) {
        lastDamageTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void onPlayerDeath(Player player) {
        UUID uuid = player.getUniqueId();
        WeaponDataManager wdm = plugin.getWeaponDataManager();

        // 主武器掉落
        String equippedId = equippedMainWeapon.get(uuid);
        if (equippedId != null) {
            HeroKit kit = getHeroKitById(equippedId);
            if (kit != null && kit.getWeaponMaterial() != Material.AIR) {
                ItemStack weapon = createMainWeaponItemStack(kit);
                int ammo = gameManager.getAmmoManager().getMagazineAmmo(uuid);
                int reserve = gameManager.getAmmoManager().getReserveAmmo(uuid);
                wdm.writeAmmo(weapon, ammo, reserve);
                player.getWorld().dropItemNaturally(player.getLocation(), weapon);
                player.getInventory().setItem(0, null);
                equippedMainWeapon.remove(uuid);
            }
        }

        // 副武器掉落
        if (hasPistol.getOrDefault(uuid, false)) {
            ItemStack sec = player.getInventory().getItem(1);
            if (sec != null && sec.getType() != Material.AIR) {
                wdm.writeAmmo(sec, getPistolAmmo(player), getPistolReserve(player));
                player.getWorld().dropItemNaturally(player.getLocation(), sec);
                player.getInventory().setItem(1, null);
            }
            hasPistol.put(uuid, false);
            pistolAmmo.remove(uuid);
            pistolReserve.remove(uuid);
        }
        if (hasSpiritGun.getOrDefault(uuid, false)) {
            ItemStack sec = player.getInventory().getItem(1);
            if (sec != null && sec.getType() != Material.AIR) {
                wdm.writeAmmo(sec, getSpiritGunAmmo(player), getSpiritGunReserve(player));
                player.getWorld().dropItemNaturally(player.getLocation(), sec);
                player.getInventory().setItem(1, null);
            }
            hasSpiritGun.put(uuid, false);
            spiritGunAmmo.remove(uuid);
            spiritGunReserve.remove(uuid);
        }
        if (hasVerdict.getOrDefault(uuid, false)) {
            ItemStack sec = player.getInventory().getItem(1);
            if (sec != null && sec.getType() != Material.AIR) {
                wdm.writeAmmo(sec, getVerdictAmmo(player), getVerdictReserve(player));
                player.getWorld().dropItemNaturally(player.getLocation(), sec);
                player.getInventory().setItem(1, null);
            }
            hasVerdict.put(uuid, false);
            verdictAmmo.remove(uuid);
            verdictReserve.remove(uuid);
        }
        if (hasRage.getOrDefault(uuid, false)) {
            ItemStack sec = player.getInventory().getItem(1);
            if (sec != null && sec.getType() == Material.GOLDEN_AXE) {
                wdm.writeAmmo(sec, getRageAmmo(player), getRageReserve(player));
                player.getWorld().dropItemNaturally(player.getLocation(), sec);
                player.getInventory().setItem(1, null);
            }
            hasRage.put(uuid, false);
            rageAmmo.remove(uuid);
            rageReserve.remove(uuid);
        }
        if (hasHandCannon.getOrDefault(uuid, false)) {
            ItemStack sec = player.getInventory().getItem(1);
            if (sec != null && sec.getType() == Material.NETHERITE_AXE) {
                wdm.writeAmmo(sec, getHandCannonAmmo(player), getHandCannonReserve(player));
                player.getWorld().dropItemNaturally(player.getLocation(), sec);
                player.getInventory().setItem(1, null);
            }
            hasHandCannon.put(uuid, false);
            handCannonAmmo.remove(uuid);
            handCannonReserve.remove(uuid);
        }

        // 防弹衣掉落
        if (hasBodyArmor.getOrDefault(uuid, false)) {
            hasBodyArmor.put(uuid, false);
            bodyArmorDurability.remove(uuid);
            player.getInventory().setChestplate(null);
        }
        if (hasHelmet.getOrDefault(uuid, false)) {
            hasHelmet.put(uuid, false);
            helmetDurability.remove(uuid);
            player.getInventory().setHelmet(null);
        }

        gameManager.setPlayerCurrentArmor(player, 0);
    }

    // ============================================================
    // 回复甲护甲恢复
    // ============================================================
    public void tickArmorRegen(Player player) {
        UUID uuid = player.getUniqueId();

        if (!hasRegenArmor.getOrDefault(uuid, false)) return;

        int pool = regenPool.getOrDefault(uuid, 0);
        if (pool <= 0) return;

        int currentArmor = gameManager.getPlayerCurrentArmor(player);
        if (currentArmor >= REGEN_ARMOR_MAX) return;

        long lastDamage = lastDamageTime.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastDamage < 3300) return;

        int need = REGEN_ARMOR_MAX - currentArmor;
        int add = Math.min(1, Math.min(need, pool));
        gameManager.setPlayerCurrentArmor(player, currentArmor + add);
        regenPool.put(uuid, pool - add);
    }

    // ============================================================
    // 重置
    // ============================================================
    public void resetRoundPurchases() {
        for (UUID uuid : hasPistol.keySet()) {
            if (hasPistol.getOrDefault(uuid, false)) {
                pistolAmmo.put(uuid, PISTOL_MAX_AMMO);
                pistolReserve.put(uuid, PISTOL_INITIAL_RESERVE);
            }
        }
        for (UUID uuid : hasSpiritGun.keySet()) {
            if (hasSpiritGun.getOrDefault(uuid, false)) {
                spiritGunAmmo.put(uuid, SPIRIT_GUN_MAX_AMMO);
                spiritGunReserve.put(uuid, SPIRIT_GUN_INITIAL_RESERVE);
            }
        }
        for (UUID uuid : hasVerdict.keySet()) {
            if (hasVerdict.getOrDefault(uuid, false)) {
                verdictAmmo.put(uuid, VERDICT_MAX_AMMO);
                verdictReserve.put(uuid, VERDICT_INITIAL_RESERVE);
            }
        }
        for (UUID uuid : hasRage.keySet()) {
            if (hasRage.getOrDefault(uuid, false)) {
                rageAmmo.put(uuid, WeaponConfig.RAGE_MAX_AMMO);
                rageReserve.put(uuid, WeaponConfig.RAGE_INITIAL_RESERVE);
            }
        }
        for (UUID uuid : hasHandCannon.keySet()) {
            if (hasHandCannon.getOrDefault(uuid, false)) {
                handCannonAmmo.put(uuid, WeaponConfig.HANDCANNON_MAX_AMMO);
                handCannonReserve.put(uuid, WeaponConfig.HANDCANNON_INITIAL_RESERVE);
            }
        }
    }

    public void resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        hasPistol.remove(uuid);
        pistolAmmo.remove(uuid);
        pistolReserve.remove(uuid);
        hasSpiritGun.remove(uuid);
        spiritGunAmmo.remove(uuid);
        spiritGunReserve.remove(uuid);
        hasVerdict.remove(uuid);
        verdictAmmo.remove(uuid);
        verdictReserve.remove(uuid);
        hasRage.remove(uuid);
        rageAmmo.remove(uuid);
        rageReserve.remove(uuid);
        hasHandCannon.remove(uuid);
        handCannonAmmo.remove(uuid);
        handCannonReserve.remove(uuid);
        sidearmLastShot.remove(uuid);
        hasBodyArmor.remove(uuid);
        hasHelmet.remove(uuid);
        bodyArmorDurability.remove(uuid);
        helmetDurability.remove(uuid);
        damageReductionCount.remove(uuid);
        ownedMainWeapons.remove(uuid);
        equippedMainWeapon.remove(uuid);
        hasRegenArmor.remove(uuid);
        regenPool.remove(uuid);
        hasHeavyArmor.remove(uuid);
        lastDamageTime.remove(uuid);
    }

    public void resetAll() {
        hasPistol.clear();
        pistolAmmo.clear();
        pistolReserve.clear();
        hasSpiritGun.clear();
        spiritGunAmmo.clear();
        spiritGunReserve.clear();
        hasVerdict.clear();
        verdictAmmo.clear();
        verdictReserve.clear();
        hasRage.clear();
        rageAmmo.clear();
        rageReserve.clear();
        hasHandCannon.clear();
        handCannonAmmo.clear();
        handCannonReserve.clear();
        sidearmLastShot.clear();
        hasBodyArmor.clear();
        hasHelmet.clear();
        bodyArmorDurability.clear();
        helmetDurability.clear();
        damageReductionCount.clear();
        ownedMainWeapons.clear();
        equippedMainWeapon.clear();
        hasRegenArmor.clear();
        regenPool.clear();
        hasHeavyArmor.clear();
        lastDamageTime.clear();
    }

    // ============================================================
    // 兼容旧API
    // ============================================================
    public void setHasPistol(Player player, boolean b) { hasPistol.put(player.getUniqueId(), b); }
    public void setHasSpiritGun(Player player, boolean b) { hasSpiritGun.put(player.getUniqueId(), b); }
    public void setHasVerdict(Player player, boolean b) { hasVerdict.put(player.getUniqueId(), b); }
    public void setHasRage(Player player, boolean b) { hasRage.put(player.getUniqueId(), b); }
    public void setHasHandCannon(Player player, boolean b) { hasHandCannon.put(player.getUniqueId(), b); }
}