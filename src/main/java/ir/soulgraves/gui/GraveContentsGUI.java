package ir.soulgraves.gui;

import ir.soulgraves.SoulGravesPlugin;
import ir.soulgraves.grave.Grave;
import ir.soulgraves.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Chest-style GUI for looting a grave. Player can freely take/place items.
 * On close, remaining items are persisted; if empty, grave is removed.
 */
public final class GraveContentsGUI implements Listener {

    private final SoulGravesPlugin plugin;
    private final Map<UUID, Grave> openByViewer = new HashMap<>();

    public GraveContentsGUI(SoulGravesPlugin plugin) { this.plugin = plugin; }

    public void open(Player player, Grave grave) {
        int size = roundUpToChestSize(grave.getItems().size());
        Inventory inv = Bukkit.createInventory(new GraveContentsHolder(grave.getId()),
                size, Msg.parse("<dark_gray>Grave — <gray>" + player.getName()));
        for (int i = 0; i < grave.getItems().size() && i < size; i++) {
            ItemStack it = grave.getItems().get(i);
            if (it != null && !it.getType().isAir()) inv.setItem(i, it);
        }
        openByViewer.put(player.getUniqueId(), grave);
        player.openInventory(inv);
    }

    private int roundUpToChestSize(int items) {
        int rows = (int) Math.ceil(Math.max(items, 9) / 9.0);
        rows = Math.max(1, Math.min(6, rows));
        return rows * 9;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        // No restriction — behaves like a chest.
        // Owner check is enforced at open time by MarkerInteractListener.
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) { /* allowed */ }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof GraveContentsHolder holder)) return;
        Grave g = plugin.getGraveManager().byId(holder.graveId);
        openByViewer.remove(e.getPlayer().getUniqueId());
        if (g == null) return;

        // Rebuild item list from the inventory contents
        List<ItemStack> updated = new ArrayList<>();
        for (ItemStack it : e.getInventory().getContents()) {
            if (it != null && !it.getType().isAir()) updated.add(it.clone());
        }
        g.getItems().clear();
        g.getItems().addAll(updated);

        plugin.getMarkerService().checkEmptyAndCleanup(g);
    }

    public static final class GraveContentsHolder implements org.bukkit.inventory.InventoryHolder {
        public final UUID graveId;
        public GraveContentsHolder(UUID graveId) { this.graveId = graveId; }
        @Override public Inventory getInventory() { return null; }
    }
}
