package com.github.alantr7.torus.structure;

import com.github.alantr7.bytils.buffer.ByteArrayReader;
import com.github.alantr7.bytils.buffer.ByteArrayWriter;
import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.exception.MissingDataException;
import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.item.TorusItem;
import com.github.alantr7.torus.log.Category;
import com.github.alantr7.torus.log.TorusLogger;
import com.github.alantr7.torus.structure.inspection.InspectableText;
import com.github.alantr7.torus.structure.socket.*;
import com.github.alantr7.torus.utils.MathUtils;
import com.github.alantr7.torus.utils.StringPool;
import com.github.alantr7.torus.model.*;
import com.github.alantr7.torus.model.animation.Animation;
import com.github.alantr7.torus.model.animation.AnimationProvider;
import com.github.alantr7.torus.model.controller.ModelContainer;
import com.github.alantr7.torus.model.de_provider.DisplayEntitiesDefaultAnimations;
import com.github.alantr7.torus.plugin.Permissions;
import com.github.alantr7.torus.structure.data.Data;
import com.github.alantr7.torus.structure.inspection.InspectableDataContainer;
import com.github.alantr7.torus.structure.state.StructureState;
import com.github.alantr7.torus.world.*;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.builder.StructurePartDef;
import com.github.alantr7.torus.structure.data.DataContainer;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.lang.reflect.Constructor;
import java.util.*;

public abstract class StructureInstance {

    public final Structure structure;

    public final BlockLocation location;

    public final Direction direction;

    public final Pitch pitch;

    public final Direction facing;

    public final DataContainer dataContainer;

    @Getter
    private Status status = Status.NOT_LOADED;

    @Getter
    protected final StructureState state;

    public boolean isCorrupted;

    public boolean isDirty = false;

    public boolean isRemoved = false;

    protected Map<String, StructurePart> parts = new HashMap<>();

    protected Map<SocketLocation, Socket> sockets = new HashMap<>();

    protected Map<String, Socket> socketsByName = new HashMap<>();

    private byte[] collisionVectors;

    private int[][] occupiedChunks;

    public Model model = new Model(ModelTemplate.EMPTY);

    @Getter
    private boolean isModelUpdateScheduled;

    @Getter @Accessors(fluent = true)
    private boolean hasActiveAnimations;

    @Nullable
    public Interaction interactionEntity;

    public TextDisplay inspectionHologram;

    public InspectableDataContainer inspectableDataContainer;

    @Getter
    private final FlowMeter flowMeter; // TODO: I hate this so much, but it's a temporary solution until I make traits system

    public StructureInstance(LoadContext context) {
        this(context.structure(), context.location(), context.data(), null, context.direction(), context.pitch());
    }

    public StructureInstance(Structure structure, BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        this(structure, location, bodyDef, direction, Pitch.FORWARD);
    }

    public StructureInstance(Structure structure, BlockLocation location, StructureBodyDef bodyDef, Direction direction, Pitch pitch) {
        this(structure, location, new DataContainer(), bodyDef, direction, pitch);
    }

    protected StructureInstance(Structure structure, BlockLocation location, DataContainer dataContainer, StructureBodyDef bodyDef, Direction direction, Pitch pitch) {
        this.structure = structure;
        this.location = location;
        this.direction = direction;
        this.pitch = pitch;
        this.facing = pitch == Pitch.FORWARD
          ? direction
          : pitch == Pitch.UP ? Direction.UP : Direction.DOWN;
        this.dataContainer = dataContainer;
        this.dataContainer.structure = this;
        this.state = new StructureState(this);
        if (bodyDef != null) {
            initBody(bodyDef);
        }
        setOccupiedChunks();
        flowMeter = new FlowMeter(location.world);
    }

    private void initBody(StructureBodyDef bodyDef) {
        for (StructurePartDef partDef : bodyDef.parts()) {
            MathUtils.applyRotation(partDef.offset, direction.rotH);
            StructurePart part = new StructurePart(this, new BlockLocation(location.world, (int) partDef.offset.x, (int) partDef.offset.y, (int) partDef.offset.z), partDef.name);
            parts.put(partDef.name, part);

            if (partDef.socketDef != null) {
                createSocket(partDef.socketDef.medium(), parts.get(partDef.name), pitch.transform(partDef.socketDef, direction), partDef.socketDef.direction());
            }
        }
    }

    protected Socket createSocket(Socket.Medium medium, StructurePart part, int allowedConnections, Socket.FlowDirection direction) {
        Socket socket = switch (medium) {
            case ENERGY -> new EnergySocket(part, allowedConnections, direction);
            case ITEM -> new ItemSocket(part, allowedConnections, direction);
            case FLUID -> new FluidSocket(part, allowedConnections,direction);
        };
        socket.structure = this;
        for (Direction possibleDirection : Direction.values()) {
            if (socket.isConnectableFrom(possibleDirection)) {
                sockets.put(new SocketLocation(part.absoluteLocation.getRelative(possibleDirection), socket.medium), socket);
            }
        }
        socketsByName.put(part.name, socket);
        return socket;
    }

    public void onModelSpawn() {
    }

    public void onModelDestroy() {
    }

    public final void makePhysical() {
        if (status == Status.PHYSICAL)
            return;

        status = Status.PHYSICAL;

        // Setup model
        try {
            if (!structure.hasCollision) {
                interactionEntity = location.world.getBukkit().spawn(location.toBukkitCentered(), Interaction.class);
                interactionEntity.setInteractionWidth(1f);
                interactionEntity.setInteractionHeight(1f);
                interactionEntity.setResponsive(true);
                interactionEntity.setPersistent(false);
            }

            updateModel();
            onModelSpawn();
        } catch (Exception exc) {
            TorusLogger.error(Category.STRUCTURES, "Could not spawn the model for " + structure.id + " at " + location);
            exc.printStackTrace();
        }

        // Setup information hologram
        setupInspectionTooltip();
    }

    public final void makeVirtual() {
        if (status == Status.VIRTUAL)
            return;

        Status oldStatus = status;
        status = Status.VIRTUAL;

        // Clean up if it was PHYSICAL
        if (oldStatus == Status.PHYSICAL) {
            if (!structure.hasCollision && interactionEntity != null) {
                interactionEntity.remove();
            }

            model.remove();
            onModelDestroy();
            if (inspectionHologram != null)
                inspectionHologram.remove();
        }

        model = new Model(ModelTemplate.EMPTY);
    }

    private void setupInspectionTooltip() {
        if (!isCorrupted && !(this instanceof Inspectable)) {
            return;
        }
        if (inspectableDataContainer.inspectableBlocks.isEmpty()) {
            byte[] bounds = getCollisionVectors();
            for (int i = 0; i < bounds.length; i += 3) {
                inspectableDataContainer.inspectableBlocks.add(location.getRelative(bounds[i], bounds[i + 1], bounds[i + 2]));
            }
        }
        spawnInspectionTooltip();
        if (isCorrupted) {
            inspectionHologram.setText(org.bukkit.ChatColor.RED + "Corrupted Structure\n" + StructureInstance.COLOR_PROPERTY + "Try to place it again");
        }
    }

    public final void handleLoad() {
        for (Socket socket : socketsByName.values()) {
            location.world.networkManager.queueLoaded(socket);
        }
    }

    public final void handleUnload() {
        if (status == Status.PHYSICAL) {
            try {
                if (!structure.hasCollision && interactionEntity != null) {
                    interactionEntity.remove();
                }

                model.remove();
                onModelDestroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (inspectionHologram != null)
                inspectionHologram.remove();
        }
        status = Status.NOT_LOADED;
        for (Socket socket : socketsByName.values()) {
            location.world.networkManager.queueUnloaded(socket);
        }
    }

    public void updateModel() {
        ModelContainer modelContainer = structure.getModelController().getModel(state);
        if (modelContainer == null) {
            if (model == null || model.template != ModelTemplate.EMPTY) {
                if (model != null)
                    model.remove();

                model = ModelTemplate.EMPTY.toModel(location, direction, pitch);
            }
            hasActiveAnimations = false;
        } else {
            if (model != null) {
                model = modelContainer.compositeModel.upgradeModel(this.model, location, direction, pitch);
            } else {
                model = modelContainer.compositeModel.toModel(location, direction, pitch);
            }

            // Reset active animations
            hasActiveAnimations = false;

            // Load default animations if Torus structure
            if (structure.addon.id.equals("torus")) {
                DisplayEntitiesDefaultAnimations.inject(this);
            }

            // Play animation
            // TODO: Animations for multipart models
            if (structure.getModelController().type == ModelType.SINGLEPART) {
                if (modelContainer.matches.getFirst().animations != null) {
                    model.parts.forEach((name, part) -> {
                        AnimationProvider<PartModel, Animation> animationProvider = model.template.parts.get(name).animationMap.get(modelContainer.matches.getFirst().animations);
                        if (animationProvider != null) {
                            part.setAnimation(animationProvider.get(part));
                            hasActiveAnimations = true;
                        }
                    });
                }

                // Stop animation if it was playing (and the model didn't change)
                else {
                    model.parts.forEach((name, part) -> part.setAnimation(null));
                }
            }
        }

        isModelUpdateScheduled = false;
    }

    public void scheduleModelUpdate() {
        TorusPlugin.getInstance().getModelManager().scheduleModelUpdate(this);
        isModelUpdateScheduled = true;
    }

    public void onSocketConnect(Socket socket, Socket neighbor, Direction direction) {
    }

    public void onSocketDisconnect(Socket socket, Socket neighbor, Direction direction) {
    }

    public void spawnInspectionTooltip() {
        float[] offset = MathUtils.rotateVectors(structure.hologramOffset, direction.rotH, 0);
        inspectionHologram = location.world.getBukkit().spawn(location.toBukkitCentered().add(offset[0], offset[1], offset[2]), TextDisplay.class);
        inspectionHologram.setBillboard(Display.Billboard.CENTER);
        inspectionHologram.setPersistent(false);
        inspectionHologram.setSeeThrough(true);
        inspectionHologram.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        inspectionHologram.setAlignment(TextDisplay.TextAlignment.LEFT);
        inspectionHologram.setVisibleByDefault(false);
        inspectionHologram.setShadowed(true);

        Transformation transformation = inspectionHologram.getTransformation();
        transformation.getScale().set(0.7f, 0.7f, 0.7f);
        transformation.getTranslation().set(structure.hologramTranslation);
        inspectionHologram.setTransformation(transformation);
    }

    public byte[] getCollisionVectors() {
        if (this.collisionVectors != null)
            return this.collisionVectors;

        return this.collisionVectors = MathUtils.rotateVectors(structure.collisionVectors, direction);
    }

    public StructurePart requirePart(String name) throws MissingDataException {
        StructurePart part = parts.get(name);
        if (part == null)
            throw new MissingDataException("Component by name '" + name + "' could not be found.");

        return part;
    }

    public StructurePart getPart(String name) {
        return parts.get(name);
    }

    public Collection<StructurePart> getParts() {
        return parts.values();
    }

    public Socket getSocket(BlockLocation location, Socket.Medium medium) {
        return sockets.get(new SocketLocation(location, medium));
    }

    public Socket getSocket(String name) {
        return socketsByName.get(name);
    }

    public Socket requireSocket(String name) throws MissingDataException {
        Socket socket = socketsByName.get(name);
        if (socket == null)
            throw new MissingDataException("Socket by name '" + name + "' could not be found.");

        return socket;
    }

    @SuppressWarnings("unchecked")
    public <T extends Socket> T requireSocket(String name, Class<T> socketType) throws MissingDataException {
        Socket socket = socketsByName.get(name);
        if (socket == null)
            throw new MissingDataException("Socket by name '" + name + "' could not be found.");

        if (!socketType.isInstance(socket))
            throw new MissingDataException("Types mismatch for socket '" + name + "'. Expected '" + socketType.getSimpleName() + "' but got '" + socket.getClass().getSimpleName() + "'");

        return (T) socket;
    }

    public Collection<Socket> getSockets() {
        return socketsByName.values();
    }

    public Map<SocketLocation, Socket> getSocketsMap() {
        return sockets;
    }

    private void setOccupiedChunks() {
        byte[] bounds = getCollisionVectors();
        Set<Vector2i> positions = new HashSet<>();

        for (int i = 0; i < bounds.length; i+=3) {
            positions.add(new Vector2i((location.x + bounds[i]) >> 4, (location.z + bounds[i + 2]) >> 4));
        }

        this.occupiedChunks = positions.stream().map(v -> new int[]{v.x, v.y}).toArray(int[][]::new);
    }

    /**
     * Structures that are loaded, but chunks that they belong to are not, are marked as virtual.
     * @return true if structure is virtual, false if physical
     */
    public boolean isUnloaded() {
        if (status == Status.NOT_LOADED)
            return true;

        for (int[] chunkPos : occupiedChunks) {
            if (location.world.getChunkAt(chunkPos[0], chunkPos[1]) != null)
                return false;
        }
        return true;
    }

    public void tick(boolean isVirtual) {}

    static final ChatColor COLOR_STRUCTURE_NAME = ChatColor.of("#ff8854");
    static final ChatColor COLOR_PROPERTY = ChatColor.of("#cfcfcf");
    public final void updateInspectionHologram() {
        if (isCorrupted)
            return;

        StringBuilder parent = new StringBuilder();
        parent.append(COLOR_STRUCTURE_NAME).append(ChatColor.BOLD).append(structure.getName()).append("\n");
        for (InspectableText var : inspectableDataContainer.lines) {
            String value = var.getText();
            if (value != null) {
                parent.append(COLOR_PROPERTY).append(value).append("\n");
            }
        }
        inspectionHologram.setText(parent.toString());
    }

    protected abstract void setup() throws SetupException;

    public void corrupt() {
        isCorrupted = true;
        if (inspectionHologram != null) {
            inspectionHologram.remove();
        }
        setupInspectionTooltip();
    }

    public void onRemove() {};

    @Nullable
    public UUID getOwnerId() {
        return dataContainer.getOrDefault("owner_id", Data.Type.UUID, null);
    }

    public void setOwnerId(@Nullable UUID id) {
        dataContainer.persist("owner_id", Data.Type.UUID, id);
    }

    public boolean testOwnership(@NotNull Player player) {
        UUID ownerId = getOwnerId();
        return player.hasPermission(Permissions.STRUCTURE_BREAK_OTHERS) || (ownerId != null && player.getUniqueId().equals(ownerId));
    }

    public ItemStack toItem(boolean includeData) {
        TorusItem torusItem = TorusPlugin.getInstance().getItemRegistry().getItemByStructure(structure);
        if (torusItem == null)
            return null;

        ItemStack item = torusItem.toItemStack().clone();
        if (!includeData || structure.portableData.isEmpty())
            return item;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!structure.portableData.isEmpty()) {
            List<String> lore = meta.getLore();
            lore.add("");
            lore.add(ChatColor.GRAY + "Data:");
            dataContainer.getEntries().forEach((key, data) -> {
                if (!structure.portableData.contains(key)) // TODO: Iterate through whitelist instead
                    return;

                lore.add(ChatColor.GRAY + " " + key + ": " + ChatColor.WHITE + (data.get().getClass().isArray() ? "array" : data.get()));
            });
            meta.setLore(lore);
        }

        StringPool strings = new StringPool();
        ByteArrayWriter stringsBytes = new ByteArrayWriter();

        byte[] bytes = dataContainer.toBytes(strings);
        for (int i = 0; i < strings.getSize(); i++) {
            stringsBytes.writeString(strings.at(i));
        }

        PersistentDataContainer structureData = pdc.getAdapterContext().newPersistentDataContainer();
        structureData.set(new NamespacedKey(TorusPlugin.getInstance(), "string_pool"), PersistentDataType.BYTE_ARRAY, stringsBytes.getBuffer());
        structureData.set(new NamespacedKey(TorusPlugin.getInstance(), "data_container"), PersistentDataType.BYTE_ARRAY, bytes);

        pdc.set(new NamespacedKey(TorusPlugin.getInstance(), "structure_data"), PersistentDataType.TAG_CONTAINER, structureData);

        item.setItemMeta(meta);
        return item;
    }

    public void remove() {
        location.world.removeStructure(this);
    }

    public void save() {
        isDirty = true;
        location.getChunk().isUnsaved = true;
    }

    public boolean onPlayerInteract(PlayerInteractEvent event, BlockLocation location) {
        return false;
    }

    public void save(ByteArrayWriter buffer, StringPool keys) {
        // Structure ID
        buffer.writeU2(structure.numericId);

        int basePointer = buffer.getPointer();

        // Structure Length
        buffer.writeU2(0);

        // Location
        buffer.writeBytes(ByteArrayWriter.toBytes(location.x & 0xf, 1));;
        buffer.writeBytes(ByteArrayWriter.toBytes(location.y, 2));
        buffer.writeBytes(ByteArrayWriter.toBytes(location.z & 0xf, 1));

        // Direction
        buffer.writeU1((pitch.ordinal() << 4) | direction.ordinal());

        // Components Length + Connectors Length
        buffer.writeU1((parts.size() << 4) | sockets.size());
        // Components
        parts.forEach((name, part) -> {
            // Name
            buffer.writeU1(keys.pool(name));

            // Component offset
            buffer.writeU1(((part.relativeLocation.x + 7) << 4) | (part.relativeLocation.z + 7));
            buffer.writeU1(part.relativeLocation.y);
        });

        // Connectors
        sockets.forEach((l, connector) -> {
            // Linked component
            buffer.writeU1(keys.pool(connector.getComponent().name));

            int data = connector.medium.ordinal();
            data = (data << 4) | connector.getFlowDirection().ordinal();

            buffer.writeU1(connector.getAllowedConnections());
            buffer.writeU1(connector.getConnections());
            buffer.writeU1(data);
        });

        // Data Container
        buffer.writeBytes(dataContainer.toBytes(keys));
        dataContainer.setDirty(false);

        int returnPointer = buffer.getPointer();
        buffer.setPointer(basePointer);
        buffer.writeU2(returnPointer - basePointer - 2);
        buffer.setPointer(returnPointer);
    }

    public static StructureInstance fromBytes(TorusRegion region, TorusChunk chunk, ByteArrayReader reader, int structureId) {
        Structure structure = TorusPlugin.getInstance().getStructureRegistry().getStructure(structureId);

        // Location
        int x = ByteArrayReader.toInt(reader.readBytes(1));
        int y = ByteArrayReader.toInt(reader.readBytes(2));
        int z = ByteArrayReader.toInt(reader.readBytes(1));
        BlockLocation location = new BlockLocation(chunk.world, (chunk.position.x << 4) | x, y, (chunk.position.y << 4) | z);

        // Direction
        int pitchDirection = reader.readU1();
        int pitch = (pitchDirection >> 4) & 0x0f;
        int direction = pitchDirection & 0x0f;

        // Components Length + Connectors Length
        int counts = reader.readU1();
        int partsLength = (counts >> 4) & 0x0f;
        int connectorsLength = counts & 0x0f;

        Map<String, StructurePart> components = new HashMap<>(partsLength);
        Map<SocketLocation, Socket> sockets = new HashMap<>(connectorsLength);

        for (int i = 0; i < partsLength; i++) {
            // Name
            String name = region.strings.at(reader.readU1());

            // Component offset
            int packedXZ = reader.readU1();
            int cx = ((packedXZ >> 4) & 0xf) - 7;
            int cz = (packedXZ & 0xf) - 7;
            int cy = reader.readU1();

            StructurePart component = new StructurePart(name, location.getRelative(cx, cy, cz), new BlockLocation(chunk.world, cx, cy, cz), Direction.values()[direction]);
            components.put(name, component);
        }

        for (int i = 0; i < connectorsLength; i++) {
            // Linked component
            String linkedComponent = region.strings.at(reader.readU1());
            StructurePart component = components.get(linkedComponent);

            int allowedConnections = reader.readU1();
            int connections = reader.readU1();
            int data = reader.readU1();
            int flowDirection = data & 0x0f;
            int matterOrdinal = (data >> 4) & 0x0f;

            if (matterOrdinal < Socket.Medium.values().length && flowDirection < Socket.FlowDirection.values().length) {
                Socket socket = switch (Socket.Medium.values()[matterOrdinal]) {
                    case ENERGY -> new EnergySocket(component, allowedConnections, Socket.FlowDirection.values()[flowDirection]);
                    case ITEM -> new ItemSocket(component, allowedConnections, Socket.FlowDirection.values()[flowDirection]);
                    case FLUID -> new FluidSocket(component, allowedConnections, Socket.FlowDirection.values()[flowDirection]);
                };
                socket.setConnections(connections);

                for (Direction possibleDirection : Direction.values()) {
                    if (socket.isConnectableFrom(possibleDirection)) {
                        sockets.put(new SocketLocation(component.absoluteLocation.getRelative(possibleDirection), socket.medium), socket);
                    }
                }
            } else {
                System.err.println("Invalid matter or direction!");
            }
        }

        // Data Container
        DataContainer dataContainer = DataContainer.fromBytes(reader, region.strings);

        if (structure == null) {
            TorusLogger.error(Category.STRUCTURES, "Unrecognized structure ID: " + structureId);
            return null;
        }

        Class<? extends StructureInstance> instanceClass = structure.instanceClass;

        try {
            Constructor<? extends StructureInstance> constructor = instanceClass.getDeclaredConstructor(LoadContext.class);
            constructor.setAccessible(true);

            StructureInstance instance = constructor.newInstance(new LoadContext(structure, location, Direction.values()[direction], Pitch.values()[pitch], dataContainer));
            dataContainer.structure = instance;
            instance.parts.putAll(components);
            instance.sockets.putAll(sockets);
            sockets.forEach((l, c) -> {
                c.structure = instance;
                instance.socketsByName.put(c.getComponent().name, c);
            });

            return instance;
        } catch (Exception e) {
            TorusLogger.error(Category.STRUCTURES, "Could not instantiate " + instanceClass.getName());
            e.printStackTrace();
        }

        return null;
    }

}
