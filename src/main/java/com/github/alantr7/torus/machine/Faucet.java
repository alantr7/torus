package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.structure.Structure;
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

public class Faucet extends Structure {

    public Faucet() {
        super(TorusPlugin.DEFAULT_ADDON, "faucet", "Faucet", FaucetInstance.class);
        isTickable = false;
        isInteractable = true;
        isHeavy = false;
    }

    @Override
    protected StructureInstance instantiate(@NotNull BlockLocation location, Direction direction, Pitch pitch) {
        return new FaucetInstance(this, location, new StructureBodyDef(new StructurePartDef[]{
          new StructurePartDef("base", new Vector3f(), new StructureSocketDef(
            Socket.Medium.FLUID, Socket.FlowDirection.IN, direction.getOpposite().mask()
          ))
        }), direction);
    }

}
