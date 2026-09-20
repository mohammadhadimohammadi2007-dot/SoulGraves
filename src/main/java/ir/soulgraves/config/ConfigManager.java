package ir.soulgraves.config;

import ir.soulgraves.SoulGravesPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class ConfigManager {

    private final SoulGravesPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;

    public ConfigManager(SoulGravesPlugin plugin) { this.plugin = plugin; }

    public void load() {
        plugin.saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        this.messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
    }

    public void reload() { load(); }

    private void saveResourceIfMissing(String name) {
        File f = new File(plugin.getDataFolder(), name);
        if (!f.exists()) plugin.saveResource(name, false);
    }

    public FileConfiguration config() { return config; }
    public FileConfiguration messages() { return messages; }

    // ─── shortcuts ────────────────────────────
    public int maxGraves() { return config.getInt("core.max_graves_per_player", 3); }
    public int graveLifetimeHours() { return config.getInt("core.grave_lifetime_hours", 24); }
    public int hardCapDays() { return config.getInt("core.hard_cap_days", 30); }
    public int expiryInterval() { return config.getInt("core.expiry_check_interval_seconds", 300); }
    public String xpHandling() { return config.getString("core.xp_handling", "VANILLA_DROP"); }

    public String worldMode() { return config.getString("worlds.mode", "BLACKLIST"); }
    public java.util.List<String> worldList() { return config.getStringList("worlds.list"); }

    public String defaultPvpBehavior() { return config.getString("pvp_behavior.default", "GRAVE"); }
    public String pvpBehaviorFor(String world) {
        return config.getString("pvp_behavior.overrides." + world, defaultPvpBehavior());
    }

    public String itemsAdderId() { return config.getString("marker.itemsadder_id", ""); }
    public String fallbackMarker() { return config.getString("marker.fallback", "CHEST"); }
    public boolean spawnInUnsafe() { return config.getBoolean("marker.spawn_in_unsafe", true); }

    public boolean teleportCostEnabled() { return config.getBoolean("teleport.cost.enabled", true); }
    public double teleportCost() { return config.getDouble("teleport.cost.amount", 250); }
    public int teleportCooldown() { return config.getInt("teleport.cooldown_seconds", 30); }
    public int teleportSafeRadius() { return config.getInt("teleport.safe_search.radius", 10); }
    public int teleportScanUp() { return config.getInt("teleport.safe_search.max_scan_up", 40); }

    public boolean isWorldAllowed(String world) {
        boolean whitelist = worldMode().equalsIgnoreCase("WHITELIST");
        boolean listed = worldList().contains(world);
        return whitelist ? listed : !listed;
    }
}
