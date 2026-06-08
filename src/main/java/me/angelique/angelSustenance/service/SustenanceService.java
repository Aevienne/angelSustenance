package me.angelique.angelSustenance.service;

import me.angelique.angelSustenance.AngelSustenance;
import me.angelique.angelSustenance.config.PluginConfig;
import me.angelique.angelSustenance.model.ConfiguredEffect;
import me.angelique.angelSustenance.model.FoodCategory;
import me.angelique.angelSustenance.model.FoodEntry;
import me.angelique.angelSustenance.model.PlayerDietData;
import me.angelique.angelSustenance.storage.PlayerDietStorage;
import me.angelique.angelNCore.events.EventBus;
import me.angelique.angelNCore.events.PlayerDietChangedEvent;
import me.angelique.angelNCore.events.SeasonChangedEvent;
import me.angelique.angelNCore.services.NutritionService;
import me.angelique.angelNCore.services.ServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SustenanceService {

    private static final String SIDEBAR_OBJECTIVE = "asustain";

    private final AngelSustenance plugin;
    private final PluginConfig config;
    private final PlayerDietStorage storage;
    private final Map<UUID, PlayerDietData> diets = new HashMap<>();
    private final NamespacedKey customFoodKey;
    private int saveTaskId = -1;
    private int sidebarTaskId = -1;
    private boolean dirty;
    private SeasonChangedEvent.Season currentSeason = SeasonChangedEvent.Season.SPRING;

    public SustenanceService(AngelSustenance plugin, PluginConfig config, PlayerDietStorage storage) {
        this.plugin = plugin;
        this.config = config;
        this.storage = storage;
        this.customFoodKey = new NamespacedKey(plugin, "custom_food_id");
    }

    public void initialize() {
        diets.clear();
        diets.putAll(storage.load());
        startTasks();
        refreshSidebars();
    }

    public void shutdown() {
        if (saveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(saveTaskId);
            saveTaskId = -1;
        }
        if (sidebarTaskId != -1) {
            Bukkit.getScheduler().cancelTask(sidebarTaskId);
            sidebarTaskId = -1;
        }
        flush();
    }

    public void setCurrentSeason(SeasonChangedEvent.Season season) {
        this.currentSeason = season;
    }

    public void handleConsume(Player player, ItemStack itemStack) {
        if (player == null || itemStack == null || !config.isWorldEnabled(player.getWorld())) {
            return;
        }

        String foodId = resolveFoodId(itemStack);
        if (foodId == null || foodId.isBlank()) {
            return;
        }

        FoodCategory category = config.resolveCategory(foodId);

        if (currentSeason == SeasonChangedEvent.Season.WINTER
                && config.getWinterBlockedCategories().contains(category)) {
            player.sendMessage(config.getPrefix() + config.getWinterBlockedMessage()
                    .replace("{food}", foodId.toLowerCase().replace('_', ' ')));
            return;
        }
        PlayerDietData data = diets.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerDietData());
        data.getRecentFoods().addFirst(new FoodEntry(foodId, category, System.currentTimeMillis()));
        while (data.getRecentFoods().size() > config.getHistorySize()) {
            data.getRecentFoods().removeLast();
        }

        int scoreGain = calculateScoreGain(data, foodId, category);
        data.setDietScore(data.getDietScore() + scoreGain);

        NutritionService nutritionService = ServiceRegistry.getNutritionService();
        if (nutritionService != null && category != FoodCategory.OTHER) {
            nutritionService.recordMeal(player.getUniqueId(),
                    NutritionService.FoodCategory.valueOf(category.name()));
        }

        boolean balanced = isBalancedMeal(data);
        data.setBalancedMealActive(balanced);
        if (balanced && config.isBalancedMealEnabled()) {
            applyEffects(player, config.getBalancedEffects(), config.isAllowStacking());
            player.sendMessage(config.getPrefix() + config.getBalancedMessage());
        } else if (shouldApplyPenalty(data)) {
            applyEffects(player, config.getPenaltyEffects(), false);
            player.sendMessage(config.getPrefix() + config.getPenaltyMessage());
        }

        markDirty();
        refreshSidebar(player);

        EventBus.publish(new PlayerDietChangedEvent(player.getUniqueId(), data.getDietScore(), data.isBalancedMealActive()));
    }

    public PlayerDietData getData(Player player) {
        return diets.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerDietData());
    }

    public void refreshSidebars() {
        if (!config.isSidebarEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshSidebar(player);
        }
    }

    private void refreshSidebar(Player player) {
        if (!config.isSidebarEnabled() || !config.isWorldEnabled(player.getWorld())) {
            return;
        }
        PlayerDietData data = getData(player);
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(SIDEBAR_OBJECTIVE, Criteria.DUMMY, ChatColor.GOLD + "Sustenance");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.getScore(ChatColor.GREEN + "Diet Score").setScore(5);
        objective.getScore(safe(String.valueOf(data.getDietScore()), 4)).setScore(4);
        objective.getScore(ChatColor.AQUA + "Balanced").setScore(3);
        objective.getScore(safe(data.isBalancedMealActive() ? "YES" : "No", 2)).setScore(2);
        objective.getScore(ChatColor.YELLOW + "Last Food").setScore(1);
        String lastFood = data.getRecentFoods().isEmpty() ? "None" : data.getRecentFoods().peekFirst().getFoodId();
        objective.getScore(safe(lastFood, 0)).setScore(0);
        player.setScoreboard(scoreboard);
    }

    private String safe(String text, int unique) {
        String value = ChatColor.WHITE + text + ChatColor.values()[Math.min(unique, ChatColor.values().length - 1)];
        return value.length() > 40 ? value.substring(0, 40) : value;
    }

    private int calculateScoreGain(PlayerDietData data, String foodId, FoodCategory category) {
        int score = category == FoodCategory.OTHER ? 0 : 1;
        if (!config.isRepeatPenaltyEnabled()) {
            return score;
        }
        int duplicates = 0;
        for (FoodEntry entry : data.getRecentFoodsAsList()) {
            if (entry.getFoodId().equalsIgnoreCase(foodId)) {
                duplicates++;
            }
        }
        if (duplicates > config.getRepeatPenaltyThreshold()) {
            score = Math.max(0, score - (duplicates - config.getRepeatPenaltyThreshold()));
        }
        return score;
    }

    private boolean isBalancedMeal(PlayerDietData data) {
        if (!config.isBalancedMealEnabled()) {
            return false;
        }
        List<FoodEntry> recent = data.getRecentFoodsAsList();
        if (recent.isEmpty()) {
            return false;
        }
        Set<FoodCategory> seen = new HashSet<>();
        int limit = Math.min(config.getLookbackItems(), recent.size());
        for (int i = 0; i < limit; i++) {
            seen.add(recent.get(i).getCategory());
        }
        return data.getDietScore() >= config.getBaseScoreThreshold() && seen.containsAll(config.getRequiredCategories());
    }

    private boolean shouldApplyPenalty(PlayerDietData data) {
        if (!config.isPenaltiesEnabled()) {
            return false;
        }
        Map<String, Integer> counts = new HashMap<>();
        for (FoodEntry entry : data.getRecentFoods()) {
            counts.merge(entry.getFoodId(), 1, Integer::sum);
            if (counts.get(entry.getFoodId()) >= config.getTriggerDuplicateCount()) {
                return true;
            }
        }
        return false;
    }

    private void applyEffects(Player player, List<ConfiguredEffect> effects, boolean stack) {
        for (ConfiguredEffect effect : effects) {
            if (effect.getType() == null) {
                continue;
            }
            PotionEffect newEffect = effect.getType().createEffect(effect.getDurationSeconds() * 20, effect.getAmplifier());
            if (stack) {
                PotionEffect existing = player.getPotionEffect(effect.getType());
                if (existing != null) {
                    int extendedDuration = existing.getDuration() + newEffect.getDuration();
                    int amplifier = Math.max(existing.getAmplifier(), newEffect.getAmplifier());
                    player.addPotionEffect(effect.getType().createEffect(extendedDuration, amplifier), true);
                    continue;
                }
            }
            player.addPotionEffect(newEffect, true);
        }
    }

    private String resolveFoodId(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(customFoodKey, PersistentDataType.STRING)) {
            String custom = meta.getPersistentDataContainer().get(customFoodKey, PersistentDataType.STRING);
            if (custom != null && !custom.isBlank()) {
                return custom.toLowerCase();
            }
        }
        Material type = itemStack.getType();
        return type == null ? null : type.name();
    }

    private void startTasks() {
        saveTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::flush, config.getSaveIntervalTicks(), config.getSaveIntervalTicks());
        sidebarTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::refreshSidebars, 40L, 60L);
    }

    private void markDirty() {
        this.dirty = true;
    }

    private void flush() {
        if (!dirty) {
            return;
        }
        storage.save(diets);
        dirty = false;
    }
}
