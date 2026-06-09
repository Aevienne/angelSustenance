package me.angelique.angelSustenance.command;

import me.angelique.angelSustenance.AngelSustenance;
import me.angelique.angelSustenance.config.PluginConfig;
import me.angelique.angelSustenance.gui.SustenanceGui;
import me.angelique.angelSustenance.model.PlayerDietData;
import me.angelique.angelSustenance.service.SustenanceService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class SustenanceCommand implements CommandExecutor, TabCompleter {

    private final AngelSustenance plugin;
    private final PluginConfig config;
    private final SustenanceService sustenanceService;

    public SustenanceCommand(AngelSustenance plugin, PluginConfig config, SustenanceService sustenanceService) {
        this.plugin = plugin;
        this.config = config;
        this.sustenanceService = sustenanceService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(config.getPrefix() + "/sustenance check <player>");
                return true;
            }
            SustenanceGui.open(player, plugin);
            return true;
        }

        if ("status".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(config.getPrefix() + "/sustenance check <player>");
                return true;
            }
            sendStatus(sender, player, player.getName());
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("angelsustenance.admin")) {
                sender.sendMessage(config.getPrefix() + "No permission.");
                return true;
            }
            plugin.reloadPlugin();
            sender.sendMessage(config.getPrefix() + config.getReload());
            return true;
        }

        if ("check".equalsIgnoreCase(args[0]) && args.length >= 2) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.isOnline()) {
                sender.sendMessage(config.getPrefix() + "Player must be online.");
                return true;
            }
            sendStatus(sender, target.getPlayer(), target.getName() == null ? args[1] : target.getName());
            return true;
        }

        sender.sendMessage(config.getPrefix() + "/sustenance status");
        sender.sendMessage(config.getPrefix() + "/sustenance check <player>");
        sender.sendMessage(config.getPrefix() + "/sustenance reload");
        return true;
    }

    private void sendStatus(CommandSender sender, Player player, String name) {
        PlayerDietData data = sustenanceService.getData(player);
        String lastFood = data.getRecentFoods().isEmpty() ? "None" : data.getRecentFoods().peekFirst().getFoodId();
        sender.sendMessage(config.getPrefix() + name + ": " + config.getStatus()
                .replace("{score}", String.valueOf(data.getDietScore()))
                .replace("{balanced}", data.isBalancedMealActive() ? "YES" : "No")
                .replace("{food}", lastFood));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("status");
            suggestions.add("check");
            suggestions.add("reload");
        } else if (args.length == 2 && "check".equalsIgnoreCase(args[0])) {
            Bukkit.getOnlinePlayers().forEach(player -> suggestions.add(player.getName()));
        }
        return suggestions;
    }
}
