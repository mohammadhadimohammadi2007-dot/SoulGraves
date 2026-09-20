package ir.soulgraves.listeners;

import ir.soulgraves.SoulGravesPlugin;
import ir.soulgraves.grave.Grave;
import ir.soulgraves.util.Msg;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;

public final class MarkerInteractListener implements Listener {

    private final SoulGravesPlugin plugin;

    public MarkerInteractListener(SoulGravesPlugin plugin) { this.plugin = plugin; }

    /**
     * Fires FIRST for armor-stand-like entities (before PlayerInteractEntityEvent).
     * We handle it at HIGH priority so we run before IA's own logic can consume it.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onEntityInteractAt(PlayerInteractAtEntityEvent e) {
        if (handleEntityInteract(e.getPlayer(), e.getRightClicked())) {
            e.setCancelled(true);
        }
    }

    /** Fallback for other entity types (ItemFrame, etc). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (e instanceof PlayerInteractAtEntityEvent) return; // handled above
        if (handleEntityInteract(e.getPlayer(), e.getRightClicked())) {
            e.setCancelled(true);
        }
    }

    /** Returns true if we opened a grave GUI for this interaction. */
    private boolean handleEntityInteract(Player player, Entity ent) {
        // 1. Direct PDC tag match
        String tag = ent.getPersistentDataContainer().get(plugin.getMarkerService().GRAVE_KEY, PersistentDataType.STRING);
        if (tag != null) {
            openIfOwner(player, tag);
            return true;
        }

        // 2. Proximity fallback — the clicked entity might be an untagged hitbox
        //    that IA spawned alongside our tagged main entity
        String type = ent.getType().name();
        if (!type.equals("ARMOR_STAND") && !type.equals("INTERACTION")
                && !type.equals("ITEM_DISPLAY") && !type.equals("ITEM_FRAME")) return false;

        Grave near = graveNearEntity(ent, 1.5);
        if (near == null) return false;
        if (!plugin.getMarkerService().isMarkerAlive(near)) {
            plugin.getMarkerService().purgeGhost(near);
            return false;
        }
        // Lazy-tag this entity so future clicks are direct
        ent.getPersistentDataContainer().set(plugin.getMarkerService().GRAVE_KEY,
                PersistentDataType.STRING, near.getId().toString());
        openIfOwner(player, near.getId().toString());
        return true;
    }

    /** Right-click on any block: if a grave exists at that location, validate marker before opening GUI. */
    @EventHandler
    public void onBlockInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        Grave g = graveAt(b.getLocation());
        if (g == null) return;

        if (!plugin.getMarkerService().isMarkerAlive(g)) {
            plugin.getMarkerService().purgeGhost(g);
            return;
        }

        e.setCancelled(true);
        openIfOwner(e.getPlayer(), g.getId().toString());
    }

    /** Prevent non-owners from breaking a grave block/furniture. */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Grave g = graveAt(e.getBlock().getLocation());
        if (g == null) return;

        if (!plugin.getMarkerService().isMarkerAlive(g)) {
            plugin.getMarkerService().purgeGhost(g);
            return;
        }

        if (!g.getOwnerUuid().equals(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Msg.parse(plugin.getConfigManager().messages()
                    .getString("marker_not_owner", "<red>Not your grave.")));
        } else {
            e.setCancelled(true);
            plugin.getGraveContentsGUI().open(e.getPlayer(), g);
        }
    }

    /** Block left-click damage on the armor stand marker from anyone. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        Entity victim = e.getEntity();
        String tag = victim.getPersistentDataContainer().get(plugin.getMarkerService().GRAVE_KEY, PersistentDataType.STRING);
        Grave g = null;
        if (tag != null) {
            try { g = plugin.getGraveManager().byId(java.util.UUID.fromString(tag)); }
            catch (IllegalArgumentException ignored) {}
        }
        if (g == null) {
            String type = victim.getType().name();
            if (!type.equals("ARMOR_STAND") && !type.equals("ITEM_DISPLAY") && !type.equals("INTERACTION")) return;
            g = graveNearEntity(victim, 1.5);
        }
        if (g == null) return;

        if (!(e.getDamager() instanceof Player damager)) { e.setCancelled(true); return; }
        if (!g.getOwnerUuid().equals(damager.getUniqueId())) {
            e.setCancelled(true);
            damager.sendMessage(Msg.parse(plugin.getConfigManager().messages()
                    .getString("marker_not_owner", "<red>Not your grave.")));
        } else {
            e.setCancelled(true);
            plugin.getGraveContentsGUI().open(damager, g);
        }
    }

    /** Some armor stand variants use the hanging break event. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent e) {
        Entity ent = e.getEntity();
        String tag = ent.getPersistentDataContainer().get(plugin.getMarkerService().GRAVE_KEY, PersistentDataType.STRING);
        Grave g = null;
        if (tag != null) {
            try { g = plugin.getGraveManager().byId(java.util.UUID.fromString(tag)); }
            catch (IllegalArgumentException ignored) {}
        }
        if (g == null) g = graveNearEntity(ent, 1.5);
        if (g == null) return;

        if (e.getRemover() instanceof Player p && g.getOwnerUuid().equals(p.getUniqueId())) {
            e.setCancelled(true);
            plugin.getGraveContentsGUI().open(p, g);
        } else {
            e.setCancelled(true);
        }
    }

    private void openIfOwner(Player player, String graveIdStr) {
        java.util.UUID graveId;
        try { graveId = java.util.UUID.fromString(graveIdStr); }
        catch (IllegalArgumentException ex) { return; }
        Grave g = plugin.getGraveManager().byId(graveId);
        if (g == null) return;
        if (!g.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage(Msg.parse(plugin.getConfigManager().messages()
                    .getString("marker_not_owner", "<red>Not your grave.")));
            return;
        }
        plugin.getGraveContentsGUI().open(player, g);
    }

    private Grave graveAt(Location loc) {
        for (Grave g : plugin.getGraveManager().all()) {
            if (loc.getWorld() == null || !g.getWorldName().equals(loc.getWorld().getName())) continue;
            if ((int) Math.floor(g.getX()) != loc.getBlockX()) continue;
            if ((int) Math.floor(g.getY()) != loc.getBlockY()) continue;
            if ((int) Math.floor(g.getZ()) != loc.getBlockZ()) continue;
            return g;
        }
        return null;
    }

    /** Proximity match limited to ITEMSADDER graves — used as fallback when an untagged hitbox is clicked. */
    private Grave graveNearEntity(Entity ent, double radius) {
        Location loc = ent.getLocation();
        if (loc.getWorld() == null) return null;
        double best = Double.MAX_VALUE;
        Grave picked = null;
        for (Grave g : plugin.getGraveManager().all()) {
            if (g.getMarkerType() != Grave.MarkerType.ITEMSADDER) continue;
            if (!g.getWorldName().equals(loc.getWorld().getName())) continue;
            double dx = (g.getX() + 0.5) - loc.getX();
            double dy = (g.getY() + 0.5) - loc.getY();
            double dz = (g.getZ() + 0.5) - loc.getZ();
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 <= radius * radius && d2 < best) { best = d2; picked = g; }
        }
        return picked;
    }
}
