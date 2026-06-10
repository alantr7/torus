package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.structure.inspection.InspectableDataContainer;
import com.github.alantr7.torus.structure.property.PropertyType;
import com.github.alantr7.torus.structure.socket.EnergySocket;
import com.github.alantr7.torus.structure.socket.ItemSocket;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.recipe.CrusherRecipe;
import com.github.alantr7.torus.structure.*;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.StructurePart;
import com.github.alantr7.torus.structure.data.Data;
import com.github.alantr7.torus.structure.inventory.CustomStructureInventory;
import com.github.alantr7.torus.world.BlockLocation;
import lombok.Getter;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static com.github.alantr7.torus.lang.Localization.translatable;
import static com.github.alantr7.torus.lang.Localization.translate;
import static com.github.alantr7.torus.machine.OreCrusher.STATE_WORKING;

public class OreCrusherInstance extends StructureInstance implements HologramProvider, EnergyContainer {

    protected StructurePart leftWheel, rightWheel;
    protected EnergySocket energySocket;
    protected ItemSocket itemInSocket, itemOutSocket;

    protected CustomStructureInventory itemOutBuffer = new CustomStructureInventory(1);

    protected int processedTicks;

    @Getter
    protected Data<Integer> storedEnergy = dataContainer.persist("energy", Data.Type.INT, 0);

    public float wheelsAngle;

    private CrusherRecipe recipe;

    OreCrusherInstance(LoadContext context) {
        super(context);
    }

    public OreCrusherInstance(BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        super(Structures.ORE_CRUSHER, location, bodyDef, direction);
    }

    @Override
    public void tick(boolean isVirtual) {
        energySocket.maintainEnergy(this);
        itemOutSocket.attemptDirectItemExport();

        if (recipe == null) {
            List<ItemStack> items = itemInSocket.consumeItems(OreCrusher.INPUT_CRITERIA, 1, true);
            if (!items.isEmpty()) {
                this.recipe = TorusPlugin.getInstance().getRecipeRegistry().getCrusherRecipeByIngredient(items.getFirst());
            }
            state.set(STATE_WORKING, false);
        } else {
            if (processedTicks >= recipe.crushTicks) {
                ItemStack result = recipe.result.asResult();
                itemOutBuffer.addItem(result);
                recipe = null;
                processedTicks = 0;
                state.set(STATE_WORKING, false);
            } else {
                if (!hasSufficientEnergy(structure.getProperty("energy_settings.consumption", PropertyType.INT))) {
                    return;
                }

                if (processedTicks % 2 == 0) {
                    location.world.getBukkit().playSound(location.toBukkitCentered(), Sound.ENTITY_MINECART_RIDING, 0.3f, 0.4f);
                }
                processedTicks++;
                wheelsAngle += (120) / 180f * (float) Math.PI;
                state.set(STATE_WORKING, true);
                consumeEnergy(structure.getProperty("energy_settings.consumption", PropertyType.INT));
            }
        }

    }

    @Override
    protected void setup() throws SetupException {
        leftWheel = getPart("wheel_left");
        rightWheel = getPart("wheel_right");

        itemInSocket = requireSocket("item_connector", ItemSocket.class);
        itemOutSocket = requireSocket("out_connector", ItemSocket.class);
        itemOutSocket.linkedInventory = itemOutBuffer;
        energySocket = requireSocket("power_connector", EnergySocket.class);
        energySocket.maximumInput = structure.getProperty("energy_settings.maximum_input", PropertyType.INT);

        state.set(STATE_WORKING, false, false);
    }

    @Override
    public int getEnergyCapacity() {
        return structure.getProperty("energy_settings.capacity", PropertyType.INT);
    }

    @Override
    public InspectableDataContainer setupInspectableData() {
        return new InspectableDataContainer((byte) 2)
          .property(translatable("inspection.energy_unit"), InspectableDataContainer.TEMPLATE_RF.apply(this))
          .property(translatable("inspection.ore_crusher.recipe"), () -> recipe != null ? recipe.key.toString() : translate("inspection.ore_crusher.recipe.none"));
    }

}
