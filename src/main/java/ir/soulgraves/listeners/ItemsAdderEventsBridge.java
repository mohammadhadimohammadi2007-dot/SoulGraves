package ir.soulgraves.listeners;

import ir.soulgraves.SoulGravesPlugin;
import ir.soulgraves.grave.Grave;
import ir.soulgraves.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Method;

/**
 * Registers listeners for ItemsAdder's FurnitureBreakEvent and CustomBlockBreakEvent
 * via reflection, so we don't need a compile-time dependency on ItemsAdder.
 *
 * When a grave's IA marker is broken (by owner, admin, or another plugin), we:
 *   - Cancel the break if the breaker is not the grave owner
 *   - Open the loot GUI for the owner
 *   - Purge the grave from DB if the marker is truly gone (external removal)
 */
public final class ItemsAdderEventsBridge implements Listener {

    private final SoulGravesPlugin plugin;

    public ItemsAdderEventsBridge(SoulGravesPlugin plugin) { this.plugin = plugin; }

    @SuppressWarnings("unchecked")
    public void register() {
        if (!plugin.getItemsAdderHook().isAvailable()) return;

        register("dev.lone.itemsadder.api.Events.FurnitureBreakEvent");
        register("dev.lone.itemsadder.api.Events.CustomBlockBreakEvent");
    }

    @SuppressWarnings("unchecked")
    private void register(String className) {
        try {
            Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName(className);
            EventExecutor executor = (listener, event) -> handle(event);
            Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.MONITOR, executor, plugin, false);
            plugin.getLogger().info("[SoulGraves] Registered IA listener for " + eventClass.getSimpleName());
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("[SoulGraves] IA event not found (older version?): " + className);
        } catch (Throwable t) {
            plugin.getLogger().warning("[SoulGraves] Failed to register IA listener for " + className + ": " + t.getMessage());
        }
    }

    private void handle(Event event) {
        Location loc = plugin.getItemsAdderHook().extractEventLocation(event);
        if (loc == null) return;

        Grave grave = graveAt(loc);
        if (grave == null) return;

        Player player = extractPlayer(event);

        if (player != null && !grave.getOwnerUuid().equals(player.getUniqueId())) {
            // Non-owner tried to break the marker — cancel and warn
            cancelEvent(event);
            player.sendMessage(Msg.parse(plugin.getConfigManager().messages()
                    .getString("marker_not_owner", "<red>Not your grave.")));
            return;
        }

        if (player != null) {
            // Owner is breaking — cancel physical break, hand them the loot GUI instead
            cancelEvent(event);
            plugin.getGraveContentsGUI().open(player, grave);
        } else {
            // No player context (WorldEdit, /iablock remove, command) — treat as ghost purge
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMarkerService().purgeGhost(grave));
        }
    }

    private Grave graveAt(Location loc) {
        for (Grave g : plugin.getGraveManager().all()) {
            if (g.getMarkerType() != Grave.MarkerType.ITEMSADDER) continue;
            if (loc.getWorld() == null || !g.getWorldName().equals(loc.getWorld().getName())) continue;
            if ((int) Math.floor(g.getX()) != loc.getBlockX()) continue;
            if ((int) Math.floor(g.getY()) != loc.getBlockY()) continue;
            if ((int) Math.floor(g.getZ()) != loc.getBlockZ()) continue;
            return g;
        }
        return null;
    }

    private Player extractPlayer(Event event) {
        try {
            Method m = event.getClass().getMethod("getPlayer");
            Object p = m.invoke(event);
            if (p instanceof Player pl) return pl;
        } catch (Throwable ignored) {}
        return null;
    }

    private void cancelEvent(Event event) {
        try {
            Method m = event.getClass().getMethod("setCancelled", boolean.class);
            m.invoke(event, true);
        } catch (Throwable ignored) {}
    }
}
