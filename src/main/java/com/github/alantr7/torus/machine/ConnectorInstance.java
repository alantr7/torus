package com.github.alantr7.torus.machine;

import com.github.alantr7.bytils.buffer.ByteArrayWriter;
import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.gui.structure.ConnectorFilterEditGUI;
import com.github.alantr7.torus.item.ItemCriteria;
import com.github.alantr7.torus.item.ItemReference;
import com.github.alantr7.torus.item.TorusItem;
import com.github.alantr7.torus.structure.HologramProvider;
import com.github.alantr7.torus.structure.inspection.InspectableDataContainer;
import com.github.alantr7.torus.structure.socket.ItemSocket;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.structure.LoadContext;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.data.Data;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.Structures;
import com.github.alantr7.torus.structure.socket.Socket;
import com.github.alantr7.torus.structure.inventory.BukkitStructureInventory;
import com.github.alantr7.torus.world.Pitch;
import com.github.alantr7.torus.world.TorusWorld;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.nio.charset.StandardCharsets;

import static com.github.alantr7.torus.lang.Localization.translatable;
import static com.github.alantr7.torus.lang.Localization.translate;
import static com.github.alantr7.torus.machine.Connector.*;

public class ConnectorInstance extends StructureInstance implements HologramProvider {

    protected Data<Integer> flowDirectionData = dataContainer.persist("flow", Data.Type.INT, 0);

    protected Data<byte[]> filterInternal = dataContainer.persist("filter", Data.Type.BYTE_ARRAY, new byte[0]);

    protected ItemSocket socket;

    protected ItemCriteria inputCriteria = null;

    public ConnectorInstance(BlockLocation location, StructureBodyDef bodyDef, Direction direction, Pitch pitch, Socket.FlowDirection flowDirection) {
        super(Structures.CONNECTOR, location, bodyDef, direction, pitch);
        flowDirectionData.update(flowDirection.ordinal());
        save();
    }

    ConnectorInstance(LoadContext context) {
        super(context);
    }

    @Override
    protected void setup() throws SetupException {
        socket = requireSocket("connector", ItemSocket.class);
        updateCriteria(getFilter());

        for (Direction direction : Direction.values()) {
            if (socket.isConnected(direction)) {
                state.set(getStateFromDirection(direction.relativeTo(this.direction, this.pitch)), true, false);
            }
        }

        if (socket.getConnections() != 0) {
            state.set(STATE_BACK, true, false);
        }
    }

    @Override
    public InspectableDataContainer setupInspectableData() {
        return new InspectableDataContainer((byte) 1)
          .property(translatable("inspection.flow"), () -> translate("inspection.flow." + getFlowDirection().name().toLowerCase()));
    }

    @Override
    public void tick(boolean isVirtual) {
        if (TorusWorld.isItemContainer(location.getRelative(direction.getOpposite()))) {
            socket.linkedInventory = new BukkitStructureInventory(((BlockInventoryHolder) location.getRelative(direction.getOpposite()).getBlock().getState()).getInventory());
        } else {
            return;
        }

        if (getFlowDirection() != Socket.FlowDirection.IN && getFlowDirection() != Socket.FlowDirection.ALL)
            return;

        socket.consumeItems(inputCriteria, 4, false).forEach(socket.linkedInventory::addItem);
    }

    @Override
    public void onSocketConnect(Socket socket, Socket neighbor, Direction direction) {
        state.set(getStateFromDirection(direction.relativeTo(this.direction, this.pitch)), true);
        state.set(STATE_BACK, true);
    }

    @Override
    public void onSocketDisconnect(Socket socket, Socket neighbor, Direction direction) {
        state.set(getStateFromDirection(direction.relativeTo(this.direction, this.pitch)), false);
        if (socket.getConnections() == 0) {
            state.set(STATE_BACK, false);
        }
    }

    public ItemReference[] getFilter() {
        ItemReference[] references = new ItemReference[9];

        byte[] items = filterInternal.get();
        int idx = 0;
        for (int i = 0; i < items.length;) {
            int len = items[i];
            String namespacedId = new String(items, i + 1, len, StandardCharsets.UTF_8);
            String[] sep = namespacedId.split(":");

            i += len + 1;
            references[idx++] = new ItemReference(sep[0], sep[1]);
        }

        return references;
    }

    public void setFilter(ItemReference[] filter) {
        ByteArrayWriter writer = new ByteArrayWriter();
        for (ItemReference itemReference : filter) {
            if (itemReference == null)
                continue;

            String namespacedId = itemReference.providerId + ":" + itemReference.itemId;
            writer.writeU1(namespacedId.length());
            writer.writeBytes(namespacedId.getBytes(StandardCharsets.UTF_8));
        }
        this.filterInternal.update(writer.getBuffer());
        this.updateCriteria(filter);
        save();
    }

    public void updateCriteria(ItemReference[] filter) {
        ItemCriteria criteria = new ItemCriteria();
        int filteredItems = 0;
        for (ItemReference ref : filter) {
            if (ref != null) {
                if (ref.providerId.equals("minecraft"))
                    criteria.materials.add(Material.valueOf(ref.itemId));
                else
                    criteria.ids.add("torus:" + ref.itemId.toLowerCase());
                filteredItems++;
            }
        }
        inputCriteria = filteredItems != 0 ? criteria : null;
    }

    @Override
    public boolean onPlayerInteract(PlayerInteractEvent event, BlockLocation location) {
        TorusItem item = TorusItem.getByItemStack(event.getPlayer().getInventory().getItemInMainHand());
        if (item != null && item.namespacedId.equals("torus:screwdriver")) {
            if (event.getPlayer().isSneaking()) {
                flowDirectionData.update(getFlowDirection() == Socket.FlowDirection.IN ? Socket.FlowDirection.OUT.ordinal() : Socket.FlowDirection.IN.ordinal());
                socket.setFlowDirection(getFlowDirection());

                event.getPlayer().sendMessage( translate("interaction.flow_change.success").replace("{flow}", translate("inspection.flow." + socket.getFlowDirection().name().toLowerCase())));
            } else {
                Vector dr = event.getInteractionPoint().subtract(event.getPlayer().getEyeLocation()).toVector().normalize().multiply(0.07f);
                Vector r = event.getInteractionPoint().subtract(location.toBukkit()).clone().toVector().add(new Vector(-0.5, -0.5, -0.5));

                // Make it relative
                for (int i = 0; i < 18; i++) {
                    for (int j = 0; j < CableInstance.interactionBoxes.length; j++) {
                        BoundingBox box = CableInstance.interactionBoxes[j];
                        if (box.contains(r)) {
                            if (socket.toggleConnection(Direction.values()[j]))
                                return true;
                        }
                    }
                    r.add(dr);
                }

                return false;
            }
            return true;
        }

        new ConnectorFilterEditGUI(event.getPlayer(), this).open();
        return true;
    }



    public Socket.FlowDirection getFlowDirection() {
        return Socket.FlowDirection.values()[flowDirectionData.get()];
    }

}
