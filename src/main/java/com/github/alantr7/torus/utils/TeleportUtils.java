package com.github.alantr7.torus.utils;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class TeleportUtils {

    private static Method teleportTo;

    public static void init() {
        try {
            teleportTo = Class.forName("net.minecraft.server.level.ServerPlayer").getDeclaredMethod("teleportTo", Double.TYPE, Double.TYPE, Double.TYPE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void teleport(Player player, Location location) {
        try {
            teleportTo.invoke(getHandle(player), location.getX(), location.getY(), location.getZ());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Object getHandle(Entity entity) {
        try {
            Method entity_getHandle = entity.getClass().getMethod("getHandle");
            return entity_getHandle.invoke(entity);
        } catch (Exception var2) {
            var2.printStackTrace();
            return null;
        }
    }

}