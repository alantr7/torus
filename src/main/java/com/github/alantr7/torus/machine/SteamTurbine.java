package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.structure.Structure;
import com.github.alantr7.torus.structure.StructureFlag;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.builder.StructurePartDef;
import com.github.alantr7.torus.structure.builder.StructureSocketDef;
import com.github.alantr7.torus.structure.socket.Socket;
import com.github.alantr7.torus.utils.ByteArrayBuilder;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.world.Pitch;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import static com.github.alantr7.torus.lang.Localization.translatable;

public class SteamTurbine extends Structure {

    public SteamTurbine() {
        super(TorusPlugin.DEFAULT_ADDON, "steam_turbine", translatable("structure.steam_turbine.name"), SteamTurbineInstance.class);
        setFlags(StructureFlag.COLLIDABLE | StructureFlag.TICKABLE | StructureFlag.HEAVY);
        setHologramOffset(new Vector3f(0f, 0f, 2f));
        setHologramTranslation(new Vector3f(1.1f, 0f, 1f));
    }

    @Override
    protected void createBounds(ByteArrayBuilder builder) {
        builder.add(0, 0, 0);
        builder.add(0, 0, 1);
        builder.add(0, 0, 2);
        builder.add(0, 0, 3);
        builder.add(0, 0, 4);
        builder.add(0, 1, 1);
        builder.add(0, 1, 2);
        builder.add(0, 1, 3);
        builder.add(1, 0, 2);
        builder.add(-1, 0, 2);
        builder.add(1, 0, 4);
        builder.add(-1, 0, 4);
    }

    @Override
    protected StructureInstance instantiate(@NotNull BlockLocation location, Direction direction, Pitch pitch) {
        return new SteamTurbineInstance(location, new StructureBodyDef(new StructurePartDef[]{
          new StructurePartDef("base", new Vector3f()),
          new StructurePartDef("shaft", new Vector3f()),
          new StructurePartDef("in_fluid", new Vector3f(0, 0, 0), new StructureSocketDef(
            Socket.Medium.FLUID, Socket.FlowDirection.IN, direction.mask() | direction.getOpposite().mask()
          )),
        }), direction);
    }

}
