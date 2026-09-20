package ir.soulgraves.integration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;

/**
 * Reflection-based wrapper around the ItemsAdder API so we don't need a compile-time dependency.
 * The plugin is a softdepend — if ItemsAdder is missing, all calls no-op.
 */
public final class ItemsAdderHook {

    private final boolean available;
    private Method furnitureSpawn;
    private Method furnitureByArmorstand;
    private Method furnitureRemove;
    private Method furnitureGetNamespacedId;
    private Method customBlockGetInstance;
    private Method customBlockPlace;
    private Method customBlockRemove;
    private Method customBlockByAlreadyPlaced;
    private Method customBlockGetNamespacedId;

    public ItemsAdderHook() {
        this.available = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
        if (available) {
            try {
                Class<?> cf = Class.forName("dev.lone.itemsadder.api.CustomFurniture");
                furnitureSpawn = cf.getMethod("spawn", String.class, org.bukkit.block.Block.class);
                furnitureByArmorstand = cf.getMethod("byAlreadySpawned", ArmorStand.class);
                furnitureRemove = cf.getMethod("remove", boolean.class);
                try { furnitureGetNamespacedId = cf.getMethod("getNamespacedID"); } catch (NoSuchMethodException ignored) {}

                Class<?> cb = Class.forName("dev.lone.itemsadder.api.CustomBlock");
                customBlockGetInstance = cb.getMethod("getInstance", String.class);
                customBlockPlace = cb.getMethod("place", Location.class);
                customBlockRemove = cb.getMethod("remove", Location.class);
                try { customBlockByAlreadyPlaced = cb.getMethod("byAlreadyPlaced", org.bukkit.block.Block.class); } catch (NoSuchMethodException ignored) {}
                try { customBlockGetNamespacedId = cb.getMethod("getNamespacedID"); } catch (NoSuchMethodException ignored) {}
            } catch (Throwable t) {
                Bukkit.getLogger().warning("[SoulGraves] ItemsAdder API mismatch: " + t.getMessage());
            }
        }
    }

    public boolean isAvailable() { return available; }

    public Entity placeFurniture(String namespaceId, Location loc) {
        if (!available || namespaceId == null || namespaceId.isEmpty() || furnitureSpawn == null) return null;
        try {
            Object furniture = furnitureSpawn.invoke(null, namespaceId, loc.getBlock());
            if (furniture != null) {
                Method getAs = furniture.getClass().getMethod("getArmorstand");
                Object as = getAs.invoke(furniture);
                if (as instanceof Entity ent) return ent;
            }
        } catch (Throwable ignored) {}
        // Fallback to custom block
        try {
            if (customBlockGetInstance != null) {
                Object block = customBlockGetInstance.invoke(null, namespaceId);
                if (block != null && customBlockPlace != null) customBlockPlace.invoke(block, loc);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public void removeFurniture(Entity entity) {
        if (!available || entity == null) return;
        try {
            if (entity instanceof ArmorStand as && furnitureByArmorstand != null) {
                Object f = furnitureByArmorstand.invoke(null, as);
                if (f != null && furnitureRemove != null) {
                    furnitureRemove.invoke(f, false);
                    return;
                }
            }
        } catch (Throwable ignored) {}
        entity.remove();
    }

    public void removeCustomBlock(Location loc) {
        if (!available || customBlockRemove == null) return;
        try { customBlockRemove.invoke(null, loc); } catch (Throwable ignored) {}
    }

    /**
     * Checks whether an ItemsAdder marker (custom block or furniture) is present at the given location.
     * If expectedId is provided, also verifies the namespaced id matches.
     * Returns false silently when the chunk isn't loaded — caller must handle that case.
     */
    public boolean isMarkerAt(Location loc, String expectedId) {
        if (!available || loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return true; // avoid false positives

        // Custom block check
        try {
            if (customBlockByAlreadyPlaced != null) {
                Object cb = customBlockByAlreadyPlaced.invoke(null, loc.getBlock());
                if (cb != null) {
                    if (expectedId == null || expectedId.isEmpty()) return true;
                    if (customBlockGetNamespacedId != null) {
                        Object id = customBlockGetNamespacedId.invoke(cb);
                        if (id != null && id.toString().equalsIgnoreCase(expectedId)) return true;
                    } else {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Furniture check — search nearby armorstands
        try {
            if (furnitureByArmorstand != null) {
                for (Entity ent : loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 0.5, 0.5), 1.2, 1.2, 1.2)) {
                    if (!(ent instanceof ArmorStand as)) continue;
                    Object f = furnitureByArmorstand.invoke(null, as);
                    if (f == null) continue;
                    if (expectedId == null || expectedId.isEmpty()) return true;
                    if (furnitureGetNamespacedId != null) {
                        Object id = furnitureGetNamespacedId.invoke(f);
                        if (id != null && id.toString().equalsIgnoreCase(expectedId)) return true;
                    } else {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        return false;
    }

    /** Returns the block Location backing an IA event object (FurnitureBreakEvent / CustomBlockBreakEvent). */
    public Location extractEventLocation(Object event) {
        if (event == null) return null;
        // FurnitureBreakEvent has getBukkitEntity() → ArmorStand
        try {
            Method m = event.getClass().getMethod("getBukkitEntity");
            Object ent = m.invoke(event);
            if (ent instanceof Entity e) return e.getLocation();
        } catch (Throwable ignored) {}
        // CustomBlockBreakEvent has getBlock() → Block
        try {
            Method m = event.getClass().getMethod("getBlock");
            Object b = m.invoke(event);
            if (b instanceof org.bukkit.block.Block bl) return bl.getLocation();
        } catch (Throwable ignored) {}
        return null;
    }
}
