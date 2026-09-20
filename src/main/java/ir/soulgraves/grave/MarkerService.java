package ir.soulgraves.grave;

import ir.soulgraves.SoulGravesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

public final class MarkerService {

    private final SoulGravesPlugin plugin;
    public final NamespacedKey GRAVE_KEY;

    public MarkerService(SoulGravesPlugin plugin) {
        this.plugin = plugin;
        this.GRAVE_KEY = new NamespacedKey(plugin, "grave_id");
    }

    /** Spawn marker at grave's location. */
    public void spawnMarker(Grave grave) {
        Location loc = grave.getLocation();
        if (loc == null) return;

        if (grave.getMarkerType() == Grave.MarkerType.ITEMSADDER
                && plugin.getItemsAdderHook().isAvailable()
                && grave.getMarkerId() != null && !grave.getMarkerId().isEmpty()) {
            Entity ent = plugin.getItemsAdderHook().placeFurniture(grave.getMarkerId(), loc);
            String graveIdStr = grave.getId().toString();
            if (ent != null) {
                ent.getPersistentDataContainer().set(GRAVE_KEY, PersistentDataType.STRING, graveIdStr);
                plugin.getGraveManager().bindMarker(ent.getUniqueId(), grave.getId());
            }
            // Tag ALL related entities that IA spawned (hitbox armorstands, interaction entities,
            // item displays) within a tight radius so any right-click on any of them opens the grave.
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> tagNearbyFurnitureEntities(loc, graveIdStr), 2L);
            return;
        }

        // Fallback: place a block-based marker
        Block block = loc.getBlock();
        switch (grave.getMarkerType()) {
            case CHEST -> block.setType(Material.CHEST);
            case PLAYER_HEAD -> {
                block.setType(Material.PLAYER_HEAD);
                if (block.getState() instanceof Skull s) {
                    s.setOwningPlayer(Bukkit.getOfflinePlayer(grave.getOwnerUuid()));
                    s.update();
                }
            }
            case SKELETON_SKULL -> block.setType(Material.SKELETON_SKULL);
            default -> block.setType(Material.CHEST);
        }
    }

    public void removeMarker(Grave grave) {
        Location loc = grave.getLocation();
        if (loc == null) return;

        if (grave.getMarkerType() == Grave.MarkerType.ITEMSADDER) {
            for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                String tag = e.getPersistentDataContainer().get(GRAVE_KEY, PersistentDataType.STRING);
                if (tag != null && tag.equals(grave.getId().toString())) {
                    plugin.getItemsAdderHook().removeFurniture(e);
                    plugin.getGraveManager().unbindMarker(e.getUniqueId());
                }
            }
            plugin.getItemsAdderHook().removeCustomBlock(loc);
            return;
        }

        Block b = loc.getBlock();
        Material m = b.getType();
        if (m == Material.CHEST || m == Material.PLAYER_HEAD || m == Material.SKELETON_SKULL
                || m == Material.PLAYER_WALL_HEAD || m == Material.SKELETON_WALL_SKULL) {
            b.setType(Material.AIR);
        }
    }

    /** Called on natural empty (looted to zero) or 24h expiry. */
    public void expireGrave(Grave grave) {
        removeMarker(grave);
        plugin.getGraveManager().remove(grave);
    }

    /** Called after player takes items; removes marker only if empty. */
    public void checkEmptyAndCleanup(Grave grave) {
        if (grave.isEmpty()) {
            expireGrave(grave);
        } else {
            plugin.getGraveManager().saveItems(grave);
        }
    }

    /**
     * Verifies that a physical marker still exists at the grave's location.
     * If the chunk isn't loaded, returns true (safe default — we can't tell yet).
     * If a block-marker was replaced by something else, returns false.
     * If an ItemsAdder marker was removed externally (WorldEdit, /iablock remove, etc.), returns false.
     */
    public boolean isMarkerAlive(Grave grave) {
        Location loc = grave.getLocation();
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return true;

        if (grave.getMarkerType() == Grave.MarkerType.ITEMSADDER) {
            return plugin.getItemsAdderHook().isMarkerAt(loc, grave.getMarkerId());
        }

        Material actual = loc.getBlock().getType();
        return switch (grave.getMarkerType()) {
            case CHEST -> actual == Material.CHEST;
            case PLAYER_HEAD -> actual == Material.PLAYER_HEAD || actual == Material.PLAYER_WALL_HEAD;
            case SKELETON_SKULL -> actual == Material.SKELETON_SKULL || actual == Material.SKELETON_WALL_SKULL;
            default -> false;
        };
    }

    /** Silently purge a grave whose marker vanished: no drops, no message — the marker was already lost. */
    public void purgeGhost(Grave grave) {
        plugin.getGraveManager().remove(grave);
    }

    /** Tag every armorstand / interaction / item_display near the location with our grave PDC key. */
    private void tagNearbyFurnitureEntities(Location loc, String graveIdStr) {
        if (loc == null || loc.getWorld() == null) return;
        double r = 1.5;
        for (Entity e : loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 0.5, 0.5), r, r, r)) {
            String type = e.getType().name();
            if (!type.equals("ARMOR_STAND") && !type.equals("INTERACTION") && !type.equals("ITEM_DISPLAY")) continue;
            String existing = e.getPersistentDataContainer().get(GRAVE_KEY, PersistentDataType.STRING);
            if (existing != null) continue;
            e.getPersistentDataContainer().set(GRAVE_KEY, PersistentDataType.STRING, graveIdStr);
        }
    }
}
