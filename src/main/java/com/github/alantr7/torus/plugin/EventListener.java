package com.github.alantr7.torus.plugin;

import com.github.alantr7.bukkitplugin.annotations.core.Singleton;
import com.github.alantr7.bytils.buffer.ByteArrayReader;
import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.config.MainConfig;
import com.github.alantr7.torus.utils.EventUtils;
import com.github.alantr7.torus.item.TorusItem;
import com.github.alantr7.torus.utils.StringPool;
import com.github.alantr7.torus.player.TorusPlayer;
import com.github.alantr7.torus.structure.data.DataContainer;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.world.Pitch;
import com.github.alantr7.torus.world.TorusWorld;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

@Singleton
public class EventListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onMachinePlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        TorusPlayer player = TorusPlayer.get(event.getPlayer());
        if (player.placementCooldownExpiry > System.currentTimeMillis())
            return;

        TorusItem torusItem = TorusItem.getByItemStack(item);
        if (torusItem == null)
            return;

        if (!torusItem.isPlaceable()) {
            event.setCancelled(true);
            return;
        }

        player.placementCooldownExpiry = player.interactionCooldownExpiry = System.currentTimeMillis() + 200;

        if (MainConfig.WORLD_BLACKLIST.contains(event.getClickedBlock().getWorld().getName())) {
            event.getPlayer().sendMessage(ChatColor.RED + "Structures were disabled in this world by server owners.");
            event.setCancelled(true);
            return;
        }

        if (!event.getPlayer().isSneaking() && TorusWorld.isItemContainer(new BlockLocation(event.getClickedBlock().getLocation())))
            return;

        Block block = event.getClickedBlock().getRelative(event.getBlockFace());
        BlockLocation location = new BlockLocation(block.getLocation());

        Direction direction = event.getBlockFace().getModY() != 0
          ? Direction.fromBlockFace(event.getPlayer().getFacing()).getOpposite()
          : Direction.fromBlockFace(event.getBlockFace());

        Pitch pitch = !torusItem.getStructure().isOmnidirectional || event.getBlockFace().getModY() == 0
          ? Pitch.FORWARD
          : event.getBlockFace().getModY() == 1 ? Pitch.UP : Pitch.DOWN;

        if (torusItem.getStructure().isPlaceableAt(location, direction)) {
            if (!EventUtils.callStructurePrePlaceEvent(event.getPlayer(), torusItem.getStructure(), location, direction)) {
                return;
            }
            StructureInstance structure = torusItem.getStructure().place(location, direction, pitch);
            if (structure != null) {
                PersistentDataContainer structureData = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(TorusPlugin.getInstance(), "structure_data"), PersistentDataType.TAG_CONTAINER);
                if (structureData != null) {
                    ByteArrayReader stringsReader = new ByteArrayReader(
                      structureData.get(new NamespacedKey(TorusPlugin.getInstance(), "string_pool"), PersistentDataType.BYTE_ARRAY)
                    );
                    StringPool strings = new StringPool();
                    while (stringsReader.hasNext()) {
                        strings.pool(stringsReader.readString());
                    }

                    ByteArrayReader dataContainerReader = new ByteArrayReader(
                      structureData.get(new NamespacedKey(TorusPlugin.getInstance(), "data_container"), PersistentDataType.BYTE_ARRAY)
                    );
                    DataContainer.overwrite(structure.dataContainer, DataContainer.fromBytes(dataContainerReader, strings), structure.structure.portableData);
                }

                structure.setOwnerId(event.getPlayer().getUniqueId());
                if (structure.structure.isHeavy) {
                    structure.location.world.getBukkit().playSound(location.toBukkit().add(.5, 0, .5), Sound.BLOCK_ANVIL_PLACE, 0.2f, 1f);
                }
                if (event.getPlayer().getGameMode() == GameMode.SURVIVAL || event.getPlayer().getGameMode() == GameMode.ADVENTURE) {
                    item.setAmount(item.getAmount() - 1);
                }
                EventUtils.callStructurePlaceEvent(event.getPlayer(), structure);
            }
        } else {
            event.getPlayer().sendMessage(ChatColor.RED + "Not enough space to place the structure here.");
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    void onMachineInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        if (!TorusPlugin.getInstance().getWorldManager().isWorldSupported(event.getPlayer().getWorld()))
            return;

        TorusPlayer player = TorusPlayer.get(event.getPlayer());

        if (player.interactionCooldownExpiry > System.currentTimeMillis())
            return;

        BlockLocation clickedBlockLocation = new BlockLocation(event.getClickedBlock().getLocation());
        StructureInstance structure = clickedBlockLocation.getStructure();
        if (structure != null) {
            player.interactionCooldownExpiry = System.currentTimeMillis() + 200;

            if (event.getPlayer().isSneaking() && event.getItem() != null) {
                TorusItem torusItem = TorusItem.getByItemStack(event.getItem());
                if (torusItem != null) {
                    if (torusItem.isPlaceable())
                        return;
                } else if (event.getItem().getType().isBlock())
                    return;
            }

            if (structure.structure.isInteractable && structure.testOwnership(event.getPlayer())) {
                if (EventUtils.callStructureInteractEvent(event.getPlayer(), structure, clickedBlockLocation)) {
                    if (structure.onPlayerInteract(event, new BlockLocation(event.getClickedBlock().getLocation()))) {
                        player.placementCooldownExpiry = System.currentTimeMillis() + 200;
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    void onMachineBreak(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;

        if (!TorusPlugin.getInstance().getWorldManager().isWorldSupported(event.getPlayer().getWorld()))
            return;

        TorusWorld world = TorusPlugin.getInstance().getWorldManager().getWorld(event.getPlayer().getWorld());
        BlockLocation loc = new BlockLocation(event.getClickedBlock().getLocation());

        StructureInstance instance = world.getStructure(loc);
        if (instance == null)
            return;

        TorusPlayer player = TorusPlayer.get(event.getPlayer());
        if (player.interactionCooldownExpiry > System.currentTimeMillis()) {
            event.setCancelled(true);
            return;
        }

        player.interactionCooldownExpiry = System.currentTimeMillis() + 200;

        if (instance.structure.isHeavy) {
            TorusItem item = TorusItem.getByItemStack(event.getPlayer().getInventory().getItemInMainHand());
            if (item == null || !item.namespacedId.equals("torus:hammer")) {
                event.getPlayer().sendMessage(ChatColor.RED + "This structure can be broken only with hammer.");
                event.setCancelled(true);
                return;
            }
        }

        if (instance.testOwnership(event.getPlayer())) {
            if (!EventUtils.callStructureBreakEvent(event.getPlayer(), instance)) {
                return;
            }
            if (event.getPlayer().getGameMode() == GameMode.SURVIVAL || event.getPlayer().getGameMode() == GameMode.ADVENTURE) {
                ItemStack drop = instance.toItem(true);
                if (drop != null) {
                    world.getBukkit().dropItem(loc.toBukkit().add(.5, 0, .5), drop);
                }
            }
            world.removeStructure(instance);
            if (instance.structure.isHeavy) {
                world.getBukkit().playSound(loc.toBukkit().add(.5, 0, .5), Sound.BLOCK_ANVIL_USE, 0.2f, 1.2f);
            }
        } else {
            event.getPlayer().sendMessage(ChatColor.RED + "You can not break a structure that you do not own.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onMachineBreak(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof Interaction interaction))
            return;

        // TODO: Code is duplicate of the one above

        if (!TorusPlugin.getInstance().getWorldManager().isWorldSupported(player.getWorld()))
            return;

        TorusWorld world = TorusPlugin.getInstance().getWorldManager().getWorld(player.getWorld());
        BlockLocation loc = new BlockLocation(interaction.getLocation());

        StructureInstance instance = world.getStructure(loc);
        if (instance == null)
            return;

        TorusPlayer torusPlayer = TorusPlayer.get(player);
        if (torusPlayer.interactionCooldownExpiry > System.currentTimeMillis()) {
            event.setCancelled(true);
            return;
        }

        torusPlayer.interactionCooldownExpiry = System.currentTimeMillis() + 200;

        if (instance.structure.isHeavy) {
            TorusItem item = TorusItem.getByItemStack(player.getInventory().getItemInMainHand());
            if (item == null || !item.namespacedId.equals("torus:hammer")) {
                player.sendMessage(ChatColor.RED + "This structure can be broken only with hammer.");
                event.setCancelled(true);
                return;
            }
        }

        if (instance.testOwnership(player)) {
            if (!EventUtils.callStructureBreakEvent(player, instance)) {
                return;
            }
            if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                ItemStack drop = instance.toItem(true);
                if (drop != null) {
                    world.getBukkit().dropItem(loc.toBukkit().add(.5, 0, .5), drop);
                }
            }
            world.removeStructure(instance);
            if (instance.structure.isHeavy) {
                world.getBukkit().playSound(loc.toBukkit().add(.5, 0, .5), Sound.BLOCK_ANVIL_USE, 0.2f, 1.2f);
            }
        } else {
            player.sendMessage(ChatColor.RED + "You can not break a structure that you do not own.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onTorusEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getScoreboardTags().contains("torus_entity")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onLeashDrop(EntityUnleashEvent event) {
        if (event.getEntity().getScoreboardTags().contains("torus_entity")) {
            event.setDropLeash(false);

            if (((LivingEntity) event.getEntity()).getLeashHolder() instanceof Player player) {
                TorusPlayer.get(player).pendingWireConnection = null;
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onSwitchHotbarItem(PlayerItemHeldEvent event) {
        if (!TorusItem.is(event.getPlayer().getInventory().getItem(event.getNewSlot()), "torus:copper_wire")) {
            TorusPlayer.get(event.getPlayer()).abortWireConnection();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onSwitchHotbarItem(InventoryClickEvent event) {
        Bukkit.getScheduler().runTaskLater(TorusPlugin.getInstance(), () -> {
            if (!TorusItem.is(event.getWhoClicked().getInventory().getItemInMainHand(), "torus:copper_wire")) {
                TorusPlayer.get((Player) event.getWhoClicked()).abortWireConnection();
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onSwitchHotbarItem(InventoryDragEvent event) {
        Bukkit.getScheduler().runTaskLater(TorusPlugin.getInstance(), () -> {
            if (!TorusItem.is(event.getWhoClicked().getInventory().getItemInMainHand(), "torus:copper_wire")) {
                TorusPlayer.get((Player) event.getWhoClicked()).abortWireConnection();
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onSwitchHotbarItem(PlayerSwapHandItemsEvent event) {
        Bukkit.getScheduler().runTaskLater(TorusPlugin.getInstance(), () -> {
            if (!TorusItem.is(event.getMainHandItem(), "torus:copper_wire")) {
                TorusPlayer.get(event.getPlayer()).abortWireConnection();
            }
        }, 1L);
    }

    @EventHandler
    void onUseTorusItemInVanillaCraftingRecipe(PrepareItemCraftEvent event) {
        if (!(event.getRecipe() instanceof CraftingRecipe recipe))
            return;

        if (!recipe.getKey().getNamespace().equals("minecraft"))
            return;

        for (ItemStack stack : event.getInventory().getMatrix()) {
            if (stack == null)
                continue;

            if (TorusItem.getByItemStack(stack) != null) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler
    void onUseTorusItemInVanillaCraftingRecipe(CraftItemEvent event) {
        if (!(event.getRecipe() instanceof CraftingRecipe recipe))
            return;

        if (!recipe.getKey().getNamespace().equals("minecraft"))
            return;

        for (ItemStack stack : event.getInventory().getMatrix()) {
            if (stack == null)
                continue;

            if (TorusItem.getByItemStack(stack) != null) {
                event.setCancelled(true);
                event.getInventory().setResult(null);
                return;
            }
        }
    }

}
