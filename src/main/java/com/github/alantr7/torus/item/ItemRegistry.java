package com.github.alantr7.torus.item;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.structure.Structure;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ItemRegistry {

    private final Map<String, TorusItem> registry = new HashMap<>();

    private final Map<String, Category> categories = new LinkedHashMap<>();

    {
        categories.put("torus:general", Category.GENERAL);
        categories.put("torus:resources", Category.RESOURCES);
        categories.put("torus:generators", Category.GENERATORS);
        categories.put("torus:machines", Category.MACHINES);
        categories.put("torus:network", Category.NETWORK);
        categories.put("torus:components", Category.COMPONENTS);
        categories.put("torus:storage", Category.STORAGE);
        categories.put("torus:tools", Category.TOOLS);
    }

    public void clear() {
        registry.clear();
        for (Category category : categories.values()) {
            category.items.clear();
        }
    }

    public void registerItem(TorusItem item) {
        registry.put(item.namespacedId, item);
        for (Category category : item.categories) {
            category.items.add(item);
        }
        item.addon.registerContent(item);
    }

    public TorusItem getItemById(String id) {
        if (id.startsWith("torus:"))
            return registry.get(id);
        return registry.get("torus:" + id);
    }

    public TorusItem getItemByItemStack(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;

        String itemId = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(TorusPlugin.getInstance(), "torus_item"), PersistentDataType.STRING);
        if (itemId == null)
            return null;

        return registry.get(itemId);
    }

    public TorusItem getItemByStructure(Structure structure) {
        for (TorusItem item : getItems()) {
            if (item.structure == structure)
                return item;
        }
        return null;
    }

    // TODO: Implement item providers system to support other plugins
    public ItemStack getItemStackByReference(ItemReference reference) {
        if (reference.providerId.equals("minecraft")) {
            try {
                return new ItemStack(Material.valueOf(reference.itemId));
            } catch (Exception ex) {
                return null;
            }
        }
        TorusItem item = getItemById("torus:" + reference.itemId.toLowerCase());
        return item != null ? item.toItemStack().clone() : null;
    }

    @NotNull
    public ItemReference createItemReference(@NotNull ItemStack item) {
        // TODO: Implement item providers system to support other plugins
        TorusItem torusItem = getItemByItemStack(item);
        return torusItem != null
          ? new ItemReference("torus", torusItem.namespacedId.substring("torus:".length()))
          : new ItemReference("minecraft", item.getType().name());
    }

    public Collection<String> getItemIds() {
        return registry.keySet();
    }

    public Collection<TorusItem> getItems() {
        return registry.values();
    }

    public Category getCategory(String id) {
        return categories.get(id);
    }

    public Collection<Category> getCategories() {
        return categories.values();
    }

}
