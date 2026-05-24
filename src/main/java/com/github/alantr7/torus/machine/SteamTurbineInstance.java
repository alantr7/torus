package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.structure.*;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.data.Data;
import com.github.alantr7.torus.structure.inspection.InspectableDataContainer;
import com.github.alantr7.torus.structure.property.PropertyType;
import com.github.alantr7.torus.structure.socket.FluidSocket;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.world.Fluid;
import lombok.Getter;

import static com.github.alantr7.torus.lang.Localization.translatable;

public class SteamTurbineInstance extends StructureInstance implements Inspectable, RotationSource {

    @Getter
    protected Data<Integer> storedEnergy = dataContainer.persist("energy", Data.Type.INT, 0);

    protected Data<Integer> rpm = dataContainer.persist("rpm", Data.Type.INT, 0);

    protected Data<Float> torque = dataContainer.persist("torque", Data.Type.FLOAT, 0f);

    protected FluidSocket inSteam;

    public float shaftAngle;

    public SteamTurbineInstance(BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        super(Structures.STEAM_TURBINE, location, bodyDef, direction);
    }

    SteamTurbineInstance(LoadContext context) {
        super(context);
    }

    @Override
    protected void setup() throws SetupException {
        inSteam = requireSocket("in_fluid", FluidSocket.class);
        inSteam.maximumInput = 1000;
    }

    @Override
    public void tick(boolean isVirtual) {
        int consumed = inSteam.consumeFluid(Fluid.STEAM, 1000);
        if (consumed == 0) {
            torque.update(torque.get() * 0.25f);
        } else {
            torque.update(torque.get() + (consumed / 100f));
        }

        if (torque.get() < 0.1f) {
            torque.update(0f);
        } else if (torque.get() > 50) {
            torque.update(50f);
        }

        rpm.update((int) Math.max(0, rpm.get() + torque.get() - rpm.get() / 25f));
        shaftAngle = rpm.get() / 900f * -180f;
    }

    @Override
    public InspectableDataContainer setupInspectableData() {
        return new InspectableDataContainer((byte) 1)
          .property(translatable("inspection.rpm"), () -> rpm.get() + "");
    }

    @Override
    public int getRpm() {
        return rpm.get();
    }

    @Override
    public float getTorque() {
        return torque.get();
    }

}
