package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.model.de_provider.DisplayEntitiesPartModel;
import com.github.alantr7.torus.structure.LoadContext;
import com.github.alantr7.torus.structure.Structure;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.data.Data;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Collection;

public class ElevatorInstance extends StructureInstance {

    private static final int NO_TARGET = 512;

    public static int targetY = NO_TARGET;

    public static final int MAX = 9;

    // 3 blocks per second

    protected Data<Integer> height = dataContainer.persist("height", Data.Type.INT, 0);

    protected ArmorStand elevatorCarrier;

    ElevatorInstance(LoadContext context) {
        super(context);
    }

    public ElevatorInstance(Structure structure, BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        super(structure, location, bodyDef, direction);
    }

    @Override
    protected void setup() throws SetupException {

    }

    @Override
    public void onModelSpawn() {
        elevatorCarrier = location.world.getBukkit().spawn(location.toBukkitCentered().add(0, height.get() + 1d, 0), ArmorStand.class);
        elevatorCarrier.setPersistent(false);
        elevatorCarrier.setInvisible(true);
        elevatorCarrier.setInvulnerable(true);
        elevatorCarrier.setGravity(false);
        elevatorCarrier.setMarker(true);
        elevatorCarrier.addPassenger(((DisplayEntitiesPartModel) model.getPartByName("platform")).parent);

        Shulker shulker = location.world.getBukkit().spawn(location.toBukkitCentered(), Shulker.class);
        shulker.setAI(false);
        shulker.setPersistent(false);
        shulker.setColor(DyeColor.GRAY);
        shulker.getAttribute(Attribute.SCALE).setBaseValue(3d);
        shulker.setInvulnerable(true);
        shulker.setInvisible(true);
        elevatorCarrier.addPassenger(shulker);
    }

    @Override
    public void onModelDestroy() {
        for (Entity passenger : elevatorCarrier.getPassengers()) {
            passenger.remove();
        }
        elevatorCarrier.remove();
    }

    float pos;

    @Override
    public void tick(boolean isVirtual) {
        if (targetY == NO_TARGET) {
            return;
        }

        int oldHeight = height.get();
        int newHeight = Math.abs(targetY - location.y);
        height.update(newHeight);

        int distance = Math.abs(newHeight - oldHeight);
        Collection<Player> players = location.getRelative(0, oldHeight - 3, 0).toBukkit().getNearbyPlayers(5d);

        Location targetLocation = location.toBukkitCentered();
        targetLocation.setY(targetY);

        pos = oldHeight;

        Vector velocity = new Vector(0, newHeight < oldHeight ? -0.05 : 0.05, 0);
        targetY = NO_TARGET;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(TorusPlugin.getInstance(), () -> {
            elevatorCarrier.teleport(location.toBukkitCentered().add(0, pos - 3, 0));
            pos -= (newHeight < oldHeight ? 0.05f : -0.05f);
            if (newHeight > oldHeight) {
                for (Player player : players) {
                    player.setVelocity(velocity);
                }
            }
        }, 1, 1);

        Bukkit.getScheduler().runTaskLater(TorusPlugin.getInstance(), () -> {
            elevatorCarrier.setGravity(false);
            task.cancel();
            elevatorCarrier.teleport(location.toBukkitCentered().add(0, newHeight - 3, 0));
            for (Player player : players) {
                Location playerLocation = player.getLocation();
                playerLocation.setY(location.y + newHeight + 1);
                player.teleport(playerLocation);
            }
        }, distance * 20L);
    }

}
