package io.github.cjustinn.specialisedworkforce2.listeners.attributes;

import io.github.cjustinn.specialisedworkforce2.SpecialisedWorkforce2;
import io.github.cjustinn.specialisedworkforce2.enums.AttributeLogInteractionMode;
import io.github.cjustinn.specialisedworkforce2.enums.AttributeLogType;
import io.github.cjustinn.specialisedworkforce2.services.AttributeLoggingService;
import io.github.cjustinn.specialisedworkforce2.services.CoreProtectService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.metadata.FixedMetadataValue;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
    This listener should only hold EventHandlers responsible for logging some kind of interaction
    with an inventory, block, etc., which has a direct impact on a WorkforceAttribute.
*/
public class WorkforceLoggingListener implements Listener {
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();

        block.setMetadata("non-natural", new FixedMetadataValue(SpecialisedWorkforce2.plugin, true));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (AttributeLoggingService.LogExists(event.getBlock().getLocation())) {
            AttributeLoggingService.LogInteraction(AttributeLogInteractionMode.REMOVE, event.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        String direction = event.getDirection().name();
        List<Block> affectedBlocks = event.getBlocks();

        Map<Character, Integer> movement = new HashMap<Character, Integer>() {{
            put('x', direction.equals("EAST") ? 1 : (direction.equals("WEST") ? -1 : 0));
            put('y', direction.equals("UP") ? 1 : (direction.equals("DOWN") ? -1 : 0));
            put('z', direction.equals("SOUTH") ? 1 : (direction.equals("NORTH") ? -1 : 0));
        }};

        this.handlePistonEvent(affectedBlocks, movement);
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        String direction = event.getDirection().name();
        List<Block> affectedBlocks = event.getBlocks();

        Map<Character, Integer> movement = new HashMap<Character, Integer>() {{
            put('x', direction.equals("EAST") ? -1 : (direction.equals("WEST") ? 1 : 0));
            put('y', direction.equals("UP") ? -1 : (direction.equals("DOWN") ? 1 : 0));
            put('z', direction.equals("SOUTH") ? -1 : (direction.equals("NORTH") ? 1 : 0));
        }};

        this.handlePistonEvent(affectedBlocks, movement);
    }

    private void handlePistonEvent(List<Block> affectedBlocks, Map<Character, Integer> movement) {
        // Appropriately mark all previous positions as removed (if applicable).
        for (Block currentBlock : affectedBlocks) {
            final boolean isNatural = CoreProtectService.IsBlockNatural(currentBlock);
            if (!isNatural) {
                CoreProtectService.LogBlockInteraction("remove", null, currentBlock.getLocation(), currentBlock.getType(), currentBlock.getBlockData());
                currentBlock.removeMetadata("non-natural", SpecialisedWorkforce2.plugin);
            }
        }

        // Appropriately mark all new positions as placed (if applicable).
        for (Block currentBlock : affectedBlocks) {
            final boolean isNatural = CoreProtectService.IsBlockNatural(currentBlock);
            if (!isNatural) {
                Block updatedBlock = currentBlock.getWorld().getBlockAt(
                        currentBlock.getLocation().getBlockX() + movement.get('x'),
                        currentBlock.getLocation().getBlockY() + movement.get('y'),
                        currentBlock.getLocation().getBlockZ() + movement.get('z')
                );

                CoreProtectService.LogBlockInteraction("place", null, updatedBlock.getLocation(), updatedBlock.getType(), updatedBlock.getBlockData());
                updatedBlock.setMetadata("non-natural", new FixedMetadataValue(SpecialisedWorkforce2.plugin, true));
            }
        }
    }

    @EventHandler
    public void onAddToInventory(InventoryClickEvent event) {
        InventoryAction action = event.getAction();
        InventoryType.SlotType slotType = event.getSlotType();

        if (event.getInventory() instanceof FurnaceInventory) {
            InventoryAction[] cursorActions = new InventoryAction[] {
                    InventoryAction.PICKUP_ALL,
                    InventoryAction.PLACE_ALL,
                    InventoryAction.PLACE_SOME,
                    InventoryAction.PLACE_ONE,
                    InventoryAction.SWAP_WITH_CURSOR,
                    InventoryAction.MOVE_TO_OTHER_INVENTORY
            };

            FurnaceInventory furnace = (FurnaceInventory) event.getInventory();
            final ItemStack target = ((Arrays.stream(cursorActions).anyMatch((cursorAction) -> cursorAction == action) || (action == InventoryAction.PICKUP_HALF && event.getCurrentItem().getAmount() == 1)) && slotType == InventoryType.SlotType.CRAFTING) ? event.getCursor() : event.getCurrentItem();

            final boolean shouldLog = this.shouldAddFurnaceLog(furnace, target, action, slotType);
            final boolean shouldRemoveLog = this.shouldRemoveFurnaceLog(furnace, target, action, slotType);

            if (shouldLog) {
                this.logBlockInteraction(AttributeLogType.FURNACE, AttributeLogInteractionMode.CREATE, furnace.getLocation(), ((Player) event.getWhoClicked()).getUniqueId().toString());
            } else if (shouldRemoveLog) {
                this.logBlockInteraction(AttributeLogInteractionMode.REMOVE, furnace.getLocation());
            }
        } else if (event.getInventory() instanceof BrewerInventory){
            InventoryAction[] cursorActions = new InventoryAction[] {
                    InventoryAction.PICKUP_ALL,
                    InventoryAction.PICKUP_HALF,
                    InventoryAction.PLACE_ALL,
                    InventoryAction.PLACE_SOME,
                    InventoryAction.PLACE_ONE,
                    InventoryAction.SWAP_WITH_CURSOR
            };

            BrewerInventory inventory = (BrewerInventory) event.getInventory();
            final ItemStack target = Arrays.stream(cursorActions).anyMatch((cursorAction) -> cursorAction == action) ? event.getCursor() : event.getCurrentItem();

            final boolean shouldLog = this.shouldAddBrewingLog(inventory, target, action, slotType);
            final boolean shouldRemoveLog = this.shouldRemoveBrewingLog(inventory, target, action, slotType);

            if (shouldLog) {
                AttributeLoggingService.LogInteraction(
                        AttributeLogType.BREWING_STAND,
                        AttributeLogInteractionMode.CREATE,
                        inventory.getLocation(),
                        event.getWhoClicked().getUniqueId().toString()
                );
            } else if (shouldRemoveLog) {
                AttributeLoggingService.LogInteraction(
                        AttributeLogInteractionMode.REMOVE,
                        inventory.getLocation()
                );
            }
        }
    }

    private boolean shouldRemoveBrewingLog(BrewerInventory inventory, ItemStack targetItem, InventoryAction action, InventoryType.SlotType slot) {
        boolean remove = false;

        if (
            action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && slot == InventoryType.SlotType.FUEL
                && this.availableSpacesForType(inventory.getViewers().get(0).getInventory(), targetItem.getType()) >= targetItem.getAmount()
                && (inventory.getIngredient() != null && inventory.getIngredient().getAmount() == targetItem.getAmount())
        ) {
            remove = true;
        } else if (
                (action == InventoryAction.PICKUP_ALL || action == InventoryAction.PICKUP_HALF)
                && slot == InventoryType.SlotType.FUEL
                && (targetItem == null || targetItem.getType() == Material.AIR)
        ) {
            remove = true;
        }

        return remove;
    }

    private int availableSpacesForType(PlayerInventory inventory, Material item) {
        int available = 0;

        for (int i = 0; i < 36; i++) {
            @Nullable ItemStack slotItem = inventory.getItem(i);

            if (slotItem == null)
                available += item.getMaxStackSize();
            else if (
                    slotItem != null
                    && slotItem.getType() == item
                    && (slotItem.getAmount() < slotItem.getMaxStackSize())
            )
                available += (item.getMaxStackSize() - slotItem.getAmount());
        }

        return available;
    }

    private boolean shouldAddBrewingLog(BrewerInventory inventory, ItemStack targetItem, InventoryAction action, InventoryType.SlotType slot) {
        boolean valid = false;
        Material[] validIngredients = new Material[] {
                Material.NETHER_WART,
                Material.REDSTONE,
                Material.GLOWSTONE_DUST,
                Material.FERMENTED_SPIDER_EYE,
                Material.GUNPOWDER,
                Material.DRAGON_BREATH,
                Material.SUGAR,
                Material.RABBIT_FOOT,
                Material.GLISTERING_MELON_SLICE,
                Material.SPIDER_EYE,
                Material.PUFFERFISH,
                Material.MAGMA_CREAM,
                Material.GOLDEN_CARROT,
                Material.BLAZE_POWDER,
                Material.GHAST_TEAR,
                Material.TURTLE_HELMET,
                Material.PHANTOM_MEMBRANE
        };

        if (
                (action.equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)
                        && (slot == InventoryType.SlotType.CONTAINER || slot == InventoryType.SlotType.QUICKBAR)
                        && Arrays.stream(validIngredients).anyMatch((validItem) -> validItem == targetItem.getType())
                        && (inventory.getIngredient() == null || (inventory.getIngredient() != null && inventory.getIngredient().getType() == targetItem.getType() && inventory.getIngredient().getAmount() < inventory.getIngredient().getMaxStackSize())))
        ) {
            valid = true;
        } else if (
                (action.equals(InventoryAction.PLACE_ALL) || action.equals(InventoryAction.PLACE_ONE) || action.equals(InventoryAction.PLACE_SOME) || action.equals(InventoryAction.SWAP_WITH_CURSOR))
                        && slot == InventoryType.SlotType.FUEL
                        && Arrays.stream(validIngredients).anyMatch((validItem) -> validItem == targetItem.getType())
        ) {
            valid = true;
        }

        return valid;
    }

    private boolean shouldRemoveFurnaceLog(FurnaceInventory inventory, ItemStack targetItem, InventoryAction action, InventoryType.SlotType slot) {
        boolean valid = false;

        if ((inventory.getSmelting() == null || !inventory.canSmelt(
                action == InventoryAction.MOVE_TO_OTHER_INVENTORY || action == InventoryAction.PICKUP_HALF || action == InventoryAction.PICKUP_ALL || action == InventoryAction.SWAP_WITH_CURSOR ? targetItem : inventory.getSmelting()
        )) && slot == InventoryType.SlotType.CRAFTING) {
            valid = true;
        }

        return valid;
    }

    private boolean shouldAddFurnaceLog(FurnaceInventory inventory, ItemStack targetItem, InventoryAction action, InventoryType.SlotType slot) {
        boolean valid = false;

        if (
                (action.equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)
                        && (slot.equals(InventoryType.SlotType.CONTAINER) || slot.equals(InventoryType.SlotType.QUICKBAR))
                        && inventory.canSmelt(targetItem)
                        && (inventory.getSmelting() == null || (inventory.getSmelting() != null && inventory.getSmelting().getType() == targetItem.getType() && inventory.getSmelting().getAmount() < inventory.getSmelting().getMaxStackSize())))
        ) {
            valid = true;
        } else if (
                (action.equals(InventoryAction.PLACE_ALL) || action.equals(InventoryAction.PLACE_ONE) || action.equals(InventoryAction.PLACE_SOME) || action.equals(InventoryAction.SWAP_WITH_CURSOR))
                        && (slot.equals(InventoryType.SlotType.CRAFTING))
                        && inventory.canSmelt(targetItem)
        ) {
            valid = true;
        }

        return valid;
    }

    private void logBlockInteraction(AttributeLogInteractionMode mode, Location location) {
        AttributeLoggingService.LogInteraction(mode, location);
    }

    private void logBlockInteraction(AttributeLogType type, AttributeLogInteractionMode mode, Location location, String uuid) {
        AttributeLoggingService.LogInteraction(type, mode, location, uuid);
    }
}
