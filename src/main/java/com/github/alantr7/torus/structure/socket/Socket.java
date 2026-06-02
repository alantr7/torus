package com.github.alantr7.torus.structure.socket;

import com.github.alantr7.torus.network.NetworkGraph;
import com.github.alantr7.torus.structure.*;
import com.github.alantr7.torus.structure.StructurePart;
import com.github.alantr7.torus.utils.EventUtils;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public abstract class Socket {

    public StructureInstance structure;

    @Getter @Setter
    protected int connections;

    @Getter @Setter
    protected int allowedConnections;

    public int maximumInput = 100;

    public int maximumOutput = 100;

    public NetworkGraph network = NetworkGraph.INIT;

    @Getter @Setter
    protected FlowDirection flowDirection;

    @Getter
    public final Medium medium;

    public enum FlowDirection {
        ALL, NONE, IN, OUT;
    }

    public enum Medium {
        ITEM, ENERGY, FLUID, DATA;
    }

    @Getter
    protected StructurePart component;

    public Socket(StructurePart component, int allowedConnections, Medium medium, FlowDirection direction) {
        this.component = component;
        this.allowedConnections = allowedConnections;
        this.flowDirection = direction;
        this.medium = medium;
    }

    public boolean isConnectableFrom(Direction direction) {
        return (allowedConnections & direction.mask()) != 0;
    }

    public boolean isConnected(Direction direction) {
        return (connections & direction.mask()) != 0;
    }

    public Direction[] getValidConnectionsDirections() {
        Direction[] values = new Direction[Direction.values().length];
        int count = 0;

        for (Direction dir : Direction.values()) {
            if (isConnectableFrom(dir)) {
                values[count++] = dir;
            }
        }

        Direction[] stripped = new Direction[count];
        System.arraycopy(values, 0, stripped, 0, count);

        return stripped;
    }

    public void setConnected(Direction direction, boolean connected) {
        if (connected) {
            setConnections(connections | direction.mask());
        } else {
            setConnections(connections & ~direction.mask());
        }
    }

    public Collection<BlockLocation> getNodes() {
        List<BlockLocation> nodes = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (isConnected(direction))
                nodes.add(component.absoluteLocation.getRelative(direction));
        }
        return nodes;
    }

    public boolean toggleConnection(Direction direction) {
        if (!isConnectableFrom(direction))
            return false;

        BlockLocation relativeLoc = getComponent().absoluteLocation.getRelative(direction);
        StructureInstance neighbor = relativeLoc.getStructure();
        if (neighbor == null)
            return false;

        Socket neighborSocket = neighbor.getSocket(getComponent().absoluteLocation, medium);
        if (neighborSocket == null || !neighborSocket.isConnectableFrom(direction.getOpposite()))
            return false;

        if (isConnected(direction)) {
            EventUtils.callStructuresDisconnectEvent(this, neighborSocket, direction, direction.getOpposite());

            setConnected(direction, false);
            structure.onSocketDisconnect(this, neighborSocket, direction);

            neighborSocket.setConnected(direction.getOpposite(), false);
            neighbor.onSocketDisconnect(neighborSocket, this, direction.getOpposite());
        } else {
            if (!EventUtils.callStructuresConnectEvent(this, neighborSocket, direction, direction.getOpposite()))
                return true;

            setConnected(direction, true);
            structure.onSocketConnect(this, neighborSocket, direction);

            neighborSocket.setConnected(direction.getOpposite(), true);
            neighbor.onSocketConnect(neighborSocket, this, direction.getOpposite());
        }

        structure.location.world.networkManager.queueLoaded(this);
        structure.location.world.networkManager.queueLoaded(neighborSocket);
        return true;
    }

}
