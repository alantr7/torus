package com.github.alantr7.torus.structure;

import com.github.alantr7.torus.api.addon.TorusAddon;
import com.github.alantr7.torus.api.resource.ResourceLocation;
import com.github.alantr7.torus.lang.Translatable;
import com.github.alantr7.torus.log.Category;
import com.github.alantr7.torus.log.TorusLogger;
import com.github.alantr7.torus.structure.config.StandardConfigGenerator;
import com.github.alantr7.torus.structure.property.Property;
import com.github.alantr7.torus.structure.property.PropertyLoader;
import com.github.alantr7.torus.structure.property.PropertyType;
import com.github.alantr7.torus.utils.MathUtils;
import com.github.alantr7.torus.model.ModelTemplate;
import com.github.alantr7.torus.model.ModelType;
import com.github.alantr7.torus.model.controller.ModelCase;
import com.github.alantr7.torus.model.controller.ModelController;
import com.github.alantr7.torus.structure.inspection.InspectableDataContainer;
import com.github.alantr7.torus.structure.state.State;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.utils.ByteArrayBuilder;
import com.github.alantr7.torus.world.Pitch;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.io.InputStreamReader;
import java.util.*;

import static com.github.alantr7.torus.lang.Localization.translate;

public abstract class Structure {

    public final TorusAddon addon;

    public final String namespacedId;

    public final String id;

    public boolean isEnabled;

    public int numericId = -1;

    @Getter
    protected final Class<? extends StructureInstance> instanceClass;

    public ResourceLocation configResource;

    public StandardConfigGenerator configGenerator;

    private final Map<String, Property<?>> properties = new HashMap<>();

    @Getter
    protected final Map<String, State<?>> allowedStates = new HashMap<>();

    @Getter
    protected byte[] collisionVectors = { 0, 0, 0 };

    @Getter
    protected byte[] size;

    @Getter
    private int flags = 0;

    @Getter
    protected int version = 1;

    @Getter @Setter
    private ModelController modelController = new ModelController(ModelType.SINGLEPART, Collections.singleton(new ModelCase(
      Collections.emptyMap(), ModelTemplate.EMPTY, null
    )));

    public Structure(TorusAddon addon, String id, Class<? extends StructureInstance> instanceClass) {
        this(addon, id, "Untitled Structure", instanceClass);
    }

    public Structure(TorusAddon addon, String id, Translatable name, Class<? extends StructureInstance> instanceClass) {
        this(addon, id, "@" + name.key, instanceClass);
    }

    public Structure(TorusAddon addon, String id, String name, Class<? extends StructureInstance> instanceClass) {
        this(addon, addon.id + ":" + id, id, name, instanceClass);
    }

    protected Structure(TorusAddon addon, String namespacedId, String id, String name, Class<? extends StructureInstance> instanceClass) {
        this.addon = addon;
        this.id = id;
        this.namespacedId = namespacedId;
        registerProperty(new Property<>("general_settings.name", PropertyType.STRING, name));
        registerProperty(new Property<>("general_settings.placement_offset", PropertyType.VECTOR3I, new Vector3i()));
        registerProperty(new Property<>("general_settings.portable_data", PropertyType.STRING_LIST, new ArrayList<>()));
        if (HologramProvider.class.isAssignableFrom(instanceClass)) {
            registerProperty(new Property<>("info_hologram.offset", PropertyType.VECTOR3F, new Vector3f(0f, 0f, 0f)));
            registerProperty(new Property<>("info_hologram.translation", PropertyType.VECTOR3F, new Vector3f(1.2f, 0f, 0f)));
        }
        this.instanceClass = instanceClass;
        this.configResource = new ResourceLocation(addon.externalContainer, "structures/" + id + ".yml");
        this.configGenerator = StandardConfigGenerator.INSTANCE;

        ByteArrayBuilder builder = new ByteArrayBuilder();
        createBounds(builder);

        collisionVectors = builder.build();
        if (collisionVectors.length == 0) {
            collisionVectors = new byte[] { 0, 0, 0 };
        } else if (collisionVectors.length % 3 != 0) {
            throw new RuntimeException("Invalid structure bounds!");
        }

        byte[] min = { 127, 127, 127 };
        byte[] max = { -128, -128, -128 };

        for (int i = 0; i < collisionVectors.length; i+=3) {
            min[0] = (byte) Math.min(collisionVectors[i], min[0]);
            min[1] = (byte) Math.min(collisionVectors[i+1], min[1]);
            min[2] = (byte) Math.min(collisionVectors[i+2], min[2]);

            max[0] = (byte) Math.max(collisionVectors[i], max[0]);
            max[1] = (byte) Math.max(collisionVectors[i+1], max[1]);
            max[2] = (byte) Math.max(collisionVectors[i+2], max[2]);
        }

        size = new byte[] { (byte) (max[0] - min[0] + 1), (byte) (max[1] - min[1] + 1), (byte) (max[2] - min[2] + 1) };
    }

    public String getName() {
        String name = getProperty("general_settings.name", PropertyType.STRING);
        return name.charAt(0) == '@' ? translate(name.substring(1)) : name;
    }

    public List<String> getPortableData() {
        return getProperty("general_settings.portable_data", PropertyType.STRING_LIST);
    }

    public void setPortableData(List<String> keys) {
        setProperty("general_settings.portable_data", PropertyType.STRING_LIST, keys);
    }

    public void setPortableData(String... keys) {
        setPortableData(Arrays.asList(keys));
    }

    public void setOffset(@NotNull Vector3i offset) {
        setProperty("general_settings.placement_offset", PropertyType.VECTOR3I, offset);
    }

    public Vector3i getOffset() {
        return getProperty("general_settings.placement_offset", PropertyType.VECTOR3I);
    }

    public Vector3f getHologramOffset() {
        return getProperty("info_hologram.offset", PropertyType.VECTOR3F);
    }

    public void setHologramOffset(@NotNull Vector3f offset) {
        setProperty("info_hologram.offset", PropertyType.VECTOR3F, offset);
    }

    public Vector3f getHologramTranslation() {
        return getProperty("info_hologram.translation", PropertyType.VECTOR3F);
    }

    public void setHologramTranslation(@NotNull Vector3f offset) {
        setProperty("info_hologram.translation", PropertyType.VECTOR3F, offset);
    }

    public boolean hasFlag(int flags) {
        return MathUtils.hasFlag(this.flags, flags);
    }

    public void setFlags(int flags) {
        this.flags = MathUtils.setFlag(this.flags, flags, true);
    }

    public void toggleFlag(int flag, boolean toggle) {
        flags = MathUtils.setFlag(flags, flag, toggle);
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String name, PropertyType<T> type) {
        Property<Object> value = (Property<Object>) properties.get(name);
        return value != null && value.type == type ? (T) value.value : null;
    }

    public <T> void setProperty(String name, PropertyType<T> type, T value) {
        Property<Object> property = (Property<Object>) properties.get(name);
        if (property == null || property.type != type)
            return;

        property.value = value;
    }

    public Collection<Property<?>> getProperties() {
        return properties.values();
    }

    public void registerProperty(Property<?> property) {
        properties.put(property.name, property);
    }

    public void loadPropertyValues(PropertyLoader loader) {
        for (Property<?> prop : properties.values()) {
            loader.load(prop);
        }
    }

    final void initDefaultProperties() {
        registerProperty(new Property<>("general_settings.heavy", PropertyType.BOOLEAN, hasFlag(StructureFlag.HEAVY)));
    }

    public void reloadConfig() {
        if (configResource != null) {
            // Generate default config if it does not exists
            if (configResource.container.type == 1 && configGenerator != null) {
                try {
                    if (!configResource.getResource().file.exists()) {
                        configGenerator.generate(this).save(configResource.getResource().file);
                    }
                } catch (Exception e) {
                    TorusLogger.error(Category.STRUCTURES, "Could not generate configuration file for structure '" + id + "'.");
                    e.printStackTrace();
                }
            }

            // Load default config if it exists
            if (configResource.exists()) {
                try {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(configResource.getResource().stream));
                    loadPropertyValues(new PropertyLoader(config));
                } catch (Exception | Error e) {
                    TorusLogger.error(Category.STRUCTURES, "Invalid configuration for structure '" + id + "'.");
                    e.printStackTrace();
                }
            }

            toggleFlag(StructureFlag.HEAVY, getProperty("general_settings.heavy", PropertyType.BOOLEAN));
        }
    }

    protected void registerState(State<?> state) {
        this.allowedStates.put(state.key, state);
    }

    protected void createBounds(ByteArrayBuilder builder) {
    }

    public boolean isPlaceableAt(BlockLocation location, Direction direction) {
        byte[] offset = calculateOffset(direction.getOpposite());
        byte[] bounds = MathUtils.rotateVectors(this.collisionVectors, direction);

        location = location.getRelative(offset[0], offset[1], offset[2]);
        for (int i = 0; i < bounds.length; i += 3) {
            BlockLocation relative = location.getRelative(bounds[i], bounds[i + 1], bounds[i + 2]);
            if (relative.getBlock().getType().isSolid() || relative.getStructure() != null) {
                return false;
            }
        }
        return true;
    }

    public StructureInstance place(BlockLocation location, Direction direction, Pitch pitch) {
        if (numericId < 0) {
            throw new RuntimeException("Structure " + id + " is not registered and therefore cannot be placed.");
        }

        byte[] offset = calculateOffset(direction.getOpposite());
        location = location.getRelative(offset[0], offset[1], offset[2]);
        StructureInstance instance = instantiate(location, direction, pitch);
        place(instance, true);

        location.world.placeStructure(instance);
        return instance;
    }

    public static void place(StructureInstance instance, boolean handleStatus) {
        try {
            instance.setup();
            if (instance instanceof HologramProvider hologramProvider) {
                instance.inspectableDataContainer = hologramProvider.setupInspectableData();
            }
        } catch (Exception exc) {
            instance.isCorrupted = true;
            instance.inspectableDataContainer = new InspectableDataContainer((byte) 0);
            exc.printStackTrace();
        }

        if (handleStatus) {
            if (instance.location.getChunk().status == Status.PHYSICAL) {
                instance.makePhysical();
            } else if (instance.location.getChunk().status == Status.VIRTUAL) {
                instance.makeVirtual();
            }
        }
    }

    private byte[] calculateOffset(Direction direction) {
        Vector3i offset = getOffset();
        return MathUtils.rotateVectors(new byte[] { (byte) offset.x, (byte) offset.y, (byte) offset.z }, direction);
    }

    @Override
    public String toString() {
        return namespacedId;
    }

    protected abstract StructureInstance instantiate(@NotNull BlockLocation location, Direction direction, Pitch pitch);

}
