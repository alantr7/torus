package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.item.TorusItem;
import com.github.alantr7.torus.structure.Conductor;
import com.github.alantr7.torus.structure.socket.*;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.structure.LoadContext;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.data.Data;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.Structures;
import com.github.alantr7.torus.world.SocketLocation;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;

import static com.github.alantr7.torus.machine.EnergyCable.*;

public class CableInstance extends StructureInstance implements Conductor {

    protected Data<Integer> type = dataContainer.persist("type", Data.Type.INT, 0);

    protected Socket socket;

    static final BoundingBox[] interactionBoxes = new BoundingBox[6];
    static {
        float width = 0.25f;
        float shift = 0.5f - width;

        BoundingBox base = new BoundingBox(-width, -width, -width, width, width, width);
        interactionBoxes[0] = base.clone().shift(0, 0, -shift);
        interactionBoxes[1] = base.clone().shift(shift, 0, 0);
        interactionBoxes[2] = base.clone().shift(0, 0, shift);
        interactionBoxes[3] = base.clone().shift(-shift, 0, 0);
        interactionBoxes[4] = base.clone().shift(0, shift, 0);
        interactionBoxes[5] = base.clone().shift(0, -shift, 0);
    }

    public CableInstance(BlockLocation location, StructureBodyDef bodyDef, Socket.Medium type) {
        super(type == Socket.Medium.ENERGY ? Structures.ENERGY_CABLE : type == Socket.Medium.ITEM ? Structures.ITEM_CABLE : type == Socket.Medium.DATA ? Structures.DATA_CABLE : Structures.FLUID_CABLE, location, bodyDef, Direction.NORTH);
        this.type.update(type.ordinal());
    }

    CableInstance(LoadContext context) {
        super(context);
    }

    @Override
    protected void setup() throws SetupException {
        if (dataContainer.getEntries().containsKey("connections")) {
            socket = switch (getMedium()) {
                case ENERGY -> new EnergySocket(getPart("base"), 0b111111, Socket.FlowDirection.ALL);
                case ITEM -> new ItemSocket(getPart("base"), 0b111111, Socket.FlowDirection.ALL);
                case FLUID -> new FluidSocket(getPart("base"), 0b111111, Socket.FlowDirection.ALL);
                case DATA -> new DataSocket(getPart("base"), 0b111111, Socket.FlowDirection.ALL);
            };
            socket.structure = this;
            socket.setConnections(dataContainer.getOrDefault("connections", Data.Type.INT, 0));

            // Register sockets
            socketsByName.put("base", socket);
            for (Direction possibleDirection : Direction.values()) {
                if (socket.isConnectableFrom(possibleDirection)) {
                    sockets.put(new SocketLocation(getPart("base").absoluteLocation.getRelative(possibleDirection), socket.medium), socket);
                }
            }

            dataContainer.getEntries().remove("connections");
            save();
        } else {
            socket = requireSocket("base");
        }

        for (Direction direction : Direction.values()) {
            if (socket.isConnected(direction)) {
                state.set(getStateFromDirection(direction), true, false);
            }
        }
    }

    @Override
    public boolean onPlayerInteract(PlayerInteractEvent event, BlockLocation location) {
        if (!TorusItem.is(event.getPlayer().getInventory().getItemInMainHand(), "torus:screwdriver")) {
            return false;
        }

        Vector dr = event.getInteractionPoint().subtract(event.getPlayer().getEyeLocation()).toVector().normalize().multiply(0.07f);
        Vector r = event.getInteractionPoint().subtract(location.toBukkit()).clone().toVector().add(new Vector(-0.5, -0.5, -0.5));

        // Make it relative
        for (int i = 0; i < 18; i++) {
            for (int j = 0; j < interactionBoxes.length; j++) {
                BoundingBox box = interactionBoxes[j];
                if (box.contains(r)) {
                    if (socket.toggleConnection(Direction.values()[j]))
                        return true;
                }
            }
            r.add(dr);
        }

        return false;
    }

    @Override
    public boolean isConductive() {
        return true;
    }

    @Override
    public void onSocketConnect(Socket socket, Socket neighbor, Direction direction) {
        state.set(getStateFromDirection(direction), true);
    }

    @Override
    public void onSocketDisconnect(Socket socket, Socket neighbor, Direction direction) {
        state.set(getStateFromDirection(direction), false);
    }

    @Override
    public Socket.Medium getMedium() {
        return Socket.Medium.values()[type.get()];
    }

    @Override
    public Collection<BlockLocation> getConnectedNodes() {
        List<BlockLocation> nodes = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (socket.isConnected(direction))
                nodes.add(location.getRelative(direction));
        }

        return nodes;
    }

}
