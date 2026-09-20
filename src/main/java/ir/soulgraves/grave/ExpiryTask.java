package ir.soulgraves.grave;

import ir.soulgraves.SoulGravesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public final class ExpiryTask extends BukkitRunnable {

    private final SoulGravesPlugin plugin;

    public ExpiryTask(SoulGravesPlugin plugin) { this.plugin = plugin; }

    @Override
    public void run() {
        long lifetimeTicks = (long) plugin.getConfigManager().graveLifetimeHours() * 60L * 60L * 20L;
        long hardCapMs = plugin.getConfigManager().hardCapDays() * 24L * 60L * 60L * 1000L;
        long now = System.currentTimeMillis();

        List<Grave> toExpire = new ArrayList<>();
        List<Grave> toGhostPurge = new ArrayList<>();

        for (Grave g : plugin.getGraveManager().all()) {

            // Wall-clock hard cap
            if (now - g.getDeathTimeMs() >= hardCapMs) {
                toExpire.add(g);
                continue;
            }

            // Playtime lifetime — only advances while owner is online (or from last known stat)
            long currentPt = plugin.getGraveManager().playtimeTicks(g.getOwnerUuid());
            if (currentPt - g.getCreationPlaytimeTicks() >= lifetimeTicks) {
                toExpire.add(g);
                continue;
            }

            // Ghost marker check — only meaningful for loaded chunks
            Location loc = g.getLocation();
            if (loc == null || loc.getWorld() == null) continue;
            if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;
            if (!plugin.getMarkerService().isMarkerAlive(g)) {
                toGhostPurge.add(g);
            }
        }

        for (Grave g : toExpire) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMarkerService().expireGrave(g));
        }
        for (Grave g : toGhostPurge) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getMarkerService().purgeGhost(g));
        }
    }
}
