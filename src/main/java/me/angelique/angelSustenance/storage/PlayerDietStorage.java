package me.angelique.angelSustenance.storage;

import me.angelique.angelSustenance.AngelSustenance;
import me.angelique.angelSustenance.model.FoodCategory;
import me.angelique.angelSustenance.model.FoodEntry;
import me.angelique.angelSustenance.model.PlayerDietData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerDietStorage {

    private final AngelSustenance plugin;
    private final File file;

    public PlayerDietStorage(AngelSustenance plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player-diets.yml");
    }

    public Map<UUID, PlayerDietData> load() {
        Map<UUID, PlayerDietData> result = new HashMap<>();
        if (!file.exists()) {
            return result;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("players");
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String base = "players." + key;
                PlayerDietData data = new PlayerDietData();
                data.setDietScore(yaml.getInt(base + ".diet-score", 0));
                data.setBalancedMealActive(yaml.getBoolean(base + ".balanced-active", false));
                ConfigurationSection history = yaml.getConfigurationSection(base + ".history");
                if (history != null) {
                    for (String index : history.getKeys(false)) {
                        String path = base + ".history." + index;
                        String foodId = yaml.getString(path + ".food-id", "AIR");
                        String categoryName = yaml.getString(path + ".category", "OTHER");
                        long consumedAt = yaml.getLong(path + ".consumed-at", System.currentTimeMillis());
                        FoodCategory category;
                        try {
                            category = FoodCategory.valueOf(categoryName.toUpperCase());
                        } catch (IllegalArgumentException ignored) {
                            category = FoodCategory.OTHER;
                        }
                        data.getRecentFoods().addLast(new FoodEntry(foodId, category, consumedAt));
                    }
                }
                result.put(uuid, data);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    public void save(Map<UUID, PlayerDietData> dataMap) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerDietData> entry : dataMap.entrySet()) {
            String base = "players." + entry.getKey();
            PlayerDietData data = entry.getValue();
            yaml.set(base + ".diet-score", data.getDietScore());
            yaml.set(base + ".balanced-active", data.isBalancedMealActive());
            int index = 0;
            for (FoodEntry foodEntry : data.getRecentFoods()) {
                String path = base + ".history." + index;
                yaml.set(path + ".food-id", foodEntry.getFoodId());
                yaml.set(path + ".category", foodEntry.getCategory().name());
                yaml.set(path + ".consumed-at", foodEntry.getConsumedAt());
                index++;
            }
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder.");
                return;
            }
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save player-diets.yml: " + exception.getMessage());
        }
    }
}
