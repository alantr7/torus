package com.github.alantr7.torus.structure.socket;

import com.github.alantr7.torus.structure.StructurePart;

public class DataSocket extends Socket {

    public DataSocket(StructurePart component, int allowedConnections, FlowDirection direction) {
        super(component, allowedConnections, Medium.DATA, direction);
    }

}
