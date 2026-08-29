package com.github.alantr7.torus.integration.worldguard;

import com.github.alantr7.bukkitplugin.annotations.core.RequiresPlugin;
import com.github.alantr7.bukkitplugin.annotations.core.Singleton;
import com.github.alantr7.torus.api.event.PlayerStructureInteractEvent;
import com.github.alantr7.torus.api.event.PlayerStructurePrePlaceEvent;
import com.github.alantr7.torus.config.MainConfig;
import com.github.alantr7.torus.log.Category;
import com.github.alantr7.torus.log.TorusLogger;
import com.github.alantr7.torus.utils.MathUtils;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import static com.github.alantr7.torus.lang.Localization.translate;

@Singleton
@RequiresPlugin("WorldGuard")
public class WorldGuardIntEntryPoint {

    static StateFlag FLAG_TORUS_STRUCTURE_PLACE;

    static StateFlag FLAG_TORUS_STRUCTURE_BREAK;

    static StateFlag FLAG_TORUS_STRUCTURE_INTERACT;

    public static void initialize() {
        if (MainConfig.INTEGRATION_BLACKLIST.contains("WorldGuard"))
            return;

        FlagRegistry flagRegistry = WorldGuard.getInstance().getFlagRegistry();
        FLAG_TORUS_STRUCTURE_PLACE      = registerOrGetExisting(flagRegistry, "torus-structure-place");
        FLAG_TORUS_STRUCTURE_BREAK      = registerOrGetExisting(flagRegistry, "torus-structure-break");
        FLAG_TORUS_STRUCTURE_INTERACT   = registerOrGetExisting(flagRegistry, "torus-structure-interact");

        TorusLogger.info(Category.INTEGRATIONS, "WorldGuard integration succeeded.");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    void preventPlacingStructuresInRegionsWithDeniedFlag(PlayerStructurePrePlaceEvent event) {
        if (event.getPlayer().asBukkit().hasPermission("worldguard.region.bypass." + event.getLocation().world.getBukkit().getName())
        || MainConfig.INTEGRATION_BLACKLIST.contains("WorldGuard"))
            return;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        LocalPlayer player = WorldGuardPlugin.inst().wrapPlayer(event.getPlayer().asBukkit());
        byte[] collision = MathUtils.rotateVectors(event.getStructure().getCollisionVectors(), event.getDirection().getOpposite());
        byte[] offset = MathUtils.rotateVectors(event.getStructure().getOffset(), event.getDirection().getOpposite());
        for (int i = 0; i < collision.length; i += 3) {
            Location location = BukkitAdapter.adapt(event.getLocation().getRelative(collision[i] + offset[0], collision[i + 1] + offset[1], collision[i + 2] + offset[2]).toBukkit());
            if (!query.testState(location, player, FLAG_TORUS_STRUCTURE_PLACE)  || !query.testBuild(location, player, Flags.BLOCK_PLACE)) {
                event.getPlayer().asBukkit().sendMessage(translate("interaction.region.no_access"));
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    void preventBreakingStructuresInRegionsWithDeniedFlag(PlayerStructurePrePlaceEvent event) {
        if (event.getPlayer().asBukkit().hasPermission("worldguard.region.bypass." + event.getLocation().world.getBukkit().getName())
        || MainConfig.INTEGRATION_BLACKLIST.contains("WorldGuard"))
            return;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        LocalPlayer player = WorldGuardPlugin.inst().wrapPlayer(event.getPlayer().asBukkit());
        byte[] collision = MathUtils.rotateVectors(event.getStructure().getCollisionVectors(), event.getDirection().getOpposite());
        byte[] offset = MathUtils.rotateVectors(event.getStructure().getOffset(), event.getDirection().getOpposite());
        for (int i = 0; i < collision.length; i += 3) {
            Location location = BukkitAdapter.adapt(event.getLocation().getRelative(collision[i] + offset[0], collision[i + 1] + offset[1], collision[i + 2] + offset[2]).toBukkit());
            if (!query.testState(location, player, FLAG_TORUS_STRUCTURE_BREAK) || !query.testBuild(location, player, Flags.BLOCK_BREAK)) {
                event.getPlayer().asBukkit().sendMessage(translate("interaction.region.no_access"));
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    void preventInteractionWithStructuresInRegionsWithDeniedFlag(PlayerStructureInteractEvent event) {
        if (event.getPlayer().asBukkit().hasPermission("worldguard.region.bypass." + event.getStructure().location.world.getBukkit().getName())
        || MainConfig.INTEGRATION_BLACKLIST.contains("WorldGuard"))
            return;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        LocalPlayer player = WorldGuardPlugin.inst().wrapPlayer(event.getPlayer().asBukkit());

        RegionQuery query = container.createQuery();
        byte[] collision = event.getStructure().getCollisionVectors();
        for (int i = 0; i < collision.length; i += 3) {
            Location location = BukkitAdapter.adapt(event.getStructure().location.getRelative(collision[i], collision[i + 1], collision[i + 2]).toBukkit());
            if (!query.testState(location, player, FLAG_TORUS_STRUCTURE_BREAK, Flags.INTERACT)) {
                event.getPlayer().asBukkit().sendMessage(translate("interaction.region.no_access"));
                event.setCancelled(true);
                return;
            }
        }
    }

    private static StateFlag registerOrGetExisting(FlagRegistry flagRegistry, String name) {
        try {
            StateFlag flag = new StateFlag(name, true);
            flagRegistry.register(flag);

            return flag;
        } catch (Exception exception) {
            return (StateFlag) flagRegistry.get(name);
        }
    }

}
