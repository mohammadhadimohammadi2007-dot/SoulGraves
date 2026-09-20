package ir.soulgraves.particles;

import ir.soulgraves.SoulGravesPlugin;
import ir.soulgraves.grave.Grave;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class SoulTrailTask extends BukkitRunnable {

    private final SoulGravesPlugin plugin;

    public SoulTrailTask(SoulGravesPlugin plugin) { this.plugin = plugin; }

    @Override
    public void run() {
        if (!plugin.getConfigManager().config().getBoolean("soul_trail.enabled", true)) return;
        Particle particle = parseParticle(plugin.getConfigManager().config()
                .getString("soul_trail.particle", "SOUL_FIRE_FLAME"));
        double activation = plugin.getConfigManager().config().getDouble("soul_trail.activation_radius", 50);
        double renderLength = plugin.getConfigManager().config().getDouble("soul_trail.render_length", 40);
        double step = plugin.getConfigManager().config().getDouble("soul_trail.step", 0.5);
        boolean sameWorldOnly = plugin.getConfigManager().config().getBoolean("soul_trail.only_when_in_grave_world", true);

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Grave target = plugin.getGraveManager().latestInWorld(p.getUniqueId(), p.getWorld().getName());
            if (target == null) continue;

            Location gLoc = target.getLocation();
            if (gLoc == null) continue;
            if (sameWorldOnly && !gLoc.getWorld().equals(p.getWorld())) continue;

            Location pLoc = p.getEyeLocation();
            double distance = pLoc.distance(gLoc);
            if (distance > activation) continue;

            Vector dir = gLoc.toVector().subtract(pLoc.toVector()).normalize();
            double length = Math.min(distance, renderLength);
            for (double d = 1.5; d <= length; d += step) {
                Location point = pLoc.clone().add(dir.clone().multiply(d));
                p.spawnParticle(particle, point, 1, 0, 0, 0, 0);
            }
        }
    }

    private Particle parseParticle(String name) {
        try { return Particle.valueOf(name.toUpperCase()); }
        catch (Exception e) { return Particle.SOUL_FIRE_FLAME; }
    }
}
