package me.angelique.angelSustenance.listener;

import me.angelique.angelSustenance.config.PluginConfig;
import me.angelique.angelSustenance.service.SustenanceService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public final class FoodConsumeListener implements Listener {

    private final PluginConfig config;
    private final SustenanceService sustenanceService;

    public FoodConsumeListener(PluginConfig config, SustenanceService sustenanceService) {
        this.config = config;
        this.sustenanceService = sustenanceService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!config.isWorldEnabled(event.getPlayer().getWorld())) {
            return;
        }
        sustenanceService.handleConsume(event.getPlayer(), event.getItem());
    }
}
