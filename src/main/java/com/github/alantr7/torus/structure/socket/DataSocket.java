package com.github.alantr7.torus.structure.socket;

import com.github.alantr7.torus.network.Node;
import com.github.alantr7.torus.structure.DataTransmitter;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.StructurePart;
import org.jetbrains.annotations.NotNull;

public class DataSocket extends Socket {

    public DataSocket(StructurePart component, int allowedConnections, FlowDirection direction) {
        super(component, allowedConnections, Medium.DATA, direction);
    }

    public StructureInstance getNode(@NotNull String mac) {
        for (Node node : network.nodes) {
            if (node.structure instanceof DataTransmitter dt && mac.equals(dt.getMAC())) {
                return node.structure;
            }
        }
        return null;
    }

}
