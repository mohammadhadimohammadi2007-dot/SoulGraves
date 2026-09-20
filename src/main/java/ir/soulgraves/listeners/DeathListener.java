package ir.soulgraves.listeners;

import ir.soulgraves.SoulGravesPlugin;
import ir.soulgraves.grave.Grave;
import ir.soulgraves.util.Msg;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class DeathListener implements Listener {

    private final SoulGravesPlugin plugin;

    public DeathListener(SoulGravesPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        String worldName = p.getWorld().getName();

        // World filter
        if (!plugin.getConfigManager().isWorldAllowed(worldName)) return;

        // PvP behavior
        boolean killedByPlayer = p.getKiller() != null;
        String pvpMode = plugin.getConfigManager().pvpBehaviorFor(worldName);
        if (killedByPlayer && pvpMode.equalsIgnoreCase("DROP")) return;

        // Max grave check
        int count = plugin.getGraveManager().countOf(p.getUniqueId());
        if (count >= plugin.getConfigManager().maxGraves()) {
            p.sendMessage(Msg.parse(plugin.getConfigManager().messages()
                    .getString("grave_full_drop", "<red>Grave slots full")));
            return;
        }

        // Collect items
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack it : e.getDrops()) {
            if (it != null && !it.getType().isAir()) items.add(it.clone());
        }
        if (items.isEmpty()) return;
        e.getDrops().clear();

        // Build grave
        Location loc = p.getLocation();
        String cause = "UNKNOWN";
        EntityDamageEvent last = p.getLastDamageCause();
        if (last != null) cause = last.getCause().name();
        String killer = p.getKiller() == null ? null : p.getKiller().getName();

        Grave.MarkerType type;
        String markerId = null;
        String iaId = plugin.getConfigManager().itemsAdderId();
        if (iaId != null && !iaId.isEmpty() && plugin.getItemsAdderHook().isAvailable()) {
            type = Grave.MarkerType.ITEMSADDER;
            markerId = iaId;
        } else {
            try { type = Grave.MarkerType.valueOf(plugin.getConfigManager().fallbackMarker()); }
            catch (Exception ex) { type = Grave.MarkerType.CHEST; }
        }

        int order = plugin.getGraveManager().nextOrderFor(p.getUniqueId());
        long playtime = p.getStatistic(Statistic.PLAY_ONE_MINUTE);

        Grave grave = new Grave(
                UUID.randomUUID(), p.getUniqueId(), worldName,
                loc.getX(), loc.getY(), loc.getZ(),
                System.currentTimeMillis(), playtime,
                cause, killer, order, type, markerId,
                items
        );

        plugin.getGraveManager().add(grave);
        plugin.getMarkerService().spawnMarker(grave);
        p.sendMessage(Msg.parse(plugin.getConfigManager().messages()
                .getString("grave_created", "<gray>Grave created")));
    }
}
