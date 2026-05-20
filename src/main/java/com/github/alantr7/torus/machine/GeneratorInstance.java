package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.structure.EnergyContainer;
import com.github.alantr7.torus.structure.LoadContext;
import com.github.alantr7.torus.structure.RotationSource;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.data.Data;
import com.github.alantr7.torus.structure.inspection.InspectableDataContainer;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import lombok.Getter;

import static com.github.alantr7.torus.lang.Localization.translate;

public class GeneratorInstance extends StructureInstance implements EnergyContainer {

    @Getter
    protected Data<Integer> storedEnergy = dataContainer.persist("energy", Data.Type.INT, 0);

    protected Data<Integer> rpm = dataContainer.persist("rpm", Data.Type.INT, 0);

    public static final float RPM_TO_RF = 0.1f;

    public GeneratorInstance(LoadContext context) {
        super(context);
    }

    public GeneratorInstance(Generator structure, BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        super(structure, location, bodyDef, direction);
    }

    @Override
    protected void setup() throws SetupException {

    }

    @Override
    public void tick(boolean isVirtual) {
        if (location.getRelative(direction.getOpposite()).getStructure() instanceof RotationSource source) {
            rpm.update(source.getRpm());
            state.set(Generator.STATE_CONNECTED, true);
        } else {
            rpm.update(0);
            state.set(Generator.STATE_CONNECTED, false);
        }

        if (rpm.get() != 0) {
            supplyEnergy((int) (rpm.get() * RPM_TO_RF));
        }
    }

    @Override
    public int getEnergyCapacity() {
        return 2000;
    }

    public int getRpm() {
        return rpm.get();
    }

    public int getProduction() {
        return (int) (rpm.get() * RPM_TO_RF);
    }

    @Override
    public InspectableDataContainer setupInspectableData() {
        return new InspectableDataContainer((byte) 2)
          .property(translate("inspection.rpm"), () -> getRpm() + "")
          .property(translate("inspection.energy_unit"), InspectableDataContainer.TEMPLATE_RF.apply(this));
    }

}
