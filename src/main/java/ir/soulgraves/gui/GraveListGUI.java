package ir.soulgraves.gui;

import ir.soulgraves.SoulGravesPlugin;
import ir.soulgraves.grave.Grave;
import ir.soulgraves.util.Msg;
import ir.soulgraves.util.TimeFormat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class GraveListGUI implements Listener {

    private final SoulGravesPlugin plugin;
    private final NamespacedKey graveIdKey;
    private final Map<UUID, List<Grave>> openSessions = new HashMap<>();

    public GraveListGUI(SoulGravesPlugin plugin) {
        this.plugin = plugin;
        this.graveIdKey = new NamespacedKey(plugin, "gui_grave_id");
    }

    public void open(Player player) {
        List<Grave> graves = plugin.getGraveManager().gravesOf(player.getUniqueId());
        int size = plugin.getConfigManager().config().getInt("gui.size", 27);
        String title = plugin.getConfigManager().config()
                .getString("gui.titles." + Math.min(graves.size(), plugin.getConfigManager().maxGraves()),
                        "<gray>Graves");

        Inventory inv = Bukkit.createInventory(new GraveListHolder(), size, Msg.parse(title));
        applyFiller(inv);

        for (int i = 0; i < graves.size() && i < plugin.getConfigManager().maxGraves(); i++) {
            Grave g = graves.get(i);
            int idx = i + 1;                            // grave_1 = oldest
            ConfigurationSection section = plugin.getConfigManager().config()
                    .getConfigurationSection("gui.grave_items.grave_" + idx);
            if (section == null) continue;
            ItemStack item = buildGraveItem(section, g);
            inv.setItem(section.getInt("slot"), item);
        }

        openSessions.put(player.getUniqueId(), graves);
        player.openInventory(inv);
    }

    private void applyFiller(Inventory inv) {
        ConfigurationSection filler = plugin.getConfigManager().config().getConfigurationSection("gui.filler");
        if (filler == null) return;
        Material mat = Material.matchMaterial(filler.getString("material", "GRAY_STAINED_GLASS_PANE"));
        if (mat == null) return;
        int cmd = filler.getInt("custom_model_data", 0);
        String name = filler.getString("name", " ");
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.displayName(Msg.parse(name));
            if (cmd > 0) meta.setCustomModelData(cmd);
            it.setItemMeta(meta);
        }
        for (int slot : filler.getIntegerList("slots")) {
            if (slot >= 0 && slot < inv.getSize()) inv.setItem(slot, it);
        }
    }

    private ItemStack buildGraveItem(ConfigurationSection section, Grave g) {
        Material mat = Material.matchMaterial(section.getString("material", "SKELETON_SKULL"));
        if (mat == null) mat = Material.SKELETON_SKULL;
        int cmd = section.getInt("custom_model_data", 0);
        String name = section.getString("name", "<gray>Grave");
        List<String> loreRaw = section.getStringList("lore");

        Map<String, String> ph = placeholders(g);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Msg.parse(name, ph).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String line : loreRaw) {
                lore.add(Msg.parse(line, ph).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            if (cmd > 0) meta.setCustomModelData(cmd);
            meta.getPersistentDataContainer().set(graveIdKey, PersistentDataType.STRING, g.getId().toString());
            item.setItemMeta(meta);
        }
        return item;
    }

    private Map<String, String> placeholders(Grave g) {
        long lifetimeMs = plugin.getConfigManager().graveLifetimeHours() * 3600_000L;
        long ptElapsedTicks = plugin.getGraveManager().playtimeTicks(g.getOwnerUuid()) - g.getCreationPlaytimeTicks();
        long ptRemainingMs = Math.max(0L, lifetimeMs - (ptElapsedTicks * 50L));

        long hardCapMs = plugin.getConfigManager().hardCapDays() * 24L * 3600_000L;
        long wallElapsed = System.currentTimeMillis() - g.getDeathTimeMs();
        long wallRemaining = Math.max(0L, hardCapMs - wallElapsed);

        long remaining = Math.min(ptRemainingMs, wallRemaining);

        String pattern = plugin.getConfigManager().config().getString("formatting.date_pattern", "yyyy-MM-dd HH:mm");

        Map<String, String> map = new HashMap<>();
        map.put("death_time", TimeFormat.formatEpoch(g.getDeathTimeMs(), pattern));
        map.put("world", g.getWorldName());
        map.put("x", String.valueOf((int) g.getX()));
        map.put("y", String.valueOf((int) g.getY()));
        map.put("z", String.valueOf((int) g.getZ()));
        map.put("death_cause", formatCause(g));
        map.put("killer", g.getKiller() == null ? "" : g.getKiller());
        map.put("time_left", TimeFormat.formatDuration(remaining));
        map.put("cost", String.valueOf((int) plugin.getConfigManager().teleportCost()));
        return map;
    }

    private String formatCause(Grave g) {
        String raw = plugin.getConfigManager().config()
                .getString("death_causes." + (g.getDeathCause() == null ? "UNKNOWN" : g.getDeathCause()),
                        "<gray>Unknown");
        if (g.getKiller() != null) raw = raw.replace("{killer}", g.getKiller());
        return raw;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof GraveListHolder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;
        String id = clicked.getItemMeta().getPersistentDataContainer().get(graveIdKey, PersistentDataType.STRING);
        if (id == null) return;
        Grave g = plugin.getGraveManager().byId(UUID.fromString(id));
        if (g == null) { p.closeInventory(); return; }
        // Ghost check — chunk may not be loaded here, so this only fires when we're certain the marker vanished
        if (g.getLocation() != null && g.getLocation().getWorld() != null
                && g.getLocation().getWorld().isChunkLoaded(g.getLocation().getBlockX() >> 4, g.getLocation().getBlockZ() >> 4)
                && !plugin.getMarkerService().isMarkerAlive(g)) {
            p.closeInventory();
            plugin.getMarkerService().purgeGhost(g);
            p.sendMessage(Msg.parse(plugin.getConfigManager().messages()
                    .getString("grave_lost", "<red>That grave marker is gone.")));
            return;
        }
        p.closeInventory();
        plugin.getTeleportService().teleportToGrave(p, g);
    }

    public static final class GraveListHolder implements org.bukkit.inventory.InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}
