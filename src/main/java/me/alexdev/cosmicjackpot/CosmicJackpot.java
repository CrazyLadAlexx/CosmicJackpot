package me.alexdev.cosmicjackpot;

import me.alexdev.cosmicjackpot.commands.CommandJackpot;
import me.alexdev.cosmicjackpot.listeners.JackpotListener;
import me.alexdev.cosmicjackpot.struct.JackpotManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class CosmicJackpot
extends JavaPlugin {
    private static CosmicJackpot instance;
    private JackpotManager jackpotManager;
    public static Economy economy;

    public void onEnable() {
        instance = this;
        CosmicJackpot.setupEconomy();
        this.registerCommands();
        this.jackpotManager = new JackpotManager();
        this.jackpotManager.loadFileData(this);
        this.getServer().getPluginManager().registerEvents((Listener)new JackpotListener(), (Plugin)this);
        this.getServer().getScheduler().scheduleSyncRepeatingTask((Plugin)this, () -> this.jackpotManager.onJackpotTick(), 20L, 20L);
    }

    public void onDisable() {
        instance = null;
        this.jackpotManager.saveFileData();
    }

    private void registerCommands() {
        this.getCommand("jackpot").setExecutor((CommandExecutor)new CommandJackpot());
    }

    private static void setupEconomy() {
        economy = (Economy)Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
    }

    public static CosmicJackpot get() {
        return instance;
    }

    public JackpotManager getJackpotManager() {
        return this.jackpotManager;
    }
}

