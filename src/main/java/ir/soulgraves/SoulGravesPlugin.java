package ir.soulgraves;

import ir.soulgraves.commands.AdminCommand;
import ir.soulgraves.commands.GravesCommand;
import ir.soulgraves.config.ConfigManager;
import ir.soulgraves.economy.VaultHook;
import ir.soulgraves.grave.ExpiryTask;
import ir.soulgraves.grave.GraveManager;
import ir.soulgraves.grave.MarkerService;
import ir.soulgraves.grave.TeleportService;
import ir.soulgraves.gui.GraveContentsGUI;
import ir.soulgraves.gui.GraveListGUI;
import ir.soulgraves.integration.ItemsAdderHook;
import ir.soulgraves.listeners.DeathListener;
import ir.soulgraves.listeners.ItemsAdderEventsBridge;
import ir.soulgraves.listeners.MarkerInteractListener;
import ir.soulgraves.particles.SoulTrailTask;
import ir.soulgraves.storage.Database;
import ir.soulgraves.storage.GraveRepository;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoulGravesPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private Database database;
    private GraveRepository repository;
    private GraveManager graveManager;
    private MarkerService markerService;
    private TeleportService teleportService;
    private VaultHook vaultHook;
    private ItemsAdderHook itemsAdderHook;
    private GraveListGUI graveListGUI;
    private GraveContentsGUI graveContentsGUI;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        configManager.load();

        this.database = new Database(getDataFolder());
        try { database.open(); }
        catch (Exception e) { getLogger().severe("DB open failed: " + e.getMessage()); setEnabled(false); return; }

        this.repository = new GraveRepository(database);
        this.graveManager = new GraveManager(this, repository);
        graveManager.loadAll();

        this.markerService = new MarkerService(this);
        this.teleportService = new TeleportService(this);
        this.vaultHook = new VaultHook();
        if (!vaultHook.setup()) getLogger().warning("Vault not available — teleport cost disabled.");
        this.itemsAdderHook = new ItemsAdderHook();
        this.graveListGUI = new GraveListGUI(this);
        this.graveContentsGUI = new GraveContentsGUI(this);

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new MarkerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(graveListGUI, this);
        getServer().getPluginManager().registerEvents(graveContentsGUI, this);
        new ItemsAdderEventsBridge(this).register();

        getCommand("graves").setExecutor(new GravesCommand(this));
        getCommand("soulgraves").setExecutor(new AdminCommand(this));

        long expiryInterval = configManager.expiryInterval() * 20L;
        new ExpiryTask(this).runTaskTimer(this, 20L * 30, expiryInterval);

        int trailTicks = getConfig().getInt("soul_trail.update_ticks", 10);
        new SoulTrailTask(this).runTaskTimer(this, 40L, trailTicks);

        getLogger().info("SoulGraves enabled.");
    }

    @Override
    public void onDisable() {
        if (database != null) database.close();
    }

    public ConfigManager getConfigManager() { return configManager; }
    public GraveManager getGraveManager() { return graveManager; }
    public MarkerService getMarkerService() { return markerService; }
    public TeleportService getTeleportService() { return teleportService; }
    public VaultHook getVaultHook() { return vaultHook; }
    public ItemsAdderHook getItemsAdderHook() { return itemsAdderHook; }
    public GraveListGUI getGraveListGUI() { return graveListGUI; }
    public GraveContentsGUI getGraveContentsGUI() { return graveContentsGUI; }
}
