package com.github.alantr7.torus.structure;

import com.github.alantr7.torus.structure.data.Data;
import com.github.alantr7.torus.structure.inspection.InspectableDataContainer;

import static com.github.alantr7.torus.lang.Localization.translatable;

public interface EnergyContainer extends HologramProvider {

    FlowMeter getFlowMeter();

    int getEnergyCapacity();

    Data<Integer> getStoredEnergy();

    default boolean hasSufficientEnergy(int amount) {
        return getStoredEnergy().get() >= amount;
    }

    default int consumeEnergy(int amount) {
        int starting = getStoredEnergy().get();
        getStoredEnergy().update(Math.max(starting - amount, 0));

        int consumed = starting - getStoredEnergy().get();
        getFlowMeter().update(-consumed);

        return consumed;
    }

    default int supplyEnergy(int amount) {
        int starting = getStoredEnergy().get();
        getStoredEnergy().update(Math.min(starting + amount, getEnergyCapacity()));

        int supplied = getStoredEnergy().get() - starting;
        getFlowMeter().update(supplied);

        return supplied;
    }

    @Override
    default InspectableDataContainer setupInspectableData() {
        return new InspectableDataContainer((byte) 1).property(translatable("inspection.energy_unit"), InspectableDataContainer.TEMPLATE_RF.apply(this));
    }

}
