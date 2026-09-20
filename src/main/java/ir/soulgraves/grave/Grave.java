package ir.soulgraves.grave;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public final class Grave {

    public enum MarkerType { ITEMSADDER, CHEST, PLAYER_HEAD, SKELETON_SKULL }

    private final UUID id;
    private final UUID ownerUuid;
    private final String worldName;
    private final double x, y, z;
    private final long deathTimeMs;
    private final long creationPlaytimeTicks;
    private final String deathCause;
    private final String killer;
    private final int graveOrder;
    private final MarkerType markerType;
    private final String markerId;
    private final List<ItemStack> items;

    public Grave(UUID id, UUID ownerUuid, String worldName,
                 double x, double y, double z,
                 long deathTimeMs, long creationPlaytimeTicks,
                 String deathCause, String killer,
                 int graveOrder, MarkerType markerType, String markerId,
                 List<ItemStack> items) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.worldName = worldName;
        this.x = x; this.y = y; this.z = z;
        this.deathTimeMs = deathTimeMs;
        this.creationPlaytimeTicks = creationPlaytimeTicks;
        this.deathCause = deathCause;
        this.killer = killer;
        this.graveOrder = graveOrder;
        this.markerType = markerType;
        this.markerId = markerId;
        this.items = items;
    }

    public UUID getId() { return id; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public long getDeathTimeMs() { return deathTimeMs; }
    public long getCreationPlaytimeTicks() { return creationPlaytimeTicks; }
    public String getDeathCause() { return deathCause; }
    public String getKiller() { return killer; }
    public int getGraveOrder() { return graveOrder; }
    public MarkerType getMarkerType() { return markerType; }
    public String getMarkerId() { return markerId; }
    public List<ItemStack> getItems() { return items; }

    public boolean isEmpty() {
        for (ItemStack it : items) {
            if (it != null && it.getAmount() > 0 && !it.getType().isAir()) return false;
        }
        return true;
    }

    public Location getLocation() {
        World w = Bukkit.getWorld(worldName);
        if (w == null) return null;
        return new Location(w, x, y, z);
    }
}
