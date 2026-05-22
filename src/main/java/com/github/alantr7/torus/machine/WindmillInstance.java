package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.structure.*;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.data.Data;
import com.github.alantr7.torus.structure.inspection.InspectableDataContainer;
import com.github.alantr7.torus.structure.property.PropertyType;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import lombok.Getter;

import static com.github.alantr7.torus.lang.Localization.translatable;
import static com.github.alantr7.torus.machine.Windmill.STATE_ACTIVE;

public class WindmillInstance extends StructureInstance implements RotationSource, Inspectable {

    @Getter
    float efficiency;

    WindmillInstance(LoadContext context) {
        super(context);
    }

    public WindmillInstance(BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        super(Structures.WINDMILL, location, bodyDef, direction);
    }

    @Override
    public void tick(boolean isVirtual) {
    }

    @Override
    protected void setup() throws SetupException {
        efficiency = (float) Math.pow(Math.E, -8f/(location.y / 8f + 8f)) * 1.15505059f;
        state.set(STATE_ACTIVE, efficiency != 0, false);
    }

    @Override
    public InspectableDataContainer setupInspectableData() {
        return new InspectableDataContainer((byte) 1)
          .property(translatable("inspection.windmill.efficiency"), () -> (int) (efficiency * 100) + "%");
    }

    @Override
    public int getRpm() {
        return (int) (efficiency * 200);
    }

    @Override
    public float getTorque() {
        return 0;
    }

}
