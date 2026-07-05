package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.item.TorusItem;
import com.github.alantr7.torus.structure.LoadContext;
import com.github.alantr7.torus.structure.Structure;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.data.Data;
import com.github.alantr7.torus.utils.AnvilUtils;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import io.papermc.paper.math.Position;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class ElevatorDetectorInstance extends StructureInstance {

    protected Data<String> name = dataContainer.persist("name", Data.Type.STRING, "");

    protected String assignedName;

    ElevatorDetectorInstance(LoadContext context) {
        super(context);
    }

    public ElevatorDetectorInstance(Structure structure, BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        super(structure, location, bodyDef, direction);
    }

    @Override
    protected void setup() throws SetupException {
    }

    @Override
    public boolean onPlayerInteract(PlayerInteractEvent event, BlockLocation location) {
        if (!TorusItem.is(event.getItem(), "torus:screwdriver"))
            return false;

        Player player = event.getPlayer();
        AnvilUtils.requestRename(player, getAssignedName(), new AnvilUtils.RenameAdapter() {
            @Override
            public void onRename(String newName) {
                name.update(newName);
            }
        });
        return true;
    }

    public String getAssignedName() {
        return assignedName != null ? assignedName : name.get();
    }

}
