package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.structure.Structure;
import com.github.alantr7.torus.structure.StructureFlag;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.builder.StructurePartDef;
import com.github.alantr7.torus.utils.ByteArrayBuilder;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.world.Pitch;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import static com.github.alantr7.torus.lang.Localization.translatable;

public class Elevator extends Structure {

    public Elevator() {
        super(TorusPlugin.DEFAULT_ADDON, "elevator", translatable("structures.elevator.name"), ElevatorInstance.class);
        setFlags(StructureFlag.COLLIDABLE | StructureFlag.HEAVY | StructureFlag.TICKABLE);
    }

    @Override
    protected void createBounds(ByteArrayBuilder builder) {
        builder.add(0, 0, 0);
    }

    @Override
    protected StructureInstance instantiate(@NotNull BlockLocation location, Direction direction, Pitch pitch) {
        return new ElevatorInstance(this, location, new StructureBodyDef(new StructurePartDef[]{
          new StructurePartDef("base", new Vector3f()),
          new StructurePartDef("platform", new Vector3f()),
          new StructurePartDef("crane1", new Vector3f()),
        }), direction);
    }

}
