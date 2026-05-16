package me.angelique.angelSustenance.listener;

import me.angelique.angelNCore.events.SeasonChangedEvent;
import me.angelique.angelSustenance.service.SustenanceService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class SeasonListener implements Listener {

    private final SustenanceService sustenanceService;

    public SeasonListener(SustenanceService sustenanceService) {
        this.sustenanceService = sustenanceService;
    }

    @EventHandler
    public void onSeasonChange(SeasonChangedEvent event) {
        sustenanceService.setCurrentSeason(event.getNewSeason());
    }
}
