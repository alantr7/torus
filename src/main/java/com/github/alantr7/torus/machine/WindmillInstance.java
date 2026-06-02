package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.structure.*;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.data.Data;
import com.github.alantr7.torus.structure.inspection.InspectableDataContainer;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static com.github.alantr7.torus.lang.Localization.translatable;
import static com.github.alantr7.torus.machine.Windmill.STATE_ACTIVE;

public class WindmillInstance extends StructureInstance implements RotationSource, Inspectable {

    @Getter
    float heightEfficiency;

    @Getter
    float efficiency;

    protected Data<Byte> isObstructed = dataContainer.persist("obstructed", Data.Type.BYTE, (byte) 1);

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

        efficiency = isObstructed.get() == 1 ? 0 : heightEfficiency;
        if (isObstructed.get() == 0 && !location.world.getBukkit().isClearWeather()) {
            efficiency = Math.min(1f, 1.2f * efficiency);
        }
    }

    @Override
    protected void setup() throws SetupException {
        heightEfficiency = (float) Math.pow(Math.E, -8f/(location.y / 8f + 8f)) * 1.15505059f;
        state.set(STATE_ACTIVE, heightEfficiency != 0, false);
    }

    @Override
    public InspectableDataContainer setupInspectableData() {
        return new InspectableDataContainer((byte) 1)
          .property(translatable("inspection.windmill.efficiency"), () -> (int) (efficiency * 100) + "%");
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
