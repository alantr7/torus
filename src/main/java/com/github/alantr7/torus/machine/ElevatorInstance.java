package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.model.de_provider.DisplayEntitiesPartModel;
import com.github.alantr7.torus.network.Node;
import com.github.alantr7.torus.structure.LoadContext;
import com.github.alantr7.torus.structure.Structure;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.data.Data;
import com.github.alantr7.torus.structure.socket.DataSocket;
import com.github.alantr7.torus.utils.TeleportUtils;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ElevatorInstance extends StructureInstance {

    private static final int NO_TARGET = 512;

    public static final int MAX = 9;

    // 3 blocks per second

    protected Data<Integer> height = dataContainer.persist("height", Data.Type.INT, 0);

    protected Data<Integer> targetHeight = dataContainer.persist("target_height", Data.Type.INT, NO_TARGET);

    protected Map<Integer, String> floors = new HashMap<>();

    protected ArmorStand elevatorCarrier;

    protected DataSocket dataSocket;

    protected boolean isMoving;

    protected BukkitTask moveTask;

    ElevatorInstance(LoadContext context) {
        super(context);
    }

    public ElevatorInstance(Structure structure, BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        super(structure, location, bodyDef, direction);
    }

    @Override
    protected void setup() throws SetupException {
        dataSocket = requireSocket("data", DataSocket.class);
    }

    @Override
    public void onModelSpawn() {
        elevatorCarrier = location.world.getBukkit().spawn(location.toBukkitCentered().add(0, height.get() - 3d, 0), ArmorStand.class);
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
        if (moveTask != null) {
            moveTask.cancel();
        }
    }

    static float increment = 0.075f;

    float pos;

    int ticks;

    @Override
    public void tick(boolean isVirtual) {
        // Update floors list
        if (ticks % 3 == 0) {
            TreeMap<Integer, String> floors = new TreeMap<>(Comparator.comparingInt(a -> (int) a).reversed());
            Map<Integer, ElevatorDetectorInstance> detectors = new HashMap<>();
            for (Node node : dataSocket.network.nodes) {
                if (node.structure instanceof ElevatorDetectorInstance detector) {
                    floors.put(detector.location.y + 1, "");
                    detectors.put(detector.location.y + 1, detector);
                }
            }
            int floor = floors.size();
            for (var entry : floors.entrySet()) {
                ElevatorDetectorInstance detector = detectors.get(entry.getKey());
                if (!detector.name.get().isBlank()) {
                    detector.assignedName = detector.name.get();
                } else {
                    detector.assignedName = "Floor " + floor;
                }
                entry.setValue(detector.assignedName);
                floor--;
            }

            this.floors = floors;
        }

        ticks++;

        if (isMoving || targetHeight.get() == NO_TARGET) {
            return;
        }

        int oldHeight = height.get();
        int newHeight = Math.abs(targetHeight.get() - location.y);
        height.update(newHeight);

        int distance = Math.abs(newHeight - oldHeight);
        Collection<Player> players = location.world.getBukkit().getNearbyEntities(location.getRelative(0, oldHeight - 3, 0).toBukkit(), 5, 5, 5, e -> e instanceof Player).stream().map(e -> (Player) e).toList();

        Location targetLocation = location.toBukkitCentered();
        targetLocation.setY(targetHeight.get());

        pos = oldHeight;
        isMoving = true;

        moveTask = Bukkit.getScheduler().runTaskTimer(TorusPlugin.getInstance(), () -> {
            elevatorCarrier.teleport(location.toBukkitCentered().add(0, pos - 3, 0));
            pos -= (newHeight < oldHeight ? increment : -increment);
            if (newHeight > oldHeight) {
                for (Player player : players) {
                    player.setGravity(false);
                    Location playerLocation = player.getLocation();
                    Location loc2 = new Location(playerLocation.getWorld(), playerLocation.getX(), location.y + pos + 0.1, playerLocation.getZ());
                    TeleportUtils.teleport(player, loc2);
                }
            }
        }, 1, 1);

        Bukkit.getScheduler().runTaskLater(TorusPlugin.getInstance(), () -> {
            elevatorCarrier.setGravity(false);
            moveTask.cancel();
            moveTask = null;
            elevatorCarrier.teleport(location.toBukkitCentered().add(0, newHeight - 3, 0));
            for (Player player : players) {
                player.setGravity(true);
                Location playerLocation = player.getLocation();
                playerLocation.setY(location.y + newHeight + 1);
                player.teleport(playerLocation);
            }
            targetHeight.update(NO_TARGET);
            isMoving = false;
        }, distance * 15L);
    }

    public void setTargetHeight(int height) {
        targetHeight.update(height);
    }

    public int getFloor() {
        int floor = 1024;
        float distance = 1024;
        for (var entry : floors.entrySet()) {
            float d = Math.abs((entry.getKey() - location.y) - pos);
            if (d < distance) {
                floor = entry.getKey();
                distance = d;
            }
        }

        return floor;
    }

}
