package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.structure.Structure;
import com.github.alantr7.torus.structure.StructureFlag;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.builder.StructurePartDef;
import com.github.alantr7.torus.structure.builder.StructureSocketDef;
import com.github.alantr7.torus.structure.socket.Socket;
import com.github.alantr7.torus.structure.state.State;
import com.github.alantr7.torus.structure.state.StateType;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.world.Pitch;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class ElevatorInterface extends Structure {

    public static final State<Boolean> STATE_ONLINE = new State<>("online", StateType.BOOLEAN, false);

    public ElevatorInterface() {
        super(TorusPlugin.DEFAULT_ADDON, "elevator_interface", ElevatorInterfaceInstance.class);
        setFlags(StructureFlag.INTERACTABLE | StructureFlag.TICKABLE);
        registerState(STATE_ONLINE);
    }

    @Override
    protected StructureInstance instantiate(@NotNull BlockLocation location, Direction direction, Pitch pitch) {
        return new ElevatorInterfaceInstance(this, location, new StructureBodyDef(new StructurePartDef[]{
          new StructurePartDef("base", new Vector3f()),
          new StructurePartDef("data", new Vector3f(), new StructureSocketDef(
            Socket.Medium.DATA, Socket.FlowDirection.ALL, direction.getOpposite().mask()
          ))
        }), direction);
    }

}
