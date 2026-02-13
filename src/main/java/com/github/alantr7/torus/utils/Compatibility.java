package com.github.alantr7.torus.utils;

import com.github.alantr7.bukkitplugin.versions.Version;
import com.github.alantr7.torus.log.Category;
import com.github.alantr7.torus.log.TorusLogger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.Collections;

public class Compatibility {

    public static final Version V1_21_4 = Version.from("1.21.4");

    public static void applyCustomModelData(ItemStack item, String cmd) {
        if (!item.hasItemMeta())
            return;

        ItemMeta meta = item.getItemMeta();
        if (Version.getServerVersion().isOlderThan(V1_21_4)) {
            try {
                meta.setCustomModelData(Integer.parseInt(cmd));
            } catch (Exception e) {
                TorusLogger.error(Category.ITEMS, "Custom model data is not a valid number: " + cmd);
            }
        } else {
            CustomModelDataComponent customModelDataComponent = meta.getCustomModelDataComponent();
            customModelDataComponent.setStrings(Collections.singletonList(cmd));
            meta.setCustomModelDataComponent(customModelDataComponent);
        }

        item.setItemMeta(meta);
    }



}
