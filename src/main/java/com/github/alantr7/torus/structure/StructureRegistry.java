package com.github.alantr7.torus.structure;

import com.github.alantr7.bukkitplugin.annotations.core.Invoke;
import com.github.alantr7.bukkitplugin.annotations.core.InvokePeriodically;
import com.github.alantr7.bukkitplugin.annotations.core.Singleton;
import com.github.alantr7.bytils.buffer.ByteArrayReader;
import com.github.alantr7.bytils.buffer.ByteArrayWriter;
import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.api.addon.ConfigType;
import com.github.alantr7.torus.api.resource.ResourceLocation;
import com.github.alantr7.torus.log.Category;
import com.github.alantr7.torus.log.TorusLogger;
import com.github.alantr7.torus.model.controller.ModelController;
import com.github.alantr7.torus.structure.registry.RegisteredStructure;
import com.github.alantr7.torus.updater.UpdateUtils_0_6_1;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

@Singleton
public class StructureRegistry {

    private static final byte FILE_VERSION = (byte) 2;

    private int nextStructureId = 2;

    private final Map<String, Structure> loaded = new HashMap<>();

    private final Map<Integer, Structure> loadedByNumericIds = new LinkedHashMap<>();

    private final Map<String, RegisteredStructure> registry = new HashMap<>();

    private final Map<String, String> changedIds = Map.of(
      "torus:item_cable", "torus:item_conduit"
    );

    private boolean isModified;

    @Invoke(Invoke.Schedule.BEFORE_PLUGIN_ENABLE)
    private void init() {
        load();
    }

    private void load() {
        File legacy = new File(TorusPlugin.getInstance().getDataFolder(), "id_map.dat");
        if (legacy.exists()) {
            loadPre0_7_0();
            isModified = true;

            return;
        }

        File structures = new File(TorusPlugin.getInstance().getDataFolder(), "structures.dat");
        if (!structures.exists())
            return;

        File file = new File(TorusPlugin.getInstance().getDataFolder(), "structures.dat");
        try {
            ByteArrayReader reader = new ByteArrayReader(Files.readAllBytes(file.toPath()));
            int fileVersion = reader.readU1();

            while (reader.hasNext()) {
                String namespacedId = reader.readShortString();
                int numericId = reader.readU2();
                int version = reader.readU1();

                Map<Integer, byte[]> collisionVersions = new HashMap<>();

                int versionsCount = reader.readU1();
                for (int i = 0; i < versionsCount; i++) {
                    int collisionsVersion = reader.readU1();
                    byte[] components = reader.readBytes(reader.readU2());
                    collisionVersions.put(collisionsVersion, components);
                }

                RegisteredStructure registeredStructure = new RegisteredStructure(namespacedId, numericId, version, collisionVersions);
                registry.put(changedIds.getOrDefault(namespacedId, namespacedId), registeredStructure);
                if (numericId >= nextStructureId) {
                    nextStructureId = numericId + 1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPre0_7_0() {
        File file = new File(TorusPlugin.getInstance().getDataFolder(), "id_map.dat");
        try {
            ByteArrayReader reader = new ByteArrayReader(Files.readAllBytes(file.toPath()));
            while (reader.hasNext()) {
                int numericId = reader.readU2();
                String id = reader.readShortString();

                RegisteredStructure registeredStructure = new RegisteredStructure(id, numericId, 0, new HashMap<>());
                registry.put(changedIds.getOrDefault(id, id), registeredStructure);
                if (numericId >= nextStructureId) {
                    nextStructureId = numericId + 1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @InvokePeriodically(delay = 100, interval = 20 * 60)
    @Invoke(Invoke.Schedule.AFTER_PLUGIN_DISABLE)
    public void save() {
        if (!isModified)
            return;

        File file = new File(TorusPlugin.getInstance().getDataFolder(), "structures.dat");
        file.getParentFile().mkdirs();

        try {
            ByteArrayWriter writer = new ByteArrayWriter();
            writer.writeU1(FILE_VERSION);

            for (RegisteredStructure str : registry.values()) {
                writer.writeShortString(str.namespacedId);
                writer.writeU2(str.numericId);
                writer.writeU1(str.version);
                writer.writeU1(str.collisionsVersions.size());

                for (int version : str.collisionsVersions.keySet()) {
                    writer.writeU1((byte) version);
                    byte[] vectors = str.collisionsVersions.get(version);
                    writer.writeU2(vectors.length);
                    writer.writeBytes(vectors);
                }
            }

            Files.write(file.toPath(), writer.getBytes());
            new File(TorusPlugin.getInstance().getDataFolder(), "id_map.dat").delete();
        } catch (Exception e) {
            e.printStackTrace();
        }

        isModified = false;
    }

    public void registerAndInitialize(Structure structure) {
        if (structure.numericId != -1) {
            throw new RuntimeException("Structure must not have numeric id already assigned when registering it");
        }

        RegisteredStructure registeredStructure;
        if (registry.containsKey(structure.namespacedId)) {
            registeredStructure = registry.get(structure.namespacedId);
            structure.numericId = registeredStructure.numericId;
            registry.get(structure.namespacedId);
        } else {
            structure.numericId = nextStructureId++;
            registeredStructure = new RegisteredStructure(structure.namespacedId, structure.numericId, 0, new HashMap<>());
            registry.put(structure.namespacedId, registeredStructure);
            isModified = true;
        }

        // Update registered structure if something changed
        if (structure.version != registeredStructure.version) {
            registeredStructure.version = structure.version;
            registeredStructure.collisionsVersions.put(registeredStructure.version, structure.collisionVectors);
            isModified = true;
        }

        structure.initDefaultProperties();

        // Update external config to 0.6.1 format
        if (structure.addon.allowsExternalConfig(ConfigType.STRUCTURE)) {
            File configFile = new File(structure.addon.configsDirectory, structure.id + ".yml");
            File pre_0_6_1_config = new File(structure.addon.configsDirectory, structure.id + ".config.yml");
            if (pre_0_6_1_config.exists()) {
                UpdateUtils_0_6_1.updateStructureConfig(pre_0_6_1_config, configFile);
            }

            structure.reloadConfig();
        }

        // Load model controller
        try {
            ResourceLocation controllerLocation = new ResourceLocation(
              structure.addon.externalContainer, "models/" + structure.id + ".yml",
              structure.addon.classpathContainer, "configs/torus/models/" + structure.id + ".yml"
            );
            if (!controllerLocation.exists()) {
                throw new Exception("Model controller file does not exist for structure '" + structure + "'!");
            }
            ModelController modelController = TorusPlugin.getInstance().getModelLoader().loadController(
              structure,
              YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(controllerLocation.getResource()).stream))
            );
            if (modelController != null) {
                structure.setModelController(modelController);
            }
        } catch (Exception exc) {
            TorusLogger.error(Category.MODELS, "Invalid model controller for structure '" + structure.id + "'");
            exc.printStackTrace();
        }

        structure.addon.registerContent(structure);
        loaded.put(structure.namespacedId, structure);
        loadedByNumericIds.put(structure.numericId, structure);
    }

    public Structure getStructure(String id) {
        return loaded.get(id);
    }

    public Structure getStructure(int id) {
        return loadedByNumericIds.get(id);
    }

    public String getStructureIdByNumericId(int id) {
        for (var entry : registry.entrySet()) {
            if (entry.getValue().numericId == id)
                return entry.getKey();
        }
        return null;
    }

    public Set<String> getStructuresIds() {
        return registry.keySet();
    }

    public Set<Map.Entry<String, RegisteredStructure>> getMap() {
        return registry.entrySet();
    }

    public Collection<Structure> getStructures() {
        return loaded.values();
    }

}
