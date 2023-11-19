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
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class WorkforceBlockListener implements Listener {
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
        List<WorkforceUserProfession> relevantProfessions = WorkforceService.GetRelevantActiveUserProfessions(
                player.getUniqueId().toString(),
                new WorkforceAttributeType[] {
                        WorkforceAttributeType.BONUS_BLOCK_DROPS
                },
                event.getBlockState().getBlockData().getMaterial().name()
        );

        if (relevantProfessions.size() > 0) {
            // If the attribute targets the broken block, apply the modifier and reward the player.
            for (final WorkforceUserProfession profession : relevantProfessions) {
                final double basePayment = EconomyService.CalculateMonetaryReward(profession.getProfession().paymentEquation, profession.getLevel());
                final int baseExperience = (int) EvaluationService.evaluate(
                        EvaluationService.populateEquation(
                                WorkforceService.earnedExperienceEquation,
                                new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                        )
                );
                double paymentModifier = 0.0, experienceModifier = 0.0;

                // Iterate through all relevant attributes, apply their modifiers once each. Payment and experience addition should only run ONCE per profession.
                Random generator = new Random();
                final List<WorkforceAttribute> attributes = profession.getProfession().getRelevantAttributes(
                        new WorkforceAttributeType[]{ WorkforceAttributeType.BONUS_BLOCK_DROPS },
                        profession.getLevel(),
                        event.getBlockState().getBlockData().getMaterial().name()
                );
                for (final WorkforceAttribute attribute : attributes) {
                    paymentModifier = attribute.paymentModifier > paymentModifier ? attribute.paymentModifier : paymentModifier;
                    experienceModifier = attribute.experienceModifier > experienceModifier ? attribute.experienceModifier : experienceModifier;

                    final double activationChance = EvaluationService.evaluate(
                            EvaluationService.populateEquation(
                                    attribute.getEquation("chance"),
                                    new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                            )
                    );
                    final double activationRoll = generator.nextDouble();

                    if (activationRoll <= activationChance) {
                        final int increaseAmount = (int) Math.ceil(EvaluationService.evaluate(
                                EvaluationService.populateEquation(
                                        attribute.getEquation("amount"),
                                        new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                                )
                        ));

                        final Collection<ItemStack> naturalItemDrops = event.getBlockState().getDrops(player.getInventory().getItemInMainHand(), player);
                        for (Item dropItem : event.getItems()) {
                            if (naturalItemDrops.stream().anyMatch((naturalItem) -> naturalItem.getType() == dropItem.getItemStack().getType())) {
                                dropItem.getItemStack().setAmount(dropItem.getItemStack().getAmount() + increaseAmount);
                            }
                        }
                    }
                }

                if (profession.getProfession().isPaymentEnabled()) {
                    // Pay the player, if economy integration is enabled, as well as payment is enabled for the profession.
                    EconomyService.RewardPlayer(player.getUniqueId().toString(), basePayment * paymentModifier, profession.getProfession().name);
                }

                WorkforceService.RewardPlayer(profession, (int) Math.ceil(baseExperience * experienceModifier));
            }
        }
    }
}
