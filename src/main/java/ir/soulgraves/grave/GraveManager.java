package ir.soulgraves.grave;

import ir.soulgraves.SoulGravesPlugin;
import ir.soulgraves.storage.GraveRepository;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GraveManager {

    private final SoulGravesPlugin plugin;
    private final GraveRepository repo;
    private final Map<UUID, Grave> cache = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> markerToGrave = new ConcurrentHashMap<>();

    public GraveManager(SoulGravesPlugin plugin, GraveRepository repo) {
        this.plugin = plugin;
        this.repo = repo;
    }

    public void loadAll() {
        try {
            for (Grave g : repo.findAll()) cache.put(g.getId(), g);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load graves: " + e.getMessage());
        }
    }

    public Collection<Grave> all() { return cache.values(); }

    public List<Grave> gravesOf(UUID owner) {
        List<Grave> list = new ArrayList<>();
        for (Grave g : cache.values()) if (g.getOwnerUuid().equals(owner)) list.add(g);
        list.sort(Comparator.comparingLong(Grave::getDeathTimeMs));
        return list;
    }

    public int countOf(UUID owner) { return gravesOf(owner).size(); }

    public Grave byId(UUID id) { return cache.get(id); }

    public Grave byMarker(UUID markerEntityId) {
        UUID graveId = markerToGrave.get(markerEntityId);
        return graveId == null ? null : cache.get(graveId);
    }

    public void bindMarker(UUID markerEntityId, UUID graveId) {
        markerToGrave.put(markerEntityId, graveId);
    }

    public void unbindMarker(UUID markerEntityId) {
        markerToGrave.remove(markerEntityId);
    }

    /** Latest grave (by death time) of owner in given world. */
    public Grave latestInWorld(UUID owner, String world) {
        Grave latest = null;
        for (Grave g : cache.values()) {
            if (!g.getOwnerUuid().equals(owner)) continue;
            if (!g.getWorldName().equals(world)) continue;
            if (latest == null || g.getDeathTimeMs() > latest.getDeathTimeMs()) latest = g;
        }
        return latest;
    }

    public void add(Grave g) {
        cache.put(g.getId(), g);
        try { repo.insert(g); }
        catch (Exception e) { plugin.getLogger().severe("Failed to persist grave: " + e.getMessage()); }
    }

    public void saveItems(Grave g) {
        try { repo.updateItems(g.getId(), g.getItems()); }
        catch (Exception e) { plugin.getLogger().severe("Failed to update grave items: " + e.getMessage()); }
    }

    public void remove(Grave g) {
        cache.remove(g.getId());
        markerToGrave.values().removeIf(id -> id.equals(g.getId()));
        try { repo.delete(g.getId()); }
        catch (Exception e) { plugin.getLogger().severe("Failed to delete grave: " + e.getMessage()); }
    }

    /** Assign order 1..N to a new grave for owner (append). */
    public int nextOrderFor(UUID owner) {
        return Math.min(countOf(owner) + 1, plugin.getConfigManager().maxGraves());
    }

    /** Returns tick playtime for online OR offline player. */
    public long playtimeTicks(UUID owner) {
        Player online = Bukkit.getPlayer(owner);
        if (online != null) return online.getStatistic(Statistic.PLAY_ONE_MINUTE);
        OfflinePlayer off = Bukkit.getOfflinePlayer(owner);
        try { return off.getStatistic(Statistic.PLAY_ONE_MINUTE); }
        catch (Throwable t) { return 0L; }
    }
}
