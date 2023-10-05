package io.github.cjustinn.specialisedworkforce2.listeners.attributes;

import io.github.cjustinn.specialisedworkforce2.enums.WorkforceAttributeType;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceAttribute;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceUserProfession;
import io.github.cjustinn.specialisedworkforce2.services.EconomyService;
import io.github.cjustinn.specialisedworkforce2.services.EvaluationService;
import io.github.cjustinn.specialisedworkforce2.services.WorkforceService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class WorkforceInventoryListener implements Listener {
    @EventHandler
    public void onItemCrafted(CraftItemEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (player != null) {
            String playerUuid = player.getUniqueId().toString();
            String craftedItem = event.getRecipe().getResult().getType().name();
            List<WorkforceUserProfession> relevantProfessions = WorkforceService.GetRelevantActiveUserProfessions(
                    playerUuid,
                    new WorkforceAttributeType[]{ WorkforceAttributeType.CRAFTING_EXPERIENCE },
                    craftedItem
            );

            if (relevantProfessions.size() > 0) {
                int totalCrafted = this.calculateTotalItemsCrafted(
                        event.getRecipe().getResult(),
                        event.getInventory().getMatrix(),
                        event.isShiftClick(),
                        player.getInventory()
                );

                if (totalCrafted > 0) {
                    for (final WorkforceUserProfession profession : relevantProfessions) {
                        final double basePayment = EconomyService.CalculateMonetaryReward(profession.getProfession().paymentEquation, profession.getLevel());
                        final int baseExperience = (int) Math.ceil(EvaluationService.evaluate(
                                        EvaluationService.populateEquation(
                                                WorkforceService.earnedExperienceEquation,
                                                new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                                        )
                                )
                        );

                        double paymentModifier = 0.0, experienceModifier = 0.0;

                        Random generator = new Random();
                        List<WorkforceAttribute> attributes = profession.getProfession().getRelevantAttributes(
                                new WorkforceAttributeType[]{ WorkforceAttributeType.CRAFTING_EXPERIENCE },
                                profession.getLevel(),
                                craftedItem
                        );

                        for (WorkforceAttribute attribute : attributes) {
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
                                final int amount = (int) Math.ceil(
                                        EvaluationService.evaluate(
                                                EvaluationService.populateEquation(
                                                        attribute.getEquation("amount"),
                                                        new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                                                )
                                        )
                                );

                                player.giveExp(amount);
                            }
                        }

                        profession.addExperience((int) Math.ceil((baseExperience * experienceModifier) * totalCrafted));

                        if (profession.getProfession().isPaymentEnabled()) {
                            EconomyService.ModifyFunds(player, (basePayment * paymentModifier) * totalCrafted);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onItemSmithingComplete(SmithItemEvent event) {
        Player player = (Player) event.getWhoClicked();
        final String playerId = player.getUniqueId().toString();
        final String templateUsed = event.getInventory().getInputTemplate().getType().name();

        List<WorkforceUserProfession> relevantProfessions = WorkforceService.GetRelevantActiveUserProfessions(
                playerId,
                new WorkforceAttributeType[]{ WorkforceAttributeType.SMITHING_EXPERIENCE },
                templateUsed
        );

        if (relevantProfessions.size() > 0) {
            for (final WorkforceUserProfession profession : relevantProfessions) {
                final double basePayment = EconomyService.CalculateMonetaryReward(profession.getProfession().paymentEquation, profession.getLevel());
                final int baseExperience = (int) Math.ceil(
                        EvaluationService.evaluate(
                                EvaluationService.populateEquation(
                                        WorkforceService.earnedExperienceEquation,
                                        new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                                )
                        )
                );

                double paymentModifier = 0.0, experienceModifier = 0.0;

                Random generator = new Random();
                List<WorkforceAttribute> attributes = profession.getProfession().getRelevantAttributes(
                        new WorkforceAttributeType[]{ WorkforceAttributeType.SMITHING_EXPERIENCE },
                        profession.getLevel(),
                        templateUsed
                );

                for (final WorkforceAttribute attribute : attributes) {
                    paymentModifier = Math.max(attribute.paymentModifier, paymentModifier);
                    experienceModifier = Math.max(attribute.experienceModifier, experienceModifier);

                    final double activationChance = EvaluationService.evaluate(
                            EvaluationService.populateEquation(
                                    attribute.getEquation("chance"),
                                    new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                            )
                    );

                    final double activationRoll = generator.nextDouble();
                    if (activationRoll <= activationChance) {
                        final int amount = (int) Math.ceil(EvaluationService.evaluate(
                                EvaluationService.populateEquation(attribute.getEquation("amount"), new HashMap<String, Object>() {{ put("level", profession.getLevel()); }})
                        ));

                        player.giveExp(amount);
                    }
                }

                profession.addExperience((int) Math.ceil(baseExperience * experienceModifier));
                if (profession.getProfession().isPaymentEnabled()) {
                    EconomyService.ModifyFunds(player, basePayment * paymentModifier);
                }
            }
        }
    }

    private int calculateTotalItemsCrafted(final ItemStack result, final ItemStack[] craftingGridContents, final boolean shiftClickCraft, final PlayerInventory playerInventory) {
        final int itemsPerCraft = result.getAmount();
        int crafted = itemsPerCraft;

        if (shiftClickCraft) {
            // Calculate the maximum number of crafting operations the current grid contents can handle.
            int maxIngredients = 64;
            for (final ItemStack ingredient : craftingGridContents) {
                if (ingredient != null) {
                    maxIngredients = Math.min(maxIngredients, ingredient.getAmount());
                }
            }

            // Calculate the maximum number of crafting operations the player's inventory will allow.
            int emptySlots = 0;
            int matchingItemSlotSpaces = 0;

            // Slot 36, 37, 38, 39, and 40 are the armor + offhand slots. Crafting operations don't deposit into any of those slots, so they can be disregarded.
            for (int i = 0; i < 36; i++) {
                final ItemStack currentSlotContent = playerInventory.getItem(i);

                if (currentSlotContent == null) {
                    emptySlots++;
                } else if (currentSlotContent != null && currentSlotContent.getType().equals(result.getType()) && currentSlotContent.getAmount() < currentSlotContent.getMaxStackSize()) {
                    matchingItemSlotSpaces += currentSlotContent.getMaxStackSize() - currentSlotContent.getAmount();
                }
            }

            // Determine the number crafted using the above details.
            crafted = Math.min((maxIngredients * itemsPerCraft), ((emptySlots * result.getMaxStackSize()) + matchingItemSlotSpaces));
        }

        return crafted;
    }
}
