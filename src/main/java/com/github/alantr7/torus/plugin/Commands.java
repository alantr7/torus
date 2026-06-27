package com.github.alantr7.torus.plugin;

import com.github.alantr7.bukkitplugin.annotations.core.Singleton;
import com.github.alantr7.bukkitplugin.commands.annotations.CommandHandler;
import com.github.alantr7.bukkitplugin.commands.executor.Evaluator;
import com.github.alantr7.bukkitplugin.commands.executor.ExecutorType;
import com.github.alantr7.bukkitplugin.commands.factory.CommandBuilder;
import com.github.alantr7.bukkitplugin.commands.registry.Command;
import com.github.alantr7.bukkitplugin.gui.GUI;
import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.api.TorusAPI;
import com.github.alantr7.torus.api.addon.LifecycleAction;
import com.github.alantr7.torus.gui.browser.ItemBrowserMainGUI;
import com.github.alantr7.torus.item.TorusItem;
import com.github.alantr7.torus.machine.ElevatorInstance;
import com.github.alantr7.torus.model.PartModel;
import com.github.alantr7.torus.model.de_provider.DisplayEntitiesPartModel;
import com.github.alantr7.torus.model.de_provider.EntityReference;
import com.github.alantr7.torus.structure.Conductor;
import com.github.alantr7.torus.structure.Status;
import com.github.alantr7.torus.structure.Structure;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.socket.Socket;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.TorusChunk;
import com.github.alantr7.torus.world.TorusRegion;
import com.github.alantr7.torus.world.TorusWorld;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Keyed;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
public class Commands {

    @com.github.alantr7.bukkitplugin.annotations.generative.Command(name = "torus", description = "")
    public static final String TORUS = "torus";

    @CommandHandler Command give = CommandBuilder.using("torus")
      .parameter("give")
      .parameter("{target}", Evaluator.PLAYER, p -> p.tabComplete(args -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList()))
      .parameter("{item}", p -> p.tabComplete((args) -> {
          if (args.length > 2 && !"torus:".startsWith(args[2]) && !args[2].startsWith("torus:")) {
              return TorusPlugin.getInstance().getItemRegistry().getItemIds().stream().map(id -> id.substring("torus:".length())).toList();
          }
          return TorusPlugin.getInstance().getItemRegistry().getItemIds();
      }))
      .parameter("{amount}", Evaluator.INTEGER, p -> p.defaultValue(ctx -> 1))
      .permission(Permissions.COMMAND_GIVE)
      .requireMatches(3)
      .executes(ctx -> {
          Player target = (Player) ctx.getArgument("target");
          if (target == null) {
              ctx.respond(ChatColor.RED + "Specified player could not be found.");
              return;
          }

          TorusItem item = TorusPlugin.getInstance().getItemRegistry().getItemById((String) ctx.getArgument("item"));
          if (item == null) {
              ctx.respond(ChatColor.RED + "Specified item does not exist.");
              return;
          }

          if (ctx.optArgument("amount").isEmpty() || (int) ctx.getArgument("amount") < 1 || (int) ctx.getArgument("amount") > 150) {
              ctx.respond(ChatColor.RED + "Specified amount is not a valid number.");
              return;
          }

          ItemStack stack = item.toItemStack().clone();
          int amount = (int) ctx.getArgument("amount");
          stack.setAmount(amount);

          target.getInventory().addItem(stack);
          ctx.respond(ChatColor.YELLOW + "Gave " + amount + " x " + ChatColor.GOLD + item.name);
          target.sendMessage(ChatColor.YELLOW + "You received " + amount + " x " + ChatColor.GOLD + item.name);
      });

    @CommandHandler Command browse = CommandBuilder.using("torus")
      .parameter("browse")
      .forExecutors(ExecutorType.PLAYER).permission(Permissions.COMMAND_BROWSE)
      .executes(ctx -> {
          new ItemBrowserMainGUI((Player) ctx.getExecutor()).open();
      });

    @CommandHandler Command recipe = CommandBuilder.using("torus")
      .parameter("recipe")
      .parameter("{item}", p -> p.tabComplete((args) -> {
            if (args.length > 2 && !"torus:".startsWith(args[2]) && !args[2].startsWith("torus:")) {
                return TorusPlugin.getInstance().getItemRegistry().getItemIds().stream().map(id -> id.substring("torus:".length())).toList();
            }
            return TorusPlugin.getInstance().getItemRegistry().getItemIds();
        }))
      .forExecutors(ExecutorType.PLAYER).permission(Permissions.COMMAND_RECIPE)
      .executes(ctx -> {
          TorusItem item = TorusPlugin.getInstance().getItemRegistry().getItemById((String) ctx.getArgument("item"));
          if (item == null) {
              ctx.respond(ChatColor.RED + "Specified item does not exist.");
              return;
          }

          if (!item.hasRecipes()) {
              ctx.respond(ChatColor.RED + "This item does not have any recipes.");
              return;
          }

          Keyed recipe = item.getRecipes().iterator().next();
          GUI viewer = TorusPlugin.getInstance().getRecipeRegistry().createRecipeViewer((Player) ctx.getExecutor(), recipe);

          if (viewer == null) {
              ctx.respond(ChatColor.RED + "This recipe can not be previewed.");
              return;
          }

          viewer.open();
      });

    @CommandHandler Command reload = CommandBuilder.using("torus")
      .parameter("reload")
      .permission(Permissions.COMMAND_RELOAD)
      .executes(ctx -> {
          for (Structure structure : TorusPlugin.getInstance().getStructureRegistry().getStructures()) {
              structure.reloadConfig();
          }
          TorusPlugin.getInstance().getItemRegistry().clear();
          TorusPlugin.getInstance().getRecipeRegistry().clear();
          TorusAPI.getAddonLifecycle().run(LifecycleAction.LOAD_ITEMS);
          TorusAPI.getAddonLifecycle().run(LifecycleAction.LOAD_RECIPES);
          TorusPlugin.getInstance().getLocalization().reload();
          ctx.respond("Structure configs, items configs, recipes and locale reloaded.");
      });

    @CommandHandler Command exportPreset = CommandBuilder.using("torus")
      .parameter("export_preset")
      .parameter("{preset}", p -> p.tabComplete())
      .permission(Permissions.COMMAND_USE_PRESET)
      .executes(ctx -> {
          String preset = (String) ctx.getArgument("preset");
          if (preset == null) {
              ctx.respond("Invalid preset specified.");
              return;
          }

          ctx.respond("Invalid preset specified.");
      });

    @CommandHandler Command reportStats = CommandBuilder.using("torus")
      .parameter("debug")
      .parameter("report_stats")
      .permission(Permissions.COMMAND_DEBUG)
      .executes(ctx -> {
          Collection<TorusWorld> worlds = TorusPlugin.getInstance().getWorldManager().getWorlds();
          ctx.respond("There are currently " + worlds.size() + " worlds loaded.");
          for (TorusWorld world : worlds) {
              int regions = 0, chunks = 0, structures = 0, virtualChunks = 0, virtualStructures = 0;
              for (TorusRegion region : world.getRegions()) {
                  regions++;
                  for (TorusChunk chunk : region.getLoadedChunks()) {
                      chunks++;
                      if (chunk.status == Status.VIRTUAL)
                          virtualChunks++;

                      for (StructureInstance structure : chunk.getStructures()) {
                          structures++;
                          if (structure.getStatus() == Status.VIRTUAL)
                              virtualStructures++;
                      }
                  }
              }

              ctx.respond("- " + world.getBukkit().getName() + ":");
              ctx.respond("  --- Regions: %d".formatted(regions));
              ctx.respond("  --- Chunks: %d (%d virtual)".formatted(chunks, virtualChunks));
              ctx.respond("  --- Structures: %d (%d virtual)".formatted(structures, virtualStructures));
          }
          ctx.respond("---");
          ctx.respond("- Average tick duration: " + TorusPlugin.getInstance().getWorldManager().getTickDurationTimings().getAverage() + "ms");
      });

    @CommandHandler Command inspectStructure = CommandBuilder.using("torus")
      .parameter("debug")
      .parameter("inspect_structure")
      .permission(Permissions.COMMAND_DEBUG)
      .forExecutors(ExecutorType.PLAYER).permission(Permissions.COMMAND_DEBUG)
      .executes(ctx -> {
          Player player = (Player) ctx.getExecutor();
          Block block = player.getTargetBlockExact(5);
          if (block == null) {
              ctx.respond("Not looking at any structure.");
              return;
          }

          TorusWorld world = TorusPlugin.getInstance().getWorldManager().getWorld(player.getWorld());
          if (world == null) {
              ctx.respond("Structures aren't allowed in this world.");
              return;
          }

          StructureInstance structure = world.getStructure(new BlockLocation(block.getLocation()));
          if (structure == null) {
              ctx.respond("Not looking at any structure.");
              return;
          }

          ctx.respond("\n");
          ctx.respond(ChatColor.GOLD + "Structure ID: " + ChatColor.RESET + structure.structure.namespacedId);
          ctx.respond(ChatColor.GOLD + "Owner ID: " + ChatColor.RESET + structure.getOwnerId());
          ctx.respond(ChatColor.GOLD + "Status: " + ChatColor.RESET + structure.getStatus());
          ctx.respond(ChatColor.GOLD + "State:");
          for (var entry : structure.getState().getEntries()) {
              ctx.respond("  - " + entry.getKey() + ": " + entry.getValue());
          }
          ctx.respond(ChatColor.GOLD + "Sockets: (" + structure.getSockets().size() + ")");
          for (Socket socket : structure.getSockets()) {
              ctx.respond("  - " + ChatColor.YELLOW + socket.getComponent().absoluteLocation + ":");
              ctx.respond("    - Matter: " + ChatColor.GRAY + socket.medium);
              ctx.respond("    - Flow: " + ChatColor.GRAY + socket.getFlowDirection());
              ctx.respond("    - Connections: " + ChatColor.GRAY + socket.getConnections());
          }
      });

    @CommandHandler Command highlightNetwork = CommandBuilder.using("torus")
      .parameter("debug")
      .parameter("highlight_networks")
      .permission(Permissions.COMMAND_DEBUG)
      .executes(ctx -> {
          Player player = (Player) ctx.getExecutor();
          Block block = player.getTargetBlockExact(5);
          if (block == null) {
              ctx.respond("Not looking at any structure.");
              return;
          }

          TorusWorld world = TorusPlugin.getInstance().getWorldManager().getWorld(player.getWorld());
          if (world == null) {
              ctx.respond("Structures aren't allowed in this world.");
              return;
          }

          BlockLocation blockLocation = new BlockLocation(block.getLocation());
          StructureInstance structure = world.getStructure(blockLocation);
          if (structure == null) {
              ctx.respond("Not looking at any structure.");
              return;
          }

          for (Socket socket : structure.getSockets()) {
              player.sendMessage(socket.medium + ": " + socket.network.id + " (invalidated?: " + socket.network.isInvalidated() + ")");
              List<StructureInstance> all = new ArrayList<>();
              all.addAll(socket.network.edges);
              all.addAll(socket.network.nodes.stream().map(n -> {
                  player.sendMessage(" * node: " + n.socket.network.id + " (invalidated?: " + n.socket.network.isInvalidated() + ")");
                  return n.structure;
              }).toList());
              for (StructureInstance edge : all) {
                  for (PartModel partModel : edge.model.parts.values()) {
                      if (!(partModel instanceof DisplayEntitiesPartModel de))
                          continue;

                      for (EntityReference ref : de.entityReferences) {
                          Display display = ref.getEntity();
                          if (display != null) {
                              display.setGlowing(true);
                              if (edge instanceof Conductor conductor) {
                                  switch (conductor.getMedium()) {
                                      case ENERGY -> display.setGlowColorOverride(Color.LIME);
                                      case ITEM -> display.setGlowColorOverride(Color.YELLOW);
                                      case FLUID -> display.setGlowColorOverride(Color.AQUA);
                                  }
                              }
                              Bukkit.getScheduler().runTaskLater(TorusPlugin.getInstance(), () -> {
                                  display.setGlowing(false);
                              }, 20L * 15);
                          }
                      }
                  }
              }
          }
      });

    @CommandHandler Command inspectChunk = CommandBuilder.using("torus")
      .parameter("debug")
      .parameter("inspect_chunk")
      .permission(Permissions.COMMAND_DEBUG)
      .forExecutors(ExecutorType.PLAYER).permission(Permissions.COMMAND_DEBUG)
      .executes(ctx -> {
          Player player = (Player) ctx.getExecutor();
          TorusChunk chunk = TorusPlugin.getInstance().getWorldManager().getWorld(player.getWorld()).getChunk(
            new BlockLocation(player.getLocation())
          );

          if (chunk == null) {
              ctx.respond("This chunk does not have any structures.");
              return;
          }

          ctx.respond("Chunk position: " + chunk.position);
          ctx.respond("Chunk size: " + chunk.getSize() + " bytes");
          ctx.respond("Structures: " + chunk.getStructures().size());
          ctx.respond("Occupations: " + chunk.getOccupations().size());
      });

    @CommandHandler Command setElevatorTarget = CommandBuilder.using("torus")
      .parameter("debug")
      .parameter("set_elevator_target")
      .parameter("{y}", Integer.class, p -> p.evaluator(Evaluator.INTEGER))
      .permission(Permissions.COMMAND_DEBUG)
      .forExecutors(ExecutorType.PLAYER).permission(Permissions.COMMAND_DEBUG)
      .executes(ctx -> {
          Player player = (Player) ctx.getExecutor();
          ElevatorInstance.targetY = (int) ctx.getArgument("y");
      });

}
