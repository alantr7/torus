package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.structure.Structure;
import com.github.alantr7.torus.structure.StructureFlag;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.builder.StructurePartDef;
import com.github.alantr7.torus.structure.builder.StructureSocketDef;
import com.github.alantr7.torus.structure.socket.Socket;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.world.Pitch;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import static com.github.alantr7.torus.lang.Localization.translatable;
import static com.github.alantr7.torus.machine.EnergyCable.*;

public class DataCable extends Structure {

    public DataCable() {
        super(TorusPlugin.DEFAULT_ADDON, "data_cable", translatable("structure.data_cable.name"), CableInstance.class);
        setFlags(StructureFlag.INTERACTABLE | StructureFlag.COLLIDABLE);
        registerState(STATE_NORTH);
        registerState(STATE_EAST);
        registerState(STATE_SOUTH);
        registerState(STATE_WEST);
        registerState(STATE_UP);
        registerState(STATE_DOWN);
    }

    @Override
    protected StructureInstance instantiate(@NotNull BlockLocation location, Direction direction, Pitch pitch) {
        StructurePartDef base = new StructurePartDef("base", new Vector3f(), new StructureSocketDef(
          Socket.Medium.DATA, Socket.FlowDirection.ALL, 0b111111
        ));
        return new CableInstance(location, new StructureBodyDef(new StructurePartDef[]{base}), Socket.Medium.DATA);
    }

}
