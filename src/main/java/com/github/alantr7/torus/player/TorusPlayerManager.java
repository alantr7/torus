package com.github.alantr7.torus.player;

import com.github.alantr7.bukkitplugin.annotations.core.Invoke;
import com.github.alantr7.bukkitplugin.annotations.core.Singleton;
import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.plugin.Permissions;
import com.github.alantr7.torus.updater.UpdateChecker;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.alantr7.torus.lang.Localization.translate;

@Singleton
public class TorusPlayerManager implements Listener {

    private final Map<UUID, TorusPlayer> players = new HashMap<>();

    private static final ChatColor COLOR_UPDATE_MESSAGE = ChatColor.of("#cfcfcf");

    @NotNull
    public TorusPlayer asTorusPlayer(Player player) {
        return players.get(player.getUniqueId());
    }

    @Nullable
    public TorusPlayer getTorusPlayer(UUID id) {
        return players.get(id);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onPlayerJoin(PlayerJoinEvent event) {
        players.put(event.getPlayer().getUniqueId(), new TorusPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName()));
        if (event.getPlayer().hasPermission(Permissions.UPDATE_AVAILABLE_NOTIFICATION) && TorusPlugin.getInstance().getVersion().isOlderThan(UpdateChecker.getLatestVersion())) {
            event.getPlayer().sendMessage(COLOR_UPDATE_MESSAGE + translate("general.update_available"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onPlayerQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer().getUniqueId());
    }

    @Invoke(Invoke.Schedule.BEFORE_PLUGIN_ENABLE)
    void registerOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers())
            players.put(player.getUniqueId(), new TorusPlayer(player.getUniqueId(), player.getName()));
    }

}
