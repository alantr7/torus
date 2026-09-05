package com.github.alantr7.torus.integration.griefprevention;

import com.github.alantr7.bukkitplugin.annotations.core.RequiresPlugin;
import com.github.alantr7.bukkitplugin.annotations.core.Singleton;
import com.github.alantr7.torus.api.event.PlayerStructureBreakEvent;
import com.github.alantr7.torus.api.event.PlayerStructurePrePlaceEvent;
import com.github.alantr7.torus.config.MainConfig;
import com.github.alantr7.torus.utils.MathUtils;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import static com.github.alantr7.torus.lang.Localization.translate;

@Singleton
@RequiresPlugin("GriefPrevention")
public class GriefPreventionIntEntryPoint {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    void preventPlacingStructuresWhereNoPermission(PlayerStructurePrePlaceEvent event) {
        if (MainConfig.INTEGRATION_BLACKLIST.contains("GriefPrevention"))
            return;

        Player player = event.getPlayer().asBukkit();
        byte[] collision = MathUtils.rotateVectors(event.getStructure().getCollisionVectors(), event.getDirection().getOpposite());
        byte[] offset = MathUtils.rotateVectors(event.getStructure().getOffset(), event.getDirection().getOpposite());
        for (int i = 0; i < collision.length; i += 3) {
            Location location = event.getLocation().getRelative(collision[i] + offset[0], collision[i + 1] + offset[1], collision[i + 2] + offset[2]).toBukkit();
            if (GriefPrevention.instance.allowBreak(player, location.getBlock(), location) != null) {
                player.sendMessage(translate("interaction.region.no_access"));
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    void preventBreakingStructuresWhereNoPermission(PlayerStructureBreakEvent event) {
        if (MainConfig.INTEGRATION_BLACKLIST.contains("GriefPrevention"))
            return;

        Player player = event.getPlayer().asBukkit();
        byte[] collision = event.getStructure().getCollisionVectors();
        for (int i = 0; i < collision.length; i += 3) {
            Location location = event.getStructure().location.getRelative(collision[i], collision[i + 1], collision[i + 2]).toBukkit();
            if (GriefPrevention.instance.allowBreak(player, location.getBlock(), location) != null) {
                player.sendMessage(translate("interaction.region.no_access"));
                event.setCancelled(true);
                return;
            }
        }
    }

}
