package com.bACS;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Random;

public class BACS extends JavaPlugin {
    private static BACS instance;
    private GameManager gameManager;
    private World voidWorld;
    private KayiSkillListener kayiSkillListener;
    private IzumiSkillListener izumiSkillListener;
    private MikaSkillListener mikaSkillListener;
    private YuukaSkillListener yuukaSkillListener;
    private CoinManager coinManager;
    private SerinaSkillListener serinaSkillListener;
    private XYAntiCheat xyAntiCheat;
    private StabilityManager stabilityManager;
    private NetworkManager networkManager;
    private RakuFuSkillListener rakuFuSkillListener;
    private ShieldManager shieldManager;
    private HoshinoSkillListener hoshinoSkillListener;
    private WeaponDataManager weaponDataManager;
    private SidearmShootingSystem sidearmShootingSystem;
    private VoteManager voteManager;              // ★ 新增

    @Override
    public void onEnable() {
        instance = this;
        gameManager = new GameManager(this);
        stabilityManager = new StabilityManager();
        networkManager = new NetworkManager(this);
        weaponDataManager = new WeaponDataManager(this);
        sidearmShootingSystem = new SidearmShootingSystem(this, gameManager);

        kayiSkillListener = new KayiSkillListener(this, gameManager);
        izumiSkillListener = new IzumiSkillListener(this, gameManager);
        mikaSkillListener = new MikaSkillListener(this, gameManager);
        yuukaSkillListener = new YuukaSkillListener(this, gameManager);
        coinManager = new CoinManager(this, gameManager);
        serinaSkillListener = new SerinaSkillListener(this, gameManager);
        xyAntiCheat = new XYAntiCheat(this, gameManager);
        rakuFuSkillListener = new RakuFuSkillListener(this, gameManager);

        // 星野相关
        shieldManager = new ShieldManager();
        hoshinoSkillListener = new HoshinoSkillListener(this, gameManager, shieldManager);

        // ★ 投票管理器
        voteManager = new VoteManager(this, gameManager);

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new SoulFireListener(this, gameManager, gameManager.getAmmoManager()), this);
        getServer().getPluginManager().registerEvents(new BombListener(this, gameManager, gameManager.getBombManager()), this);
        getServer().getPluginManager().registerEvents(new SmokeGrenadeListener(this, gameManager), this);
        getServer().getPluginManager().registerEvents(new MeleeListener(this, gameManager), this);
        getServer().getPluginManager().registerEvents(kayiSkillListener, this);
        getServer().getPluginManager().registerEvents(izumiSkillListener, this);
        getServer().getPluginManager().registerEvents(mikaSkillListener, this);
        getServer().getPluginManager().registerEvents(yuukaSkillListener, this);
        getServer().getPluginManager().registerEvents(coinManager, this);
        getServer().getPluginManager().registerEvents(new BlockProtectListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryProtectListener(), this);
        getServer().getPluginManager().registerEvents(new SidearmListener(this, gameManager), this);
        getServer().getPluginManager().registerEvents(new HeroSelectListener(this), this);
        getServer().getPluginManager().registerEvents(xyAntiCheat, this);
        getServer().getPluginManager().registerEvents(serinaSkillListener, this);
        getServer().getPluginManager().registerEvents(rakuFuSkillListener, this);
        getServer().getPluginManager().registerEvents(hoshinoSkillListener, this);
        getServer().getPluginManager().registerEvents(new WeaponPickupListener(this, gameManager), this);
        getServer().getPluginManager().registerEvents(new ExSkillTriggerListener(this, gameManager), this);

        getCommand("forcegame").setExecutor(new ForceStartCommand(this));
        getCommand("resetgame").setExecutor(new ResetCommand(this));
        getCommand("voidworld").setExecutor(new WorldCommand(this));
        getCommand("coin").setExecutor(new CoinCommand(coinManager));
        getCommand("buykits").setExecutor(new BuyKitsCommand(this));
        getCommand("vote").setExecutor(new VoteCommand(this));   // ★ 新增

        createVoidWorld();
        setVoidWorldSpawn();

        getLogger().info("§a[BACS] 插件已启用！");
        getLogger().info("§e当前玩家数量: " + getServer().getOnlinePlayers().size());

        getServer().getScheduler().runTaskLater(this, () -> {
            gameManager.checkAndStartCountdown();
        }, 20L);

        getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            gameManager.checkForReset();
        }, 0L, 100L);
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.cancelCountdown();
            gameManager.stopHealthDisplay();
            if (gameManager.getBombManager() != null) {
                gameManager.getBombManager().cleanup();
            }
        }

        if (networkManager != null) {
            networkManager.cleanup();
        }

        if (voteManager != null) {
            voteManager.forceEnd();
        }

        if (mikaSkillListener != null) {
            mikaSkillListener.resetAll();
        }
        if (yuukaSkillListener != null) {
            yuukaSkillListener.resetAll();
        }
        if (coinManager != null) {
            coinManager.resetAll();
        }
        if (serinaSkillListener != null) {
            serinaSkillListener.resetAll();
        }
        if (rakuFuSkillListener != null) {
            rakuFuSkillListener.resetAll();
        }
        if (hoshinoSkillListener != null) {
            hoshinoSkillListener.resetAll();
        }
        if (shieldManager != null) {
            shieldManager.resetAll();
        }
        if (stabilityManager != null) {
            stabilityManager.resetAll();
        }
        if (sidearmShootingSystem != null) {
            sidearmShootingSystem.resetAll();
        }

        getLogger().info("§c[BACS] 插件已禁用！");
    }

    @SuppressWarnings("removal")
    private void createVoidWorld() {
        voidWorld = Bukkit.getWorld("voidworld");
        if (voidWorld == null) {
            getLogger().info("§e正在创建虚空世界 'voidworld'...");
            WorldCreator creator = new WorldCreator("voidworld");
            creator.generator(new VoidChunkGenerator());
            creator.generateStructures(false);
            voidWorld = creator.createWorld();
            if (voidWorld != null) {
                getLogger().info("§a✅ 虚空世界创建成功！");
                voidWorld.getWorldBorder().setSize(3200);
                voidWorld.getWorldBorder().setCenter(0, 0);
                voidWorld.setDifficulty(org.bukkit.Difficulty.PEACEFUL);
                voidWorld.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
                voidWorld.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
                voidWorld.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
            }
        } else {
            getLogger().info("§e虚空世界已存在！");
        }
    }

    private void setVoidWorldSpawn() {
        if (voidWorld == null) return;
        voidWorld.setSpawnLocation(new Location(voidWorld, 0, 101, 0));
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                new Location(voidWorld, x, 100, z).getBlock().setType(org.bukkit.Material.GLASS);
            }
        }
        new Location(voidWorld, 0, 101, 0).getBlock().setType(org.bukkit.Material.SEA_LANTERN);
    }

    public World getVoidWorld() { return voidWorld; }
    public static BACS getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
    public KayiSkillListener getKayiSkillListener() { return kayiSkillListener; }
    public IzumiSkillListener getIzumiSkillListener() { return izumiSkillListener; }
    public MikaSkillListener getMikaSkillListener() { return mikaSkillListener; }
    public YuukaSkillListener getYuukaSkillListener() { return yuukaSkillListener; }
    public CoinManager getCoinManager() { return coinManager; }
    public SerinaSkillListener getSerinaSkillListener() { return serinaSkillListener; }
    public XYAntiCheat getXYAntiCheat() { return xyAntiCheat; }
    public StabilityManager getStabilityManager() { return stabilityManager; }
    public NetworkManager getNetworkManager() { return networkManager; }
    public RakuFuSkillListener getRakuFuSkillListener() { return rakuFuSkillListener; }
    public ShieldManager getShieldManager() { return shieldManager; }
    public HoshinoSkillListener getHoshinoSkillListener() { return hoshinoSkillListener; }
    public WeaponDataManager getWeaponDataManager() { return weaponDataManager; }
    public SidearmShootingSystem getSidearmShootingSystem() { return sidearmShootingSystem; }
    public VoteManager getVoteManager() { return voteManager; }   // ★ 新增

    public static class VoidChunkGenerator extends ChunkGenerator {
        @Override
        public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
            return createChunkData(world);
        }
        @Override
        public boolean canSpawn(World world, int x, int z) { return true; }
        @Override
        public Location getFixedSpawnLocation(World world, Random random) {
            return new Location(world, 0, 101, 0);
        }
    }
}