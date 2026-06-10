package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.structure.*;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.data.Data;
import com.github.alantr7.torus.structure.inspection.InspectableDataContainer;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Cuboid;
import com.github.alantr7.torus.world.Direction;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.WorldType;
import org.bukkit.entity.LivingEntity;

import static com.github.alantr7.torus.lang.Localization.translatable;
import static com.github.alantr7.torus.lang.Localization.translate;
import static com.github.alantr7.torus.machine.Windmill.STATE_ACTIVE;

public class WindmillInstance extends StructureInstance implements RotationSource, HologramProvider {

    @Getter
    float heightEfficiency;

    @Getter
    float efficiency;

    protected Data<Byte> isObstructed = dataContainer.persist("obstructed", Data.Type.BYTE, (byte) 1);

    protected final Cuboid damageCuboid;
    {
        Direction right = direction.getRight();
        damageCuboid = new Cuboid(
          location.getRelative(right.modX * -2, -2, right.modZ * -2),
          location.getRelative(right.modX * 2, 2, right.modZ * 2)
        );
    }

    private int ticks = 0;

    WindmillInstance(LoadContext context) {
        super(context);
    }

    public WindmillInstance(BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        super(Structures.WINDMILL, location, bodyDef, direction);
        dataContainer.persist("_version.cv", Data.Type.INT, 2);
    }

    @Override
    public void tick(boolean isVirtual) {
        // check if obstructed
        if (!isVirtual && ticks % 4 == 0) {
            isObstructed.update(isObstructed() ? (byte) 1: 0);
        }

        // hurt entities
        if (!isVirtual && isObstructed.get() == 0) {
            for (LivingEntity entity : location.toBukkit().getNearbyLivingEntities(2, 2, 2)) {
                if (damageCuboid.contains(entity.getLocation()) || damageCuboid.contains(entity.getEyeLocation())) {
                    entity.damage(2d);
                }
            }
        }

        efficiency = isObstructed.get() == 1 ? 0 : heightEfficiency;
        if (isObstructed.get() == 0 && !location.world.getBukkit().isClearWeather()) {
            efficiency = Math.min(1f, 1.2f * efficiency);
        }
        ticks++;
    }

    @Override @SuppressWarnings("deprecation")
    protected void setup() throws SetupException {
        if (location.world.getBukkit().getWorldType() == WorldType.FLAT) {
            heightEfficiency = 1.0f;
        } else {
            heightEfficiency = (float) Math.pow(Math.E, -8f / (location.y / 8f + 8f)) * 1.15505059f;
        }
        state.set(STATE_ACTIVE, heightEfficiency != 0, false);
    }

    @Override
    public InspectableDataContainer setupInspectableData() {
        return new InspectableDataContainer((byte) 2)
          .line(() -> isObstructed.get() == 1 ? translate("inspection.windmill.obstructed") : null)
          .property(translatable("inspection.rpm"), () -> (int) Math.ceil(efficiency * 20f) + "");
    }

    @Override
    public void onModelSpawn() {
        // temporary solution for removing old barriers
        if (dataContainer.getOrDefault("_version.cv", Data.Type.INT, 1) == 1) {
            for (int i = 1; i <= 3; i++) {
                location.getRelative(0, i, 0).toBukkit().getBlock().setType(Material.AIR);
            }
            dataContainer.persist("_version.cv", Data.Type.INT, 0).update(2);
        }
    }

    static int radius2 = 3 * 3;
    protected boolean isObstructed() {
        Direction right = direction.getRight();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x == 0 && z == 0)
                    continue;

                if (x*x + z*z <= radius2) {
                    BlockLocation relative = location.getRelative(z * right.modX, x, z * right.modZ);
                    if (!relative.getBlock().getType().isAir()) {
                        return true;
                    }
                }
            }
        }
        return false;
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
