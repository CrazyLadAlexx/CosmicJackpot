package me.alexdev.cosmicjackpot;

import me.alexdev.cosmicjackpot.commands.CommandJackpot;
import me.alexdev.cosmicjackpot.listeners.JackpotListener;
import me.alexdev.cosmicjackpot.struct.JackpotManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class CosmicJackpot extends JavaPlugin {
    private static CosmicJackpot instance;

    public static Economy economy;

    private JackpotManager jackpotManager;

    @Override
    public void onEnable() {
        instance = this;
        if (!this.setupEconomy()) {
            this.getLogger().severe("Vault economy provider was not found. Disabling CosmicJackpot.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.registerCommands();
        this.jackpotManager = new JackpotManager();
        this.jackpotManager.loadFileData(this);
        this.getServer().getPluginManager().registerEvents(new JackpotListener(), this);
        this.getServer().getScheduler().runTaskTimer(this, () -> this.jackpotManager.onJackpotTick(), 20L, 20L);
    }

    @Override
    public void onDisable() {
        if (this.jackpotManager != null) {
            this.jackpotManager.saveFileData();
        }
        instance = null;
    }

    private void registerCommands() {
        PluginCommand command = this.getCommand("jackpot");
        if (command == null) {
            throw new IllegalStateException("Command 'jackpot' is missing from plugin.yml");
        }
        CommandJackpot jackpotCommand = new CommandJackpot();
        command.setExecutor(jackpotCommand);
        command.setTabCompleter(jackpotCommand);
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            return false;
        }
        economy = provider.getProvider();
        return economy != null;
    }

    public static CosmicJackpot get() {
        return instance;
    }

    public JackpotManager getJackpotManager() {
        return this.jackpotManager;
    }
}
