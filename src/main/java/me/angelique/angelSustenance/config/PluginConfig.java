package me.angelique.angelSustenance.config;

import me.angelique.angelSustenance.model.ConfiguredEffect;
import me.angelique.angelSustenance.model.FoodCategory;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PluginConfig {

    private final boolean sidebarEnabled;
    private final long saveIntervalTicks;
    private final Set<String> enabledWorlds;
    private final boolean useAllWorldsWhenEmpty;
    private final int historySize;
    private final int repeatPenaltyThreshold;
    private final boolean repeatPenaltyEnabled;
    private final boolean customFoodSupport;
    private final boolean balancedMealEnabled;
    private final List<FoodCategory> requiredCategories;
    private final int lookbackItems;
    private final int baseScoreThreshold;
    private final int buffDurationSeconds;
    private final boolean allowStacking;
    private final String sidebarLabel;
    private final String balancedMessage;
    private final boolean penaltiesEnabled;
    private final int triggerDuplicateCount;
    private final String penaltyMessage;
    private final Map<FoodCategory, Set<String>> categories = new EnumMap<>(FoodCategory.class);
    private final Map<String, FoodCategory> customFoods = new HashMap<>();
    private final List<ConfiguredEffect> balancedEffects;
    private final List<ConfiguredEffect> penaltyEffects;
    private final String prefix;
    private final String status;
    private final String reload;

    public PluginConfig(FileConfiguration config) {
        this.sidebarEnabled = config.getBoolean("general.sidebar-enabled", true);
        this.saveIntervalTicks = Math.max(20L, config.getLong("general.save-interval-seconds", 60L) * 20L);
        this.enabledWorlds = new HashSet<>(config.getStringList("general.enabled-worlds"));
        this.useAllWorldsWhenEmpty = config.getBoolean("general.use-all-worlds-when-empty", true);
        this.historySize = Math.max(3, config.getInt("general.history-size", 6));
        this.repeatPenaltyThreshold = Math.max(1, config.getInt("general.repeat-penalty-threshold", 2));
        this.repeatPenaltyEnabled = config.getBoolean("general.repeat-penalty-enabled", true);
        this.customFoodSupport = config.getBoolean("general.custom-food-support", true);
        this.balancedMealEnabled = config.getBoolean("balanced-meal.enabled", true);
        this.requiredCategories = loadRequiredCategories(config.getStringList("balanced-meal.required-categories"));
        this.lookbackItems = Math.max(3, config.getInt("balanced-meal.lookback-items", 4));
        this.baseScoreThreshold = Math.max(1, config.getInt("balanced-meal.base-score-threshold", 3));
        this.buffDurationSeconds = Math.max(1, config.getInt("balanced-meal.buff-duration-seconds", 600));
        this.allowStacking = config.getBoolean("balanced-meal.allow-stacking", true);
        this.sidebarLabel = color(config.getString("balanced-meal.sidebar-label", "&aBalanced Meal"));
        this.balancedMessage = color(config.getString("balanced-meal.message", "&aBalanced meal achieved! You feel nourished."));
        this.penaltiesEnabled = config.getBoolean("penalties.enabled", true);
        this.triggerDuplicateCount = Math.max(2, config.getInt("penalties.trigger-duplicate-count", 3));
        this.penaltyMessage = color(config.getString("penalties.message", "&eYour diet is becoming repetitive."));
        loadCategories(config.getConfigurationSection("categories"));
        loadCustomFoods(config.getConfigurationSection("custom-foods"));
        this.balancedEffects = loadEffects(config.getMapList("buffs.balanced"));
        this.penaltyEffects = loadEffects(config.getMapList("penalties.effects"));
        this.prefix = color(config.getString("messages.prefix", "&6[AngelSustenance] &r"));
        this.status = color(config.getString("messages.status", "&eDiet Score: &f{score} &7| &eBalanced: &f{balanced} &7| &eLast Food: &f{food}"));
        this.reload = color(config.getString("messages.reload", "&aAngelSustenance reloaded."));
    }

    private List<FoodCategory> loadRequiredCategories(List<String> names) {
        List<FoodCategory> categories = new ArrayList<>();
        for (String name : names) {
            try {
                categories.add(FoodCategory.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return categories;
    }

    private void loadCategories(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                FoodCategory category = FoodCategory.valueOf(key.toUpperCase());
                Set<String> values = new HashSet<>();
                for (String value : section.getStringList(key)) {
                    values.add(value.toUpperCase());
                }
                categories.put(category, values);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void loadCustomFoods(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String categoryName = section.getString(key + ".category", "OTHER");
            try {
                customFoods.put(key.toLowerCase(), FoodCategory.valueOf(categoryName.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                customFoods.put(key.toLowerCase(), FoodCategory.OTHER);
            }
        }
    }

    private List<ConfiguredEffect> loadEffects(List<Map<?, ?>> rawList) {
        List<ConfiguredEffect> effects = new ArrayList<>();
        for (Map<?, ?> map : rawList) {
            Object typeObject = map.get("type");
            if (typeObject == null) {
                continue;
            }
            PotionEffectType type = PotionEffectType.getByName(String.valueOf(typeObject).toUpperCase());
            if (type == null) {
                continue;
            }
            int durationSeconds = Math.max(1, parseInt(map.get("duration-seconds"), 60));
            int amplifier = Math.max(0, parseInt(map.get("amplifier"), 0));
            effects.add(new ConfiguredEffect(type, durationSeconds, amplifier));
        }
        return effects;
    }

    private int parseInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    public boolean isWorldEnabled(World world) {
        if (useAllWorldsWhenEmpty && enabledWorlds.isEmpty()) {
            return true;
        }
        return enabledWorlds.contains(world.getName());
    }

    public FoodCategory resolveCategory(String foodId) {
        if (foodId == null || foodId.isBlank()) {
            return FoodCategory.OTHER;
        }
        String normalized = foodId.toUpperCase();
        for (Map.Entry<FoodCategory, Set<String>> entry : categories.entrySet()) {
            if (entry.getValue().contains(normalized)) {
                return entry.getKey();
            }
        }
        if (customFoodSupport) {
            return customFoods.getOrDefault(foodId.toLowerCase(), FoodCategory.OTHER);
        }
        return FoodCategory.OTHER;
    }

    public boolean isSidebarEnabled() { return sidebarEnabled; }
    public long getSaveIntervalTicks() { return saveIntervalTicks; }
    public int getHistorySize() { return historySize; }
    public int getRepeatPenaltyThreshold() { return repeatPenaltyThreshold; }
    public boolean isRepeatPenaltyEnabled() { return repeatPenaltyEnabled; }
    public boolean isBalancedMealEnabled() { return balancedMealEnabled; }
    public List<FoodCategory> getRequiredCategories() { return requiredCategories; }
    public int getLookbackItems() { return lookbackItems; }
    public int getBaseScoreThreshold() { return baseScoreThreshold; }
    public int getBuffDurationSeconds() { return buffDurationSeconds; }
    public boolean isAllowStacking() { return allowStacking; }
    public String getSidebarLabel() { return sidebarLabel; }
    public String getBalancedMessage() { return balancedMessage; }
    public boolean isPenaltiesEnabled() { return penaltiesEnabled; }
    public int getTriggerDuplicateCount() { return triggerDuplicateCount; }
    public String getPenaltyMessage() { return penaltyMessage; }
    public List<ConfiguredEffect> getBalancedEffects() { return balancedEffects; }
    public List<ConfiguredEffect> getPenaltyEffects() { return penaltyEffects; }
    public String getPrefix() { return prefix; }
    public String getStatus() { return status; }
    public String getReload() { return reload; }
}
