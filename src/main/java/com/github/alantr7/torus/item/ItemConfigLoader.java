package com.github.alantr7.torus.item;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.api.addon.TorusAddon;
import com.github.alantr7.torus.log.TorusLogger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ItemConfigLoader {

    public static void load(TorusAddon addon) {
        File[] files = addon.itemsDirectory.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            for (String itemId : yaml.getKeys(false)) {
                TorusItem torusItem = TorusPlugin.getInstance().getItemRegistry().getItemById(addon.id + ":" + itemId);
                if (torusItem == null) {
                    TorusLogger.error(com.github.alantr7.torus.log.Category.ITEMS, "Item with ID '%s' not found.".formatted(itemId));
                    continue;
                }

                ConfigurationSection data = yaml.getConfigurationSection(itemId);
                if (!data.isString("base")) {
                    TorusLogger.error(com.github.alantr7.torus.log.Category.ITEMS, "Base item is not set.");
                    continue;
                }

                ItemReference base = ItemReference.parse(data.getString("base"));
                String headTexture = data.getString("head_texture_url");
                String name = data.getString("name");
                String customModelData = data.getString("custom_model_data");
                List<String> categoriesRaw = data.getStringList("categories");
                List<String> lore = data.getStringList("lore");

                ItemStack baseItem = base.getItem();
                if (baseItem == null) {
                    TorusLogger.error(com.github.alantr7.torus.log.Category.ITEMS, "Base item with ID '%s' not found.".formatted(data.getString("base")));
                    continue;
                }

                List<Category> categories = new ArrayList<>();
                for (String categoryId : categoriesRaw) {
                    Category category = TorusPlugin.getInstance().getItemRegistry().getCategory(categoryId);
                    if (category == null) {
                        TorusLogger.error(com.github.alantr7.torus.log.Category.ITEMS, "Invalid category: " + categoryId);
                        continue;
                    }

                    categories.add(category);
                }

                torusItem.setCategories(categories.toArray(Category[]::new));
                torusItem.setBaseItem(
                  baseItem.getType() == Material.PLAYER_HEAD && headTexture != null ? new HeadData(headTexture).stack : base.getItem(), name, lore, customModelData
                );
            }
        }
    }

}
