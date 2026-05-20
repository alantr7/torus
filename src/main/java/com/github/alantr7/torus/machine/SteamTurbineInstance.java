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

public class SteamTurbineInstance extends StructureInstance implements Inspectable {

    @Getter
    protected Data<Integer> storedEnergy = dataContainer.persist("energy", Data.Type.INT, 0);

    public SteamTurbineInstance(BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        super(Structures.STEAM_TURBINE, location, bodyDef, direction);
    }

    SteamTurbineInstance(LoadContext context) {
        super(context);
    }

    @Override
    protected void setup() throws SetupException {

    }

    @Override
    public void tick(boolean isVirtual) {
    }

    @Override
    public InspectableDataContainer setupInspectableData() {
        return new InspectableDataContainer((byte) 1)
          .property(translatable("inspection.rpm"), () -> getRPM() + "");
    }

    public float getRPM() {
        return 30;
    }

}
