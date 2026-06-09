package me.angelique.angelSustenance.gui;

import me.angelique.angelSustenance.AngelSustenance;
import me.angelique.angelSustenance.model.PlayerDietData;
import me.angelique.angelNCore.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public final class SustenanceGui {

    public static final String TITLE = TextUtil.color("&8Nutrition &7\u2014 &cDiet Tracker");
    static final int SIZE = 27;

    private SustenanceGui() {}

    public static void open(Player player, AngelSustenance plugin) {
        PlayerDietData data = plugin.getSustenanceService().getData(player);
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        fillBorder(inv);

        int score = data.getDietScore();
        boolean balanced = data.isBalancedMealActive();

        String scoreBar = "&a\u2588".repeat(Math.min(score / 10, 10)) + "&7\u2588".repeat(Math.max(10 - score / 10, 0));

        inv.setItem(11, item(Material.GOLDEN_APPLE, "&cDiet Score: &f" + score,
                "&7" + scoreBar,
                "&7Eat varied foods to improve score",
                "&7Categories: &fPROTEIN, GRAINS, FRUITS, VEGETABLES",
                balanced ? "&aBalanced Meal Active!" : "&7No balanced meal active"));

        inv.setItem(13, item(Material.COOKED_BEEF, "&eRecent Foods",
                data.getRecentFoodsAsList().stream()
                    .limit(5)
                    .map(f -> "&7\u2022 &f" + f.getFoodId() + " &8(" + f.getCategory().name() + ")")
                    .toArray(String[]::new)));

        inv.setItem(15, item(Material.BOOK, "&eDiet Tips",
                "&7\u2022 Eat all 4 categories for balanced meal",
                "&7\u2022 Avoid repeating same food",
                "&7\u2022 High score = better buffs",
                "&7\u2022 Winter blocks some food categories"));

        player.openInventory(inv);
    }

    static void fillBorder(Inventory inv) {
        ItemStack glass = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, glass);
    }

    static ItemStack item(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TextUtil.color(name));
            meta.setLore(Arrays.stream(lore).map(TextUtil::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    static ItemStack pane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }
}
