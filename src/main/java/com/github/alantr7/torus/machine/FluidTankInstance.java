package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.structure.property.PropertyType;
import com.github.alantr7.torus.structure.socket.FluidSocket;
import com.github.alantr7.torus.utils.MathUtils;
import com.github.alantr7.torus.model.de_provider.DisplayEntitiesPartModel;
import com.github.alantr7.torus.structure.inspection.InspectableDataContainer;
import com.github.alantr7.torus.world.Fluid;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.structure.*;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.StructurePart;
import com.github.alantr7.torus.structure.data.Data;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

public class FluidTankInstance extends StructureInstance implements FluidContainer, Inspectable {

    protected StructurePart liquidComponent;

    protected FluidSocket input;

    protected Data<Integer> fluid = dataContainer.persist("fluid", Data.Type.INT, -1);

    protected Data<Integer> stored = dataContainer.persist("stored", Data.Type.INT, 0);

    protected ItemDisplay fluidDisplay;

    FluidTankInstance(LoadContext context) {
        super(context);
    }

    public FluidTankInstance(BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        super(Structures.FLUID_TANK, location, bodyDef, direction);
    }

    @Override
    protected void setup() throws SetupException {
        liquidComponent = getPart("liquid");
        input = requireSocket("in_fluid", FluidSocket.class);
        requireSocket("out_fluid").maximumOutput = 1000;
    }

    @Override
    public InspectableDataContainer setupInspectableData() {
        return new InspectableDataContainer((byte) 2)
          .property("Fluid", () -> getFluid() == null ? "(None)" : getFluid().name())
          .property("Level", () -> MathUtils.formatNumber(getStoredFluid()) + "/" + MathUtils.formatNumber(getFluidCapacity()) + " mb");
    }

    @Override
    public void onModelSpawn() {
        fluidDisplay = (ItemDisplay) ((DisplayEntitiesPartModel) FluidTank.MODEL_FLUID.toModel(location, direction, pitch).parts.get("fluid")).entityReferences.getFirst().getEntity();
        fluidDisplay.setInterpolationDuration(10);

        updateFluidDisplay();
    }

    @Override
    public void onModelDestroy() {
        fluidDisplay.remove();
    }

    @Override
    public void tick(boolean isVirtual) {
        if (input.network.nodes.isEmpty())
            return;

        if (stored.get() < getFluidCapacity()) {
            Fluid fluid = getFluid();
            if (fluid == null) {
                for (Fluid fluid1 : Fluid.values()) {
                    int consumed = input.consumeFluid(fluid1, 1000);
                    if (consumed != 0) {
                        supplyFluid(consumed);
                        this.fluid.update(fluid1.ordinal());
                        break;
                    }
                }
            } else {
                int consumed = input.consumeFluid(fluid, 1000);
                supplyFluid(consumed);
            }
        }
    }

    public void updateFluidDisplay() {
        float height = (float) stored.get() / getFluidCapacity() * 2.75f;
        if ((fluidDisplay.getItemStack().getType() == Material.BLUE_CONCRETE && fluid.get() != Fluid.WATER.ordinal()) || (fluidDisplay.getItemStack().getType() == Material.ORANGE_CONCRETE && fluid.get() != Fluid.LAVA.ordinal())) {
            fluidDisplay.setItemStack(new ItemStack(fluid.get() == Fluid.WATER.ordinal() ? Material.BLUE_CONCRETE : Material.ORANGE_CONCRETE));
        }

        Transformation transformation = fluidDisplay.getTransformation();

        Vector3f translation = transformation.getTranslation();
        translation.y = 0.14f + height / 2.0f;

        Vector3f scale = transformation.getScale();
        scale.y = height;

        fluidDisplay.setInterpolationDelay(0);
        fluidDisplay.setTransformation(transformation);
    }

    @Override
    public int consumeFluid(int amount) {
        int consumed = FluidContainer.super.consumeFluid(amount);
        if (consumed != 0)
            updateFluidDisplay();

        return consumed;
    }

    @Override
    public int supplyFluid(int amount) {
        int supplied = FluidContainer.super.supplyFluid(amount);
        if (supplied != 0)
            updateFluidDisplay();

        return supplied;
    }

    @Override
    public Fluid getFluid() {
        return (fluid.get() == -1 || stored.get() == 0) ? null : Fluid.values()[fluid.get()];
    }

    @Override
    public int getFluidCapacity() {
        return structure.getProperty("fluid_settings.capacity", PropertyType.INT);
    }

    @Override
    public int getStoredFluid() {
        return stored.get();
    }

    @Override
    public void setStoredFluid(int fluid) {
        stored.update(fluid);
        if (fluid == 0) {
            this.fluid.update(-1);
        }
    }

}
