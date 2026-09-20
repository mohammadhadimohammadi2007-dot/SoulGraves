package ir.soulgraves.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultHook {

    private Economy economy;

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean isAvailable() { return economy != null; }

    public boolean has(OfflinePlayer p, double amount) {
        return economy != null && economy.has(p, amount);
    }

    public boolean withdraw(OfflinePlayer p, double amount) {
        return economy != null && economy.withdrawPlayer(p, amount).transactionSuccess();
    }
}
