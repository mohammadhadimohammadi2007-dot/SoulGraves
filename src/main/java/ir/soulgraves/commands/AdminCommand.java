package ir.soulgraves.commands;

import ir.soulgraves.SoulGravesPlugin;
import ir.soulgraves.grave.Grave;
import ir.soulgraves.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AdminCommand implements CommandExecutor {

    private final SoulGravesPlugin plugin;

    public AdminCommand(SoulGravesPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("soulgraves.admin")) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("/sg reload | purge <player> | list <player>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.getConfigManager().reload();
                sender.sendMessage(Msg.parse(plugin.getConfigManager().messages()
                        .getString("admin_reload", "<green>Reloaded")));
            }
            case "purge" -> {
                if (args.length < 2) { sender.sendMessage("/sg purge <player>"); return true; }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                List<Grave> gs = plugin.getGraveManager().gravesOf(target.getUniqueId());
                for (Grave g : gs) plugin.getMarkerService().expireGrave(g);
                Map<String, String> ph = new HashMap<>();
                ph.put("player", args[1]);
                ph.put("count", String.valueOf(gs.size()));
                sender.sendMessage(Msg.parse(plugin.getConfigManager().messages()
                        .getString("admin_purge", "<green>Purged {count} for {player}"), ph));
            }
            case "list" -> {
                if (args.length < 2) { sender.sendMessage("/sg list <player>"); return true; }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                List<Grave> gs = plugin.getGraveManager().gravesOf(target.getUniqueId());
                Map<String, String> ph = new HashMap<>();
                ph.put("player", args[1]);
                sender.sendMessage(Msg.parse(plugin.getConfigManager().messages()
                        .getString("admin_list_header", "<gold>Graves of {player}"), ph));
                int i = 1;
                for (Grave g : gs) {
                    Map<String, String> lp = new HashMap<>();
                    lp.put("n", String.valueOf(i++));
                    lp.put("world", g.getWorldName());
                    lp.put("x", String.valueOf((int) g.getX()));
                    lp.put("y", String.valueOf((int) g.getY()));
                    lp.put("z", String.valueOf((int) g.getZ()));
                    long remain = (plugin.getConfigManager().graveLifetimeHours() * 3600_000L)
                            - ((plugin.getGraveManager().playtimeTicks(g.getOwnerUuid()) - g.getCreationPlaytimeTicks()) * 50L);
                    lp.put("time_left", ir.soulgraves.util.TimeFormat.formatDuration(Math.max(0, remain)));
                    sender.sendMessage(Msg.parse(plugin.getConfigManager().messages()
                            .getString("admin_list_entry", "<gray>#{n} — {world} {x},{y},{z}"), lp));
                }
            }
            default -> sender.sendMessage("Unknown subcommand.");
        }
        return true;
    }
}
