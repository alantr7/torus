package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.structure.LoadContext;
import com.github.alantr7.torus.structure.Structure;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.utils.MathUtils;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class ElevatorInterfaceInstance extends StructureInstance {

    protected BoundingBox[] interactionBoxes = new BoundingBox[8];

    protected TextDisplay[] buttons = new TextDisplay[8];

    protected static final float panelWidth = 0.8f;

    protected static final float panelHeight = 0.9f;

    ElevatorInterfaceInstance(LoadContext context) {
        super(context);
    }

    public ElevatorInterfaceInstance(Structure structure, BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        super(structure, location, bodyDef, direction);
    }

    @Override
    protected void setup() throws SetupException {
        float minX = -0.5f;
        float maxX = 0.5f;
        float minZ = 0.4f;
        float maxZ = 0.5f;
        float[] matrixA = MathUtils.rotateVector(new float[] { minX, minZ }, direction.rotH);
        float[] matrixB = MathUtils.rotateVector(new float[] { maxX, maxZ }, direction.rotH);


        for (int i = 0; i < 8; i++) {
            interactionBoxes[i] = new BoundingBox(matrixA[0], (1 - panelHeight) / 2f + (7 - i) / 8f * panelHeight, matrixA[1], matrixB[0], (1 - panelHeight) / 2f + (8 - i) / 8f * panelHeight, matrixB[1]);
        }
    }

    @Override
    public void onModelSpawn() {
        updateScreen();
    }

    @Override
    public void onModelDestroy() {
        for (TextDisplay button : buttons) {
            if (button != null)
                button.remove();
        }
    }

    @Override
    public boolean onPlayerInteract(PlayerInteractAtEntityEvent event, Interaction entity) {
        Vector dr = event.getPlayer().getEyeLocation().getDirection().multiply(0.07);
        Vector r = location.toBukkit().add(event.getClickedPosition()).subtract(location.toBukkit()).clone().toVector();

        // middle for r: x = 0, y = 0.5, z = 0

        event.getPlayer().sendMessage("r: " + r);
        event.getPlayer().sendMessage("dr: " + dr);

        // Make it relative
        for (int i = 0; i < 18; i++) {
            for (int j = 0; j < interactionBoxes.length; j++) {
                BoundingBox box = interactionBoxes[j];
                if (box.contains(r)) {
                    event.getPlayer().sendMessage("#" + (j + 1));
                    return true;
                }
            }
            r.add(dr);
        }

        return false;
    }

    protected void updateScreen() {
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] != null) {
                buttons[i].remove();
            }
            buttons[i] = null;
        }

        float[] positionXZ = MathUtils.rotateVector(new float[] { 0f, 0.35f }, direction.rotH);

        for (int i = 0; i < buttons.length; i++) {
            Location buttonLocation = location.toBukkitCentered();
            buttonLocation.add(positionXZ[0], (1 - panelHeight) / 2f + (i / 8f) * panelHeight, positionXZ[1]);

            TextDisplay button = location.world.getBukkit().spawn(buttonLocation, TextDisplay.class);
            button.setRotation(direction.rotH + 180, 0);
            button.setDisplayWidth(1);
            button.setDefaultBackground(false);
            button.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            button.setDisplayHeight(panelHeight / 8);
            button.setPersistent(false);
            button.setShadowed(false);
            button.setBrightness(new Display.Brightness(15, 15));
            button.setText(ChatColor.BLACK + "Floor #" + (8 - i));

            Transformation transformation = button.getTransformation();
            transformation.getScale().set(0.4f, 0.4f, 0.4f);
            button.setTransformation(transformation);

            buttons[i] = button;
        }
    }

}
