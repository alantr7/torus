package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.structure.LoadContext;
import com.github.alantr7.torus.structure.Structure;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;

public class ElevatorDetectorInstance extends StructureInstance {

    ElevatorDetectorInstance(LoadContext context) {
        super(context);
    }

    public ElevatorDetectorInstance(Structure structure, BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        super(structure, location, bodyDef, direction);
    }

    @Override
    protected void setup() throws SetupException {

    }

}
