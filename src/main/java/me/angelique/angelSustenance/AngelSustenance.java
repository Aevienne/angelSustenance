package me.angelique.angelSustenance;

import me.angelique.angelSustenance.command.SustenanceCommand;
import me.angelique.angelSustenance.config.PluginConfig;
import me.angelique.angelSustenance.listener.FoodConsumeListener;
import me.angelique.angelSustenance.service.SustenanceService;
import me.angelique.angelSustenance.storage.PlayerDietStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public final class AngelSustenance extends JavaPlugin {

    private PluginConfig pluginConfig;
    private PlayerDietStorage playerDietStorage;
    private SustenanceService sustenanceService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPlugin();
    }

    public void reloadPlugin() {
        HandlerList.unregisterAll(this);
        reloadConfig();

        this.pluginConfig = new PluginConfig(getConfig());
        this.playerDietStorage = new PlayerDietStorage(this);

        if (this.sustenanceService != null) {
            this.sustenanceService.shutdown();
        }

        this.sustenanceService = new SustenanceService(this, pluginConfig, playerDietStorage);
        this.sustenanceService.initialize();

        Bukkit.getPluginManager().registerEvents(new FoodConsumeListener(pluginConfig, sustenanceService), this);

        PluginCommand command = getCommand("sustenance");
        if (command != null) {
            SustenanceCommand executor = new SustenanceCommand(this, pluginConfig, sustenanceService);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        if (sustenanceService != null) {
            sustenanceService.shutdown();
        }
    }
}
