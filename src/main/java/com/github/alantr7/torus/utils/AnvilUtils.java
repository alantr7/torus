package com.github.alantr7.torus.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.github.alantr7.bukkitplugin.annotations.core.Invoke;
import com.github.alantr7.bukkitplugin.annotations.core.Singleton;
import com.github.alantr7.torus.TorusPlugin;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@Singleton
public class AnvilUtils {

    @Invoke(Invoke.Schedule.AFTER_PLUGIN_ENABLE)
    void init() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(TorusPlugin.getInstance(), PacketType.Play.Client.ITEM_NAME) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                String name = event.getPacket().getStrings().read(0);
                if (event.getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof PendingRename holder) {
                    holder.name = name;
                }
            }
        });
    }

    @EventHandler
    void onInteract(InventoryClickEvent event) {
        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof PendingRename holder))
            return;

        event.setCancelled(true);
        if (event.getSlot() == 2) {
            holder.adapter.onRename(holder.name);
            event.getClickedInventory().close();
        }
    }

    public static void requestRename(Player player, String name, RenameAdapter adapter) {
        PendingRename inventoryHolder = new PendingRename();
        inventoryHolder.inventory = Bukkit.createInventory(inventoryHolder, InventoryType.ANVIL, "Enter floor name:");
        inventoryHolder.adapter = adapter;

        ItemStack inputItem = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = inputItem.getItemMeta();
        meta.setItemName(name);
        inputItem.setItemMeta(meta);

        inventoryHolder.name = name;
        inventoryHolder.inventory.addItem(inputItem);
        player.openInventory(inventoryHolder.inventory);
    }

    public static class RenameAdapter {

        public void onRename(String newName) {}

    }

    static class PendingRename implements InventoryHolder {

        String name;

        @Getter
        Inventory inventory;

        RenameAdapter adapter;

    }

}
