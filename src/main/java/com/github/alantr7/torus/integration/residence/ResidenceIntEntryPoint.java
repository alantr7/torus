package com.github.alantr7.torus.integration.residence;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.github.alantr7.bukkitplugin.annotations.core.RequiresPlugin;
import com.github.alantr7.bukkitplugin.annotations.core.Singleton;
import com.github.alantr7.torus.api.event.PlayerStructureBreakEvent;
import com.github.alantr7.torus.api.event.PlayerStructurePrePlaceEvent;
import com.github.alantr7.torus.config.MainConfig;
import com.github.alantr7.torus.utils.MathUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@Singleton
@RequiresPlugin("Residence")
public class ResidenceIntEntryPoint {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    void preventPlacingStructuresWhereNoPermission(PlayerStructurePrePlaceEvent event) {
        if (MainConfig.INTEGRATION_BLACKLIST.contains("Residence"))
            return;

        Player player = event.getPlayer().asBukkit();
        byte[] collision = MathUtils.rotateVectors(event.getStructure().getCollisionVectors(), event.getDirection().getOpposite());
        byte[] offset = MathUtils.rotateVectors(event.getStructure().getOffset(), event.getDirection().getOpposite());
        for (int i = 0; i < collision.length; i += 3) {
            Location location = event.getLocation().getRelative(collision[i] + offset[0], collision[i + 1] + offset[1], collision[i + 2] + offset[2]).toBukkit();
            ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(location);
            if (res != null && !res.getPermissions().playerHas(player, Flags.place, true)) {
                player.sendMessage(ChatColor.RED + "Sorry, you can not do that here.");
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    void preventBreakingStructuresWhereNoPermission(PlayerStructureBreakEvent event) {
        if (MainConfig.INTEGRATION_BLACKLIST.contains("Residence"))
            return;

        Player player = event.getPlayer().asBukkit();
        byte[] collision = event.getStructure().getCollisionVectors();
        for (int i = 0; i < collision.length; i += 3) {
            Location location = event.getStructure().location.getRelative(collision[i], collision[i + 1], collision[i + 2]).toBukkit();
            ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(location);
            if (res != null && !res.getPermissions().playerHas(player, Flags.destroy, true)) {
                player.sendMessage(ChatColor.RED + "Sorry, you can not do that here.");
                event.setCancelled(true);
                return;
            }
        }
    }

}
