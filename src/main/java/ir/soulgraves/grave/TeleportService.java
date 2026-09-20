package ir.soulgraves.grave;

import ir.soulgraves.SoulGravesPlugin;
import ir.soulgraves.util.Msg;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TeleportService {

    private final SoulGravesPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public TeleportService(SoulGravesPlugin plugin) { this.plugin = plugin; }

    public void teleportToGrave(Player player, Grave grave) {
        long cd = plugin.getConfigManager().teleportCooldown() * 1000L;
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < cd) {
            long left = (cd - (now - last)) / 1000L;
            player.sendMessage(Msg.parse(plugin.getConfigManager().messages()
                    .getString("teleport_cooldown", "<red>Cooldown {seconds}s")
                    .replace("{seconds}", String.valueOf(left))));
            return;
        }

        double cost = plugin.getConfigManager().teleportCost();
        if (plugin.getConfigManager().teleportCostEnabled() && cost > 0 && plugin.getVaultHook().isAvailable()) {
            if (!plugin.getVaultHook().has(player, cost)) {
                Map<String, String> ph = new HashMap<>();
                ph.put("cost", String.valueOf((int) cost));
                player.sendMessage(Msg.parse(plugin.getConfigManager().messages()
                        .getString("teleport_no_money", "<red>Not enough money"), ph));
                return;
            }
            plugin.getVaultHook().withdraw(player, cost);
        }

        Location safe = findSafeSpot(grave.getLocation());
        if (safe == null) {
            player.sendMessage(Msg.parse(plugin.getConfigManager().messages()
                    .getString("teleport_no_safe", "<red>No safe spot")));
            return;
        }

        cooldowns.put(player.getUniqueId(), now);
        player.teleport(safe);
        player.sendMessage(Msg.parse(plugin.getConfigManager().messages()
                .getString("teleport_success", "<green>Teleported")));
    }

    private Location findSafeSpot(Location origin) {
        if (origin == null || origin.getWorld() == null) return null;
        int radius = plugin.getConfigManager().teleportSafeRadius();
        int scanUp = plugin.getConfigManager().teleportScanUp();

        // Try scanning upward from origin first
        for (int dy = 0; dy <= scanUp; dy++) {
            Location cand = origin.clone().add(0, dy, 0);
            if (isSafe(cand)) return cand;
        }

        // Then try a small horizontal ring
        for (int r = 1; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    for (int dy = 0; dy <= scanUp; dy++) {
                        Location cand = origin.clone().add(dx, dy, dz);
                        if (isSafe(cand)) return cand;
                    }
                }
            }
        }
        return null;
    }

    private boolean isSafe(Location loc) {
        if (loc.getY() < loc.getWorld().getMinHeight() + 1) return false;
        if (loc.getY() > loc.getWorld().getMaxHeight() - 2) return false;
        Block feet = loc.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block ground = feet.getRelative(0, -1, 0);
        if (!feet.getType().isAir() || !head.getType().isAir()) return false;
        Material g = ground.getType();
        if (g.isAir() || g == Material.LAVA || g == Material.FIRE || g == Material.MAGMA_BLOCK) return false;
        return ground.getType().isSolid();
    }
}
