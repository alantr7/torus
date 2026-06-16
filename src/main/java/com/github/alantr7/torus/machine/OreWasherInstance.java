package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.structure.property.PropertyType;
import com.github.alantr7.torus.structure.socket.EnergySocket;
import com.github.alantr7.torus.structure.socket.FluidSocket;
import com.github.alantr7.torus.structure.socket.ItemSocket;
import com.github.alantr7.torus.world.Fluid;
import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.recipe.WasherRecipe;
import com.github.alantr7.torus.structure.*;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.data.Data;
import com.github.alantr7.torus.structure.inventory.CustomStructureInventory;
import com.github.alantr7.torus.structure.inventory.StructureInventory;
import com.github.alantr7.torus.world.BlockLocation;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OreWasherInstance extends StructureInstance implements EnergyContainer, FluidContainer, HologramProvider {

    protected Data<Integer> water = dataContainer.persist("fluid", Data.Type.INT, 0);

    @Getter
    protected Data<Integer> storedEnergy = dataContainer.persist("energy", Data.Type.INT, 0);

    protected EnergySocket powerSocket;

    protected ItemSocket itemInSocket, itemOutSocket;

    protected FluidSocket waterInSocket;

    protected StructureInventory itemOutBuffer = new CustomStructureInventory(1);

    protected int processedTicks;

    private WasherRecipe recipe;

    public OreWasherInstance(LoadContext context) {
        super(context);
    }

    public OreWasherInstance(BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        super(Structures.ORE_WASHER, location, bodyDef, direction);
    }

    @Override
    public void tick(boolean isVirtual) {
        powerSocket.maintainEnergy(this);
        itemOutSocket.attemptDirectItemExport();

        if (water.get() < getFluidCapacity()) {
            int consumed = waterInSocket.consumeFluid(Fluid.WATER, structure.getProperty("fluid_settings.capacity", PropertyType.INT) - water.get());
            supplyFluid(consumed);
        }

        if (recipe != null) {
            if (!hasSufficientEnergy(structure.getProperty("energy_settings.consumption", PropertyType.INT)) || water.get() < structure.getProperty("fluid_settings.consumption", PropertyType.INT)) {
                return;
            }

            processedTicks++;
            consumeEnergy(structure.getProperty("energy_settings.consumption", PropertyType.INT));
            consumeFluid(structure.getProperty("fluid_settings.consumption", PropertyType.INT));
        } else {
            List<ItemStack> items = itemInSocket.consumeItems(OreWasher.INPUT_CRITERIA, 1, true);
            if (!items.isEmpty()) {
                recipe = TorusPlugin.getInstance().getRecipeRegistry().getWasherRecipeByIngredient(items.getFirst());
            }
        }

        if (recipe != null && processedTicks >= recipe.washTicks) {
            itemOutBuffer.addItem(recipe.result.asResult());
            recipe = null;
            processedTicks = 0;
        }
    }

    @Override
    protected void setup() throws SetupException {
        powerSocket = requireSocket("power_connector", EnergySocket.class);
        powerSocket.maximumInput = structure.getProperty("energy_settings.maximum_input", PropertyType.INT);
        itemInSocket = requireSocket("item_connector", ItemSocket.class);
        waterInSocket = requireSocket("fluid_connector", FluidSocket.class);
        itemOutSocket = requireSocket("out_connector", ItemSocket.class);
        itemOutSocket.linkedInventory = itemOutBuffer;
    }

    @Override
    public int getEnergyCapacity() {
        return structure.getProperty("energy_settings.capacity", PropertyType.INT);
    }

    @Override
    public @Nullable Fluid getFluid() {
        return Fluid.WATER;
    }

    @Override
    public int getFluidCapacity() {
        return 1_000;
    }

    @Override
    public int getStoredFluid() {
        return water.get();
    }

    @Override
    public void setStoredFluid(int fluid) {
        water.update(fluid);
    }

}
