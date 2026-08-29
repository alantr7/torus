package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.gui.structure.BlastFurnaceGUI;
import com.github.alantr7.torus.item.ItemReference;
import com.github.alantr7.torus.structure.inspection.InspectableDataContainer;
import com.github.alantr7.torus.structure.socket.ItemSocket;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.recipe.BlastFurnaceRecipe;
import com.github.alantr7.torus.structure.HologramProvider;
import com.github.alantr7.torus.structure.LoadContext;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.Structures;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.inventory.BukkitStructureInventory;
import com.github.alantr7.torus.world.BlockLocation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedList;
import java.util.List;

import static com.github.alantr7.torus.lang.Localization.translate;

public class BlastFurnaceInstance extends StructureInstance implements HologramProvider {

    protected ItemSocket inputSocket;

    protected ItemSocket itemOutputSocket, slugOutputSocket;

    public Inventory inventory = Bukkit.createInventory(null, 27, "Blast Furnace");

    public boolean recipeLookup;

    public BlastFurnaceRecipe recipe;

    public boolean isActive;

    public boolean autoSmelt;

    private int processedTicks;

    BlastFurnaceInstance(LoadContext context) {
        super(context);
    }

    public BlastFurnaceInstance(BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        super(Structures.BLAST_FURNACE, location, bodyDef, direction);
    }

    @Override
    public void tick(boolean isVirtual) {
        itemOutputSocket.attemptDirectItemExport();

        if (recipe != null) {
            if (isActive && processedTicks++ >= recipe.smeltDuration) {
                ItemStack resultSlot = inventory.getItem(16);
                if (resultSlot != null && ItemReference.compare(resultSlot, recipe.result.item)) {
                    resultSlot.setAmount(resultSlot.getAmount() + recipe.result.asResult().getAmount());
                } else {
                    inventory.setItem(16, recipe.result.asResult());
                }
                this.recipe = null;
                this.processedTicks = 0;
                this.isActive = false;
                this.recipeLookup = true;
            } else {
                updateRecipeDisplay();
            }
        }

        if (recipeLookup) {
            this.recipe = null;
            for (BlastFurnaceRecipe recipe : TorusPlugin.getInstance().getRecipeRegistry().getBlastFurnaceRecipes()) {
                if (canUseRecipe(recipe)) {
                    this.recipe = recipe;
                    break;
                }
            }
            updateRecipeDisplay();
            recipeLookup = false;
        }
    }

    private void updateRecipeDisplay() {
        if (recipe != null) {
            ItemStack recipeItem = recipe.result.asResult();
            ItemMeta meta = recipeItem.getItemMeta();
            List<String> lore = new LinkedList<>();

            if (isActive) {
                meta.setDisplayName(translate("gui.blast_furnace.smelting"));

                float ratio = (float) processedTicks / recipe.smeltDuration;
                int progress = (int) Math.floor(ratio * 10);
                lore.addAll(List.of(
                  translate("gui.blast_furnace.progress")
                    .replace("{bar}", "■".repeat(progress) + "□".repeat(10 - progress))
                    .replace("{percent}", String.format("%.0f", ratio * 100)),
                  translate("gui.blast_furnace.time_remaining")
                    .replace("{time}", String.valueOf(recipe.smeltDuration - processedTicks)),
                  ""
                ));
            } else {
                lore.add(translate("gui.blast_furnace.duration").replace("{duration}", String.valueOf(recipe.smeltDuration)));
                lore.add("");
                lore.add(translate("gui.blast_furnace.click_smelt"));
            }
            lore.add(translate("gui.blast_furnace.auto_smelt." + autoSmelt));

            meta.setLore(lore);

            recipeItem.setItemMeta(meta);
            inventory.setItem(14, recipeItem);
        } else {
            ItemStack recipeNotFound = new ItemStack(Material.BARRIER);
            ItemMeta meta = recipeNotFound.getItemMeta();
            meta.setDisplayName(translate("gui.blast_furnace.no_recipe"));
            recipeNotFound.setItemMeta(meta);
            inventory.setItem(14, recipeNotFound);
        }
    }

    public void smelt() {
        if (isActive || !canUseRecipe(recipe))
            return;

        // Consume items and produce result
        outer: for (ItemReference ingredient : recipe.ingredients) {
            int consumedAmount = 0;
            for (int i = 10; i < 13; i++) {
                ItemStack stack = inventory.getContents()[i];
                if (stack == null)
                    continue;

                if (ItemReference.create(stack).itemId.equals(ingredient.itemId)) {
                    int taken = Math.min(stack.getAmount(), ingredient.getItem().getAmount() - consumedAmount);
                    consumedAmount += taken;

                    stack.setAmount(stack.getAmount() - taken);
                    if (stack.getAmount() == 0) {
                        inventory.setItem(i, null);
                    }

                    if (consumedAmount == ingredient.getItem().getAmount())
                        continue outer;
                }
            }
        }

        isActive = true;
        updateRecipeDisplay();
    }

    public boolean canUseRecipe(BlastFurnaceRecipe recipe) {
        if (recipe == null)
            return false;

        outer: for (ItemReference ingredient : recipe.ingredients) {
            for (int i = 10; i < 13; i++) {
                ItemStack stack = inventory.getContents()[i];
                if (stack == null)
                    continue;

                if (ItemReference.create(stack).itemId.equals(ingredient.itemId)) {
                    continue outer;
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean onPlayerInteract(PlayerInteractEvent event, BlockLocation location) {
        new BlastFurnaceGUI(this, event.getPlayer()).open();
        return true;
    }

    @Override
    protected void setup() throws SetupException {
        inputSocket = requireSocket("in_item", ItemSocket.class);
        itemOutputSocket = requireSocket("out_item", ItemSocket.class);
        slugOutputSocket = requireSocket("out_slug", ItemSocket.class);

        itemOutputSocket.linkedInventory = new BukkitStructureInventory(inventory);
        itemOutputSocket.linkedInventoryAllowedSlots = new int[]{16};
        updateRecipeDisplay();
    }

    @Override
    public InspectableDataContainer setupInspectableData() {
        return new InspectableDataContainer((byte) 1)
          .property("Recipe", () -> recipe != null ? recipe.key.toString() : "(None)");
    }

}
