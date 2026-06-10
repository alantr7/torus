package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.structure.inspection.InspectableProperty;
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

public class FluidTankInstance extends StructureInstance implements FluidContainer, HologramProvider {

    protected StructurePart liquidComponent;

    protected FluidSocket input;

    protected Data<Integer> fluid = dataContainer.persist("fluid", Data.Type.INT, -1);

    protected Data<Integer> stored = dataContainer.persist("stored", Data.Type.INT, 0);

    protected Data<Integer> steam = dataContainer.persist("steam", Data.Type.INT, 0);

    protected Data<Float> temperature = dataContainer.persist("temperature", Data.Type.FLOAT, 0f);

    protected Data<Byte> isVenting = dataContainer.persist("is_venting", Data.Type.BYTE, (byte) 0);

    protected ItemDisplay fluidDisplay;

    protected boolean hasExploded;

    private static final float GAS_FACTOR = 100f;

    private static final Fluid[] ALLOWED_FLUIDS = { Fluid.WATER, Fluid.LAVA };

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
        return (new InspectableDataContainer((byte) 4)
          .property("Fluid", () -> this.getFluid() == null ? null : MathUtils.formatNumber(this.getStoredFluid()) + "/" + MathUtils.formatNumber(this.getFluidCapacity()) + " mb")
          .property("Steam", () -> this.getFluid() != Fluid.WATER ? null : MathUtils.formatNumber((Integer)this.steam.get()) + " mb")
          .property("Boiling", () -> this.getFluid() != Fluid.WATER ? null : String.format("%.1f%%", this.temperature.get()))
          .property("Pressure", () -> this.getFluid() != Fluid.WATER ? null : String.format("%.1f%%", this.calculatePressure() * 100.0F)));
    }

    public void updateInspectionHologram() {
        ((InspectableProperty) this.inspectableDataContainer.lines[0]).setName(this.getFluid() != null ? this.getFluid().name() : "");
        super.updateInspectionHologram();
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
                for (Fluid fluid1 : ALLOWED_FLUIDS) {
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

        if (this.getFluid() == Fluid.WATER) {
            float k = calculateHeat();

            // increase the temperature
            if (k > 0.0F) {
                this.temperature.update(Math.min(100.0F, temperature.get() + k * (100.0F - temperature.get())));
            } else {
                this.temperature.update(Math.max(0.0F, temperature.get() - 1.0F));
            }

            // boil water
            if (temperature.get() > 85.0F) {
                for (int i = 0; i < 5; i++) {
                    for(int j = 0; j < 6; ++j) {
                        if (Math.random() < (double)(1.0F - (100.0F - temperature.get()) / 15.0F)) {
                            spawnBoilingParticles(3 * j);
                        }
                    }

                    spawnSteamParticle();
                }

                int evaporated = (int) (5f * (1.0F - (float)Math.random() * (100.0F - temperature.get()) / 100.0F));
                steam.update(this.steam.get() + (int) (consumeFluid(evaporated) * GAS_FACTOR));
            }

            // condensation
            if (steam.get() < 100) {
                this.steam.update(0);
            } else {
                int condensed = supplyFluid((int) ((this.steam.get() >= 200 ? 200 : this.steam.get()) / GAS_FACTOR));
                this.steam.update(steam.get() - (int) (condensed * GAS_FACTOR));
            }

            // explode if pressure is too high
            float pressure = calculatePressure();
            if (pressure > 1.5F && Math.random() < 0.15f * pressure) {
                hasExploded = true;
                remove();
            }

            // use pressure vents
            else if (pressure >= 1.4f || isVenting.get() == (byte) 1) {
                if (pressure >= 1.4f) isVenting.update((byte) 1);
                if (location.getRelative(0, 3, 0).getStructure() instanceof PressureVentInstance vent) {
                    int steamToVent = (int) Math.max(25, pressure * steam.get() / 20);
                    int ventedSteam = vent.vent(steamToVent);

                    steam.update(steam.get() - ventedSteam);
                }
                if (pressure <= 1.2f) isVenting.update((byte) 0);
            }
        }

    }

    public void updateFluidDisplay() {
        float height = (float) this.stored.get() / (float)this.getFluidCapacity() * 2.75F;
        if (this.fluidDisplay.getItemStack().getType() == Material.BLUE_CONCRETE && fluid.get() != Fluid.WATER.ordinal() || fluidDisplay.getItemStack().getType() == Material.ORANGE_CONCRETE && fluid.get() != Fluid.LAVA.ordinal()) {
            this.fluidDisplay.setItemStack(new ItemStack(fluid.get() == Fluid.WATER.ordinal() ? Material.BLUE_CONCRETE : Material.ORANGE_CONCRETE));
        }

        Transformation transformation = this.fluidDisplay.getTransformation();
        Vector3f translation = transformation.getTranslation();
        translation.y = 0.14F + height / 2.0F;
        Vector3f scale = transformation.getScale();
        scale.y = height;
        this.fluidDisplay.setInterpolationDelay(0);
        this.fluidDisplay.setTransformation(transformation);
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
        return steam.get() != 0 ? Fluid.WATER : fluid.get() != -1 && (stored.get() != 0 || steam.get() != 0) ? Fluid.values()[fluid.get()] : null;
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

    private float calculateHeat() {
        float heat = 0.0F;

        for(int i = -1; i <= 1; ++i) {
            int j = -1;

            while(j <= 1) {
                Material material = this.location.getRelative(i, -1, j).getBlock().getType();
                switch(material) {
                    case CAMPFIRE:
                        heat += 0.006F;
                    default:
                        ++j;
                }
            }
        }

        return heat;
    }

    private float calculatePressure() {
        float liquidRatio = Math.max(0.001f, (1f - (float) stored.get() / getFluidCapacity()));
        return ((float) steam.get() / getFluidCapacity()) / liquidRatio;
    }

    private void spawnBoilingParticles(int delay) {
        Bukkit.getScheduler().runTaskLater(TorusPlugin.getInstance(), () -> {
            float height = (float) this.stored.get() / (float)this.getFluidCapacity() * 2.8f;
            this.location.world.getBukkit().spawnParticle(Particle.BUBBLE_POP, this.location.toBukkitCentered().add(-0.875D + Math.random() * 1.75D, (double)(height + 0.2F), -0.875D + Math.random() * 1.75D), 0);
        }, delay);
    }

    private void spawnSteamParticle() {
        float height = (float) this.stored.get() / (float) this.getFluidCapacity() * 2.8f;
        this.location.world.getBukkit().spawnParticle(Particle.WHITE_SMOKE, this.location.toBukkitCentered().add(-0.875D + Math.random() * 1.75D, (double)(height + 0.2F), -0.875D + Math.random() * 1.75D), 0);
        this.location.world.getBukkit().spawnParticle(Particle.WHITE_SMOKE, this.location.toBukkitCentered().add(-0.875D + Math.random() * 1.75D, (double)(height + 0.2F), -0.875D + Math.random() * 1.75D), 0);
        this.location.world.getBukkit().spawnParticle(Particle.WHITE_SMOKE, this.location.toBukkitCentered().add(-0.875D + Math.random() * 1.75D, (double)(height + 0.2F), -0.875D + Math.random() * 1.75D), 0);
    }

    public void onRemove() {
        if (hasExploded) {
            this.location.world.getBukkit().createExplosion(this.location.toBukkit(), 2.0F);
        }

    }

}
