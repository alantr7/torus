package com.github.alantr7.torus.hook;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.github.alantr7.bukkitplugin.annotations.core.Inject;
import com.github.alantr7.bukkitplugin.annotations.core.Invoke;
import com.github.alantr7.bukkitplugin.annotations.core.RequiresPlugin;
import com.github.alantr7.bukkitplugin.annotations.core.Singleton;
import com.github.alantr7.bukkitplugin.versions.Version;
import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.item.TorusItem;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.utils.Compatibility;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.TorusWorldManager;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;

@Singleton
@RequiresPlugin("ProtocolLib")
public class ProtocolLibHook {

    @Inject
    TorusWorldManager worldManager;

    @Invoke(Invoke.Schedule.AFTER_PLUGIN_ENABLE)
    void registerListener() {
        if (TorusPlugin.getInstance().getVersion().isOlderThan(Compatibility.V1_21_4))
            return;

        registerItemPickupFromBlockPacketListener();
    }

    private void registerItemPickupFromBlockPacketListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(TorusPlugin.getInstance(), ListenerPriority.NORMAL, PacketType.Play.Client.PICK_ITEM_FROM_BLOCK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!worldManager.isWorldSupported(event.getPlayer().getWorld()))
                    return;

                GameMode gameMode = event.getPlayer().getGameMode();
                if (gameMode != GameMode.CREATIVE && gameMode != GameMode.SURVIVAL)
                    return;

                BlockPosition data = event.getPacket().getBlockPositionModifier().getValues().getFirst();
                boolean includeData = event.getPacket().getBooleans().getValues().getFirst();
                StructureInstance instance = worldManager.getWorld(event.getPlayer().getWorld()).getStructure(new BlockLocation(
                  worldManager.getWorld(event.getPlayer().getWorld()), data.getX(), data.getY(), data.getZ()
                ));

                if (instance == null)
                    return;

                TorusItem item = TorusPlugin.getInstance().getItemRegistry().getItemByStructure(instance.structure);
                if (item == null)
                    return;

                event.setCancelled(true);

                if (gameMode == GameMode.CREATIVE && includeData) {
                    event.getPlayer().getInventory().setItemInMainHand(instance.toItem(true));
                    return;
                }

                for (int i = 0; i < 9; i++) {
                    ItemStack hotbarItem = event.getPlayer().getInventory().getItem(i);
                    if (TorusItem.is(hotbarItem, item.namespacedId)) {
                        event.getPlayer().getInventory().setHeldItemSlot(i);
                        return;
                    }
                }

                for (int i = 9; i < 36; i++) {
                    if (TorusItem.is(event.getPlayer().getInventory().getItem(i), item.namespacedId)) {
                        ItemStack structureItem = event.getPlayer().getInventory().getItem(i);
                        event.getPlayer().getInventory().setItem(i, event.getPlayer().getInventory().getItemInMainHand());
                        event.getPlayer().getInventory().setItemInMainHand(structureItem);
                        return;
                    }
                }

                if (gameMode == GameMode.CREATIVE) {
                    event.getPlayer().getInventory().setItemInMainHand(item.toItemStack().clone());
                }
            }
        });
    }

    @Invoke(Invoke.Schedule.AFTER_PLUGIN_DISABLE)
    void unregisterListener() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(TorusPlugin.getInstance());
    }

}
