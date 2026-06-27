package com.github.alantr7.torus.addon;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.api.addon.LifecycleAdapter;
import com.github.alantr7.torus.api.addon.TorusAddon;
import com.github.alantr7.torus.config.MainConfig;
import com.github.alantr7.torus.item.*;
import com.github.alantr7.torus.log.TorusLogger;
import com.github.alantr7.torus.recipe.TorusRecipeManager;
import com.github.alantr7.torus.structure.Structure;
import com.github.alantr7.torus.structure.StructureFlag;
import com.github.alantr7.torus.structure.StructureRegistry;
import com.github.alantr7.torus.structure.Structures;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public class DefaultAddonLifecycleAdapter extends LifecycleAdapter {

    public DefaultAddonLifecycleAdapter(TorusAddon addon) {
        super(addon);
    }

    @Override
    public void registerStructures(StructureRegistry registry) {
        registry.registerAndInitialize(Structures.BLAST_FURNACE);
        registry.registerAndInitialize(Structures.BLOCK_BREAKER);
        registry.registerAndInitialize(Structures.PUMP);
        registry.registerAndInitialize(Structures.PRESSURE_VENT);
        registry.registerAndInitialize(Structures.ORE_CRUSHER);
        registry.registerAndInitialize(Structures.ORE_WASHER);
        registry.registerAndInitialize(Structures.QUARRY);

        registry.registerAndInitialize(Structures.ENERGY_CABLE);
        registry.registerAndInitialize(Structures.ITEM_CABLE);
        registry.registerAndInitialize(Structures.FLUID_CABLE);
        registry.registerAndInitialize(Structures.DATA_CABLE);

        registry.registerAndInitialize(Structures.POWER_POLE);
        registry.registerAndInitialize(Structures.CONNECTOR);
        registry.registerAndInitialize(Structures.WIRE_CONNECTOR);
        registry.registerAndInitialize(Structures.WIRE_RELAY);
        registry.registerAndInitialize(Structures.JUNCTION_BOX);
        registry.registerAndInitialize(Structures.ELECTRICITY_METER);

        registry.registerAndInitialize(Structures.POWER_BANK);
        registry.registerAndInitialize(Structures.FLUID_TANK);

        registry.registerAndInitialize(Structures.TURRET);
        registry.registerAndInitialize(Structures.ELEVATOR);
        registry.registerAndInitialize(Structures.ELEVATOR_INTERFACE);

        registry.registerAndInitialize(Structures.FAUCET);
        registry.registerAndInitialize(Structures.LIGHT_BULB);

        registry.registerAndInitialize(Structures.GENERATOR);
        registry.registerAndInitialize(Structures.SOLAR_GENERATOR);
        registry.registerAndInitialize(Structures.WINDMILL);
        registry.registerAndInitialize(Structures.STEAM_TURBINE);

        for (String structureId : MainConfig.EXPERIMENTAL_VIRTUALIZATION_STRUCTURES) {
            Structure structure = registry.getStructure(structureId);
            if (structure != null) {
                structure.setFlags(StructureFlag.VIRTUALIZABLE);
            }
        }
    }

    @Override
    public void registerItems(ItemRegistry registry) {
        // Structure Items
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "generator", new Category[]{Category.GENERATORS}, Structures.GENERATOR, new HeadData("http://textures.minecraft.net/texture/8cbca012f67e54de9aee72ff424e056c2ae58de5eacc949ab2bcd9683cec"), "Generator", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "solar_generator", new Category[]{Category.GENERATORS}, Structures.SOLAR_GENERATOR, Material.PAPER, "Solar Generator", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "windmill", new Category[]{Category.GENERATORS}, Structures.WINDMILL, Material.PAPER, "Windmill", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "steam_turbine", new Category[]{Category.GENERATORS}, Structures.STEAM_TURBINE, Material.PAPER, "Steam Turbine", Collections.emptyList()));

        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "turret", new Category[]{Category.MACHINES}, Structures.TURRET, Material.PAPER, "Turret", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "elevator", new Category[]{Category.MACHINES}, Structures.ELEVATOR, Material.PAPER, "Elevator", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "elevator_interface", new Category[]{Category.MACHINES}, Structures.ELEVATOR_INTERFACE, Material.PAPER, "Elevator Interface", Collections.emptyList()));
        // Machines
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "turret", new Category[]{Category.MACHINES}, Structures.TURRET, Material.PAPER, "Turret", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "blast_furnace", new Category[]{Category.MACHINES}, Structures.BLAST_FURNACE, Material.PAPER, "Blast Furnace", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "block_breaker", new Category[]{Category.MACHINES}, Structures.BLOCK_BREAKER, Material.PAPER, "Block Breaker", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "pressure_vent", new Category[]{Category.MACHINES}, Structures.PRESSURE_VENT, Material.PAPER, "Pressure Relief Vent", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "pump", new Category[]{Category.MACHINES}, Structures.PUMP, Material.PAPER, "Pump", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "ore_crusher", new Category[]{Category.MACHINES}, Structures.ORE_CRUSHER, Material.PAPER, "Ore Crusher", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "ore_washer", new Category[]{Category.MACHINES}, Structures.ORE_WASHER, Material.PAPER, "Ore Washer", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "quarry", new Category[]{Category.MACHINES}, Structures.QUARRY, Material.PAPER, "Quarry", Collections.emptyList()));

        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "energy_cable", new Category[]{Category.NETWORK}, Structures.ENERGY_CABLE, Material.PAPER, "Energy Cable", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "fluid_pipe", new Category[]{Category.NETWORK}, Structures.FLUID_CABLE, Material.PAPER, "Fluid Pipe", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "item_conduit", new Category[]{Category.NETWORK}, Structures.ITEM_CABLE, Material.PAPER, "Item Conduit", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "data_cable", new Category[]{Category.NETWORK}, Structures.DATA_CABLE, Material.PAPER, "Data Cable", Collections.emptyList()));

        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "power_pole", new Category[]{Category.NETWORK}, Structures.POWER_POLE, Material.SPRUCE_FENCE, "Power Pole", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "connector", new Category[]{Category.NETWORK}, Structures.CONNECTOR, Material.HEAVY_CORE, "Connector", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "wire_connector", new Category[]{Category.NETWORK}, Structures.WIRE_CONNECTOR, Material.HEAVY_CORE, "Wire Connector", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "wire_relay", new Category[]{Category.NETWORK}, Structures.WIRE_RELAY, Material.HEAVY_CORE, "Wire Relay", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "junction_box", new Category[]{Category.NETWORK}, Structures.JUNCTION_BOX, Material.OBSERVER, "Junction Box", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "electricity_meter", new Category[]{Category.NETWORK}, Structures.ELECTRICITY_METER, Material.OBSERVER, "Electricity Meter", Collections.emptyList()));

        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "screwdriver", new Category[]{Category.TOOLS}, null, Material.STICK, "Screwdriver", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "hammer", new Category[]{Category.TOOLS}, null, Material.STICK, "Hammer", Collections.emptyList()));

        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "faucet", new Category[]{Category.TOOLS}, Structures.FAUCET, Material.TRIPWIRE_HOOK, "Faucet", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "light_bulb", new Category[]{Category.TOOLS}, Structures.LIGHT_BULB, Material.OCHRE_FROGLIGHT, "Light Bulb", Collections.emptyList()));

        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "power_bank", new Category[]{Category.STORAGE}, Structures.POWER_BANK, Material.PAPER, "Power Bank", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "fluid_tank", new Category[]{Category.STORAGE}, Structures.FLUID_TANK, Material.PAPER, "Fluid Tank", Collections.emptyList()));

        // Regular Items
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "iron_dust", new Category[]{Category.RESOURCES}, null, Material.DEAD_TUBE_CORAL_FAN, "Iron Dust", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "copper_dust", new Category[]{Category.RESOURCES}, null, Material.GLOWSTONE_DUST, "Copper Dust", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "gold_dust", new Category[]{Category.RESOURCES}, null, Material.HORN_CORAL_FAN, "Gold Dust", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "steel_ingot", new Category[]{Category.RESOURCES}, null, Material.IRON_INGOT, "Steel Ingot", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "steel_nugget", new Category[]{Category.RESOURCES}, null, Material.IRON_NUGGET, "Steel Nugget", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "machine_block", new Category[]{Category.COMPONENTS}, null, new HeadData("http://textures.minecraft.net/texture/944bdc56def9a99e0f775d0c3879a69138d1510a18bbacb0c82902bbf816171c"), "Machine Block", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "copper_wire", new Category[]{Category.COMPONENTS}, null, Material.PITCHER_POD, "Copper Wire", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "copper_coil", new Category[]{Category.COMPONENTS}, null, new HeadData("http://textures.minecraft.net/texture/b3652284921a2dba440060f2f63aa3ba2b0df62c9c36bf7883acedc336df911b"), "Copper Coil", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "transistor", new Category[]{Category.COMPONENTS}, null, Material.COMPARATOR, "Transistor", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "circuit_board", new Category[]{Category.COMPONENTS}, null, Material.PAPER, "Circuit Board", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "motor", new Category[]{Category.COMPONENTS}, null, new HeadData("http://textures.minecraft.net/texture/8cbca012f67e54de9aee72ff424e056c2ae58de5eacc949ab2bcd9683cec"), "Motor", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "shaft", new Category[]{Category.COMPONENTS}, null, Material.END_ROD, "Shaft", Collections.emptyList()));
        registry.registerItem(new TorusItem(TorusPlugin.DEFAULT_ADDON, "windmill_blade", new Category[]{Category.COMPONENTS}, null, Material.WHITE_BANNER, "Windmill Blade", Collections.emptyList()));

        Category.RESOURCES.display = registry.getItemById("torus:copper_dust").toItemStack();
        Category.GENERATORS.display = new ItemStack(Material.FURNACE);
        Category.MACHINES.display = registry.getItemById("torus:motor").toItemStack();
        Category.COMPONENTS.display = registry.getItemById("torus:transistor").toItemStack();
        Category.TOOLS.display = registry.getItemById("torus:screwdriver").toItemStack();
    }

    @Override
    public void registerRecipes(TorusRecipeManager registry) {
        // Save default recipes on the first run
        if (!addon.recipesDirectory.exists()) {
            addon.recipesDirectory.mkdirs();
            String[] fileNames = { "blasting", "crafting", "crusher", "smelting", "washer" };
            for (String fileName : fileNames) {
                File targetFile = new File(addon.recipesDirectory, fileName + ".yml");
                InputStream resource = addon.plugin.getResource("configs/torus/recipes/" + fileName + ".yml");
                if (resource != null) {
                    try (resource) {
                        Files.copy(resource, targetFile.toPath());
                    } catch (Exception ex) {
                        TorusLogger.error(com.github.alantr7.torus.log.Category.RECIPES, "Could not save recipes file '" + fileName + "'.");
                        ex.printStackTrace();
                    }
                }
            }
        }

        super.registerRecipes(registry);
    }

}