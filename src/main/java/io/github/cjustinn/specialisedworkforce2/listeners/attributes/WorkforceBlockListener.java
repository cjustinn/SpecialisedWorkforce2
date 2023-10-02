package io.github.cjustinn.specialisedworkforce2.listeners.attributes;

import io.github.cjustinn.specialisedworkforce2.SpecialisedWorkforce2;
import io.github.cjustinn.specialisedworkforce2.enums.WorkforceAttributeType;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceAttribute;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceUserProfession;
import io.github.cjustinn.specialisedworkforce2.services.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class WorkforceBlockListener implements Listener {
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();

        block.setMetadata("non-natural", new FixedMetadataValue(SpecialisedWorkforce2.plugin, true));
    }

    @EventHandler
    public void onBlockDropItem(BlockDropItemEvent event) {
        // If the broken block is a non-fully-grown crop, return immediately - we don't care.
        // Also check to see if the broken block was a natural or player-placed block, eventually.
        final BlockData blockData = event.getBlockState().getBlockData();
        if (
                (blockData instanceof Ageable && ((Ageable) blockData).getAge() < ((Ageable) blockData).getMaximumAge())
                || (!(blockData instanceof Ageable) && !(blockData instanceof Sapling) && !CoreProtectService.IsBlockNatural(event.getBlockState().getBlock()))
        ) {
            return;
        }

        // Get the player who broke the block.
        Player player = event.getPlayer();

        // Check if the player has any attributes with the BONUS_BLOCK_DROPS attribute.
        // If the player has the attribute, check if any of their targets include the block broken.
        List<WorkforceUserProfession> relevantProfessions =
                WorkforceService.GetActiveUserProfessionsWithAttribute(player.getUniqueId().toString(), WorkforceAttributeType.BONUS_BLOCK_DROPS)
                        .stream().filter(
                                (userProfession) -> userProfession.getProfession().getAttributesByType(WorkforceAttributeType.BONUS_BLOCK_DROPS, userProfession.getLevel()).stream().anyMatch(
                                        (attribute) -> attribute.targets(event.getBlockState().getBlockData().getMaterial().name())
                                )
                        ).collect(Collectors.toList());

        if (relevantProfessions.size() > 0) {
            // If the attribute targets the broken block, apply the modifier and reward the player.
            for (final WorkforceUserProfession profession : relevantProfessions) {
                if (profession.getProfession().isPaymentEnabled()) {
                    // Pay the player, if economy integration is enabled, as well as payment is enabled for the profession.
                    EconomyService.ModifyFunds(
                            player,
                            EconomyService.CalculateMonetaryReward(profession.getProfession().paymentEquation, profession.getLevel())
                    );
                }

                // Add job experience to the relevant user profession.
                final int experienceToAdd = (int) EvaluationService.evaluate(
                        WorkforceService.earnedExperienceEquation.replace("{level}", String.valueOf(profession.getLevel()))
                );

                profession.addExperience(experienceToAdd);

                // Iterate through all relevant attributes, apply their modifiers once each. Payment and experience addition should only run ONCE per profession.
                Random generator = new Random();
                for (final WorkforceAttribute attribute : profession.getProfession().getAttributesByType(WorkforceAttributeType.BONUS_BLOCK_DROPS, profession.getLevel())) {
                    final double activationChance = EvaluationService.evaluate(attribute.getEquation("chance").replace("{level}", String.valueOf(profession.getLevel())));
                    final double activationRoll = generator.nextDouble();

                    if (activationRoll <= activationChance) {
                        final int increaseAmount = (int) Math.ceil(EvaluationService.evaluate(
                                attribute.getEquation("amount").replace("{level}", String.valueOf(profession.getLevel()))
                        ));

                        for (Item dropItem : event.getItems()) {
                            dropItem.getItemStack().setAmount(dropItem.getItemStack().getAmount() + increaseAmount);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        String direction = event.getDirection().name();
        List<Block> affectedBlocks = event.getBlocks();

        Map<Character, Integer> movement = new HashMap<Character, Integer>() {{
            put('x', direction == "EAST" ? 1 : (direction == "WEST" ? -1 : 0));
            put('y', direction == "UP" ? 1 : (direction == "DOWN" ? -1 : 0));
            put('z', direction == "SOUTH" ? 1 : (direction == "NORTH" ? -1 : 0));
        }};

        this.handlePistonEvent(affectedBlocks, movement);
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        String direction = event.getDirection().name();
        List<Block> affectedBlocks = event.getBlocks();

        Map<Character, Integer> movement = new HashMap<Character, Integer>() {{
            put('x', direction == "EAST" ? -1 : (direction == "WEST" ? 1 : 0));
            put('y', direction == "UP" ? -1 : (direction == "DOWN" ? 1 : 0));
            put('z', direction == "SOUTH" ? -1 : (direction == "NORTH" ? 1 : 0));
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
}
