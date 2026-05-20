package com.github.alantr7.torus;

import com.github.alantr7.bukkitplugin.BukkitPlugin;
import com.github.alantr7.bukkitplugin.annotations.generative.JavaPlugin;
import com.github.alantr7.bukkitplugin.annotations.generative.SoftDepends;
import com.github.alantr7.bukkitplugin.annotations.relocate.Relocate;
import com.github.alantr7.bukkitplugin.annotations.relocate.Relocations;
import com.github.alantr7.bukkitplugin.versions.Version;
import com.github.alantr7.torus.addon.DefaultAddonLifecycleAdapter;
import com.github.alantr7.torus.addon.TorusAddonManager;
import com.github.alantr7.torus.api.TorusAPI;
import com.github.alantr7.torus.api.addon.ConfigType;
import com.github.alantr7.torus.api.addon.TorusAddon;
import com.github.alantr7.torus.config.MainConfig;
import com.github.alantr7.torus.integration.worldguard.WorldGuardIntEntryPoint;
import com.github.alantr7.torus.item.Category;
import com.github.alantr7.torus.item.ItemReference;
import com.github.alantr7.torus.item.ItemRegistry;
import com.github.alantr7.torus.item.TorusItem;
import com.github.alantr7.torus.lang.Localization;
import com.github.alantr7.torus.log.TorusLogger;
import com.github.alantr7.torus.model.ModelLoader;
import com.github.alantr7.torus.model.ModelManager;
import com.github.alantr7.torus.player.TorusPlayerManager;
import com.github.alantr7.torus.recipe.TorusRecipeManager;
import com.github.alantr7.torus.structure.StructureRegistry;
import com.github.alantr7.torus.utils.Compatibility;
import com.github.alantr7.torus.world.TorusWorldManager;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.List;

@JavaPlugin(name = "Torus", version = "0.6.3", apiVersion = "1.21")
@Relocations(@Relocate(from = "com.github.alantr7.bukkitplugin", to = "com.github.alantr7.torus.bpf"))
@SoftDepends({"ProtocolLib", "ModelEngine", "WorldGuard", "GriefPrevention", "Lands", "Residence"})
public class TorusPlugin extends BukkitPlugin {

    @Getter
    static TorusPlugin instance;

    public static TorusAddon DEFAULT_ADDON;

    @Getter
    protected final TorusAddonManager addonManager;

    @Getter
    protected ItemRegistry itemRegistry;

    private Metrics metrics;

    private static boolean usesPaperAPI;

    public TorusPlugin() {
        instance = this;
        addonManager = new TorusAddonManager();

        checkPaperAPI();
    }

    @Override
    public void onLoad() {
        // WorldGuard integration
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            WorldGuardIntEntryPoint.initialize();
        }
    }

    @Override
    protected void onPluginEnable() {
        DEFAULT_ADDON = TorusAPI.newAddon(this, "torus")
          .name("Torus (Default)")
          .allowExternalConfigurations(ConfigType.STRUCTURE, ConfigType.MODEL, ConfigType.ITEMS, ConfigType.RECIPES)
          .register();

        TorusAPI.getAddonLifecycle().subscribe(DEFAULT_ADDON, new DefaultAddonLifecycleAdapter(DEFAULT_ADDON));
        itemRegistry = new ItemRegistry();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            addonManager.getLifecycle().start();
            savePresets();
        }, 1L);

        metrics = new Metrics(TorusPlugin.getInstance(), 28910);
        metrics.addCustomChart(new SimplePie("config_virtualization", () -> String.valueOf(MainConfig.EXPERIMENTAL_VIRTUALIZATION_ENABLED)));
    }

    @Override
    protected void onPluginDisable() {
    }

    public void savePresets() {
        File torusDirectory = DEFAULT_ADDON.rootDirectory;
        torusDirectory.mkdirs();

        File itemsDirectory = new File(torusDirectory, "items");
        if (!itemsDirectory.exists()) {
            itemsDirectory.mkdir();
            for (Category category : getItemRegistry().getCategories()) {
                if (!category.namespacedId.startsWith("torus:"))
                    continue;

                YamlConfiguration config = new YamlConfiguration();
                for (TorusItem item : category.items) {
                    if (item.addon != DEFAULT_ADDON)
                        continue;

                    ItemStack baseItem = item.getBaseItem();
                    config.set(item.id + ".base", ItemReference.create(baseItem).toString().toLowerCase());
                    config.set(item.id + ".name", item.name);
                    config.set(item.id + ".lore", baseItem.getItemMeta().getLore());
                    config.set(item.id + ".categories", List.of(category.namespacedId));
                    if (baseItem.getType() == Material.PLAYER_HEAD) {
                        config.set(item.id + ".head_texture_url", ((SkullMeta) baseItem.getItemMeta()).getPlayerProfile().getTextures().getSkin().toExternalForm());
                    }
                    if (baseItem.hasItemMeta()) {
                        ItemMeta meta = baseItem.getItemMeta();
                        if (Version.getServerVersion().isOlderThan(Compatibility.V1_21_4) && meta.hasCustomModelData()) {
                            config.set(item.id + ".custom_model_data", meta.getCustomModelData());
                        } else if (!Version.getServerVersion().isOlderThan(Compatibility.V1_21_4) && meta.hasCustomModelDataComponent() && !meta.getCustomModelDataComponent().getStrings().isEmpty()) {
                            config.set(item.id + ".custom_model_data", baseItem.getItemMeta().getCustomModelDataComponent().getStrings().get(0));
                        }
                    }
                }

                try {
                    config.save(new File(itemsDirectory, category.id + ".yml"));
                } catch (Exception e) {
                    TorusLogger.error(com.github.alantr7.torus.log.Category.GENERAL, "Could not save items config file: '" + category.name + ".yml'");
                    e.printStackTrace();
                }
            }
        }
    }

    public StructureRegistry getStructureRegistry() {
        return getSingleton(StructureRegistry.class);
    }

    public TorusRecipeManager getRecipeRegistry() {
        return getSingleton(TorusRecipeManager.class);
    }

    public TorusWorldManager getWorldManager() {
        return getSingleton(TorusWorldManager.class);
    }

    public TorusPlayerManager getPlayerManager() {
        return getSingleton(TorusPlayerManager.class);
    }

    public ModelLoader getModelLoader() {
        return getSingleton(ModelLoader.class);
    }

    public ModelManager getModelManager() {
        return getSingleton(ModelManager.class);
    }

    public Localization getLocalization() {
        return getSingleton(Localization.class);
    }

    public static boolean usesPaperAPI() {
        return usesPaperAPI;
    }

    private static void checkPaperAPI() {
        try {
            Class.forName("io.papermc.paper.entity.TeleportFlag");
            usesPaperAPI = true;

            instance.getLogger().info("Using Paper API");
        } catch (Exception | Error e) {
            instance.getLogger().info("Using Spigot API. Some features may not function properly.");
        }
    }

}
