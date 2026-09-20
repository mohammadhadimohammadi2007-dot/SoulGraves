package ir.soulgraves.commands;

import ir.soulgraves.SoulGravesPlugin;
import ir.soulgraves.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GravesCommand implements CommandExecutor {

    private final SoulGravesPlugin plugin;

    public GravesCommand(SoulGravesPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (plugin.getGraveManager().gravesOf(p.getUniqueId()).isEmpty()) {
            p.sendMessage(Msg.parse(plugin.getConfigManager().messages()
                    .getString("no_graves", "<gray>No graves.")));
            return true;
        }
        plugin.getGraveListGUI().open(p);
        return true;
    }
}
