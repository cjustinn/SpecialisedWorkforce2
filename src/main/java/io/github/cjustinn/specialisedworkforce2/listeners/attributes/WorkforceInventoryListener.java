package io.github.cjustinn.specialisedworkforce2.listeners.attributes;

import io.github.cjustinn.specialisedworkforce2.enums.AttributeLogInteractionMode;
import io.github.cjustinn.specialisedworkforce2.enums.AttributeLogType;
import io.github.cjustinn.specialisedworkforce2.enums.WorkforceAttributeType;
import io.github.cjustinn.specialisedworkforce2.models.PotionDuration;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceAttribute;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceUserProfession;
import io.github.cjustinn.specialisedworkforce2.services.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.StonecutterInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nullable;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class WorkforceInventoryListener implements Listener {
    @EventHandler
    public void onItemCrafted(CraftItemEvent event) {
        Player player = (Player) event.getWhoClicked();
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
                        paymentModifier = Math.max(paymentModifier, attribute.paymentModifier);
                        experienceModifier = Math.max(experienceModifier, attribute.experienceModifier);

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

                            WorkforceService.RewardPlayerMinecraftExperience(playerUuid, amount, profession.getProfession().name);
                        }
                    }

                    WorkforceService.RewardPlayer(profession, (int) Math.ceil((baseExperience * experienceModifier) * totalCrafted));

                    if (profession.getProfession().isPaymentEnabled()) {
                        EconomyService.RewardPlayer(playerUuid, (basePayment * paymentModifier) * totalCrafted, profession.getProfession().name);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onItemForged(InventoryClickEvent event) {
        final InventoryAction[] validActions = new InventoryAction[] {
                InventoryAction.PICKUP_ALL,
                InventoryAction.PICKUP_HALF,
                InventoryAction.MOVE_TO_OTHER_INVENTORY
        };

        Player player = (Player) event.getWhoClicked();
        if (player != null && event.getSlotType() == InventoryType.SlotType.RESULT && Arrays.stream(validActions).anyMatch((action) -> action == event.getAction())) {
            final String playerUuid = player.getUniqueId().toString();

            List<WorkforceUserProfession> relevantProfessions = WorkforceService.GetRelevantActiveUserProfessions(
                    playerUuid,
                    new WorkforceAttributeType[]{ WorkforceAttributeType.ANVIL_FORGE_EXPERIENCE },
                    event.getCurrentItem().getType().name()
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
                            new WorkforceAttributeType[]{ WorkforceAttributeType.ANVIL_FORGE_EXPERIENCE },
                            profession.getLevel(),
                            event.getCurrentItem().getType().name()
                    );

                    for (WorkforceAttribute attribute : attributes) {
                        paymentModifier = Math.max(paymentModifier, attribute.paymentModifier);
                        experienceModifier = Math.max(experienceModifier, attribute.experienceModifier);

                        final double activationChance = EvaluationService.evaluate(
                                EvaluationService.populateEquation(
                                        attribute.getEquation("chance"),
                                        new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                                )
                        );
                        final double activationRoll = generator.nextDouble();

                        if (activationRoll <= activationChance) {
                            final int amount = (int) Math.ceil(EvaluationService.evaluate(
                                    EvaluationService.populateEquation(
                                            attribute.getEquation("amount"),
                                            new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                                    )
                            ));

                            WorkforceService.RewardPlayerMinecraftExperience(playerUuid, amount, profession.getProfession().name);
                        }
                    }

                    WorkforceService.RewardPlayer(profession, (int) Math.ceil(baseExperience * experienceModifier));
                    if (profession.getProfession().isPaymentEnabled()) {
                        EconomyService.RewardPlayer(playerUuid, basePayment * paymentModifier, profession.getProfession().name);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onStonecuttingCompleted(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String playerUuid = player.getUniqueId().toString();

        if (player != null && event.getInventory() instanceof StonecutterInventory && event.getCurrentItem().getType() != Material.AIR) {
            // Current Item: Crafted item (count not included), Input Item: resource cut
            List<WorkforceUserProfession> relevantProfessions = WorkforceService.GetRelevantActiveUserProfessions(
                    playerUuid,
                    new WorkforceAttributeType[]{ WorkforceAttributeType.STONECUTTER_EXPERIENCE },
                    event.getCurrentItem().getType().name()
            );

            if (relevantProfessions.size() > 0) {
                final int cutAmount = calculateTotalItemsCrafted(
                        event.getCurrentItem(),
                        new ItemStack[]{ ((StonecutterInventory) event.getInventory()).getInputItem() },
                        event.isShiftClick(),
                        player.getInventory()
                );

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
                            new WorkforceAttributeType[]{ WorkforceAttributeType.STONECUTTER_EXPERIENCE },
                            profession.getLevel(),
                            event.getCurrentItem().getType().name()
                    );

                    for (WorkforceAttribute attribute : attributes) {
                        paymentModifier = Math.max(paymentModifier, attribute.paymentModifier);
                        experienceModifier = Math.max(experienceModifier, attribute.experienceModifier);

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

                            WorkforceService.RewardPlayerMinecraftExperience(playerUuid, amount * cutAmount, profession.getProfession().name);
                        }
                    }

                    WorkforceService.RewardPlayer(profession, (int) Math.ceil((baseExperience * experienceModifier) * cutAmount));
                    if (profession.getProfession().isPaymentEnabled()) {
                        EconomyService.RewardPlayer(playerUuid, (basePayment * paymentModifier) * cutAmount, profession.getProfession().name);
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

                        WorkforceService.RewardPlayerMinecraftExperience(playerId, amount, profession.getProfession().name);
                    }
                }

                WorkforceService.RewardPlayer(profession, (int) Math.ceil(baseExperience * experienceModifier));

                if (profession.getProfession().isPaymentEnabled()) {
                    EconomyService.RewardPlayer(playerId, basePayment * paymentModifier, profession.getProfession().name);
                }
            }
        }
    }

    @EventHandler
    public void onItemSmelted(FurnaceSmeltEvent event) {
        if (AttributeLoggingService.LogExists(event.getBlock().getLocation(), AttributeLogType.FURNACE)) {
            String smelterUuid = AttributeLoggingService.logs.get(event.getBlock().getLocation()).uuid;
            List<WorkforceUserProfession> relevantProfessions = WorkforceService.GetRelevantActiveUserProfessions(
                    smelterUuid,
                    new WorkforceAttributeType[] {
                            WorkforceAttributeType.SMELTING_EXPERIENCE
                    },
                    event.getResult().getType().name()
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
                            new WorkforceAttributeType[]{ WorkforceAttributeType.SMELTING_EXPERIENCE },
                            profession.getLevel(),
                            event.getResult().getType().name()
                    );

                    for (final WorkforceAttribute attribute : attributes) {
                        paymentModifier = Math.max(paymentModifier, attribute.paymentModifier);
                        experienceModifier = Math.max(experienceModifier, attribute.experienceModifier);

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

                            WorkforceService.RewardPlayerMinecraftExperience(smelterUuid, amount, profession.getProfession().name);
                        }
                    }

                    if (profession.getProfession().isPaymentEnabled()) {
                        EconomyService.RewardPlayer(smelterUuid, basePayment * paymentModifier, profession.getProfession().name);
                    }

                    WorkforceService.RewardPlayer(profession, (int) Math.ceil(baseExperience * experienceModifier));
                }
            }

            if (event.getSource().getAmount() == 1) {
                AttributeLoggingService.LogInteraction(AttributeLogInteractionMode.REMOVE, event.getBlock().getLocation());
            }
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        Player player = (Player) event.getViewers().get(0);
        if (player != null && event.getInventory().getResult() != null) {
            final String playerUuid = player.getUniqueId().toString();

            List<WorkforceUserProfession> relevantProfessions = WorkforceService.GetRelevantActiveUserProfessions(
                    playerUuid,
                    new WorkforceAttributeType[]{ WorkforceAttributeType.REDUCE_ANVIL_COST },
                    event.getInventory().getResult().getType().name()
            );

            if (relevantProfessions.size() > 0) {
                double costsModifier = 0.0;

                for (final WorkforceUserProfession profession : relevantProfessions) {
                    List<WorkforceAttribute> attributes = profession.getProfession().getRelevantAttributes(
                            new WorkforceAttributeType[]{ WorkforceAttributeType.REDUCE_ANVIL_COST },
                            profession.getLevel(),
                            event.getInventory().getResult().getType().name()
                    );

                    for (WorkforceAttribute attribute : attributes) {
                        costsModifier = Math.max(costsModifier, EvaluationService.evaluate(
                                EvaluationService.populateEquation(attribute.getEquation("amount"), new HashMap<String, Object>() {{ put("level", profession.getLevel()); }})
                        ));
                    }
                }

                final int repairExperienceCost = event.getInventory().getRepairCost();
                final int repairResourceCost = event.getInventory().getRepairCostAmount();

                event.getInventory().setRepairCost(Math.max(1, (int) Math.floor(repairExperienceCost * (1.0 - costsModifier))));
                event.getInventory().setRepairCostAmount(Math.max(1, (int) Math.floor(repairResourceCost * (1.0 - costsModifier))));
            }
        }
    }

    @EventHandler
    public void onPotionBrewed(BrewEvent event) {
        // Get all brewed types.
        Map<String, Integer> brewedPotionTypes = new HashMap<String, Integer>();
        for (PotionEffectType type : event.getResults().stream().map((potion) -> {
            PotionMeta meta = (PotionMeta) potion.getItemMeta();
            return meta.getBasePotionData().getType().getEffectType();
        }).collect(Collectors.toList())) {
            if (!brewedPotionTypes.containsKey(type.getName())) {
                brewedPotionTypes.put(type.getName(), 1);
            } else {
                int amount = brewedPotionTypes.get(type.getName());
                brewedPotionTypes.put(type.getName(), amount + 1);
            }
        }

        @Nullable String brewerUuid =
                AttributeLoggingService.LogExists(event.getContents().getLocation(), AttributeLogType.BREWING_STAND)
                ? AttributeLoggingService.logs.get(event.getContents().getLocation()).uuid
                : null;

        if (brewerUuid != null) {
            // Get all professions which target each of the potions.
            List<WorkforceUserProfession> relevantProfessions = new ArrayList<WorkforceUserProfession>() {{
                for (String type : brewedPotionTypes.keySet()) {
                    addAll(WorkforceService.GetRelevantActiveUserProfessions(
                            brewerUuid,
                            new WorkforceAttributeType[] { WorkforceAttributeType.INCREASE_POTION_DURATION },
                            type
                    ));
                }
            }}.stream().distinct().collect(Collectors.toList());

            if (relevantProfessions.size() > 0) {
                for (final WorkforceUserProfession profession : relevantProfessions) {
                    List<WorkforceAttribute> attributes = new ArrayList<WorkforceAttribute>() {{
                        for (String type : brewedPotionTypes.keySet()) {
                            List<WorkforceAttribute> relevantAttributes = profession.getProfession().getRelevantAttributes(
                                    new WorkforceAttributeType[]{ WorkforceAttributeType.INCREASE_POTION_DURATION },
                                    profession.getLevel(),
                                    type
                            );

                            if (relevantAttributes.size() < 1) {
                                brewedPotionTypes.remove(type);
                            }

                            addAll(relevantAttributes);
                        }
                    }}.stream().distinct().collect(Collectors.toList());
                    for (WorkforceAttribute attribute : attributes) {
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
                        int affectedCount = 0;

                        paymentModifier = Math.max(paymentModifier, attribute.paymentModifier);
                        experienceModifier = Math.max(experienceModifier, attribute.experienceModifier);

                        final int amount = (int) Math.ceil(
                                EvaluationService.evaluate(
                                        EvaluationService.populateEquation(
                                                attribute.getEquation("amount"),
                                                new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                                        )
                                )
                        ) * 20;

                        for (String type : brewedPotionTypes.keySet()) {
                            if (attribute.targets(type)) {
                                affectedCount += brewedPotionTypes.get(type);

                                for (ItemStack potion : event.getResults()) {
                                    if (potion != null && potion.getType() != Material.AIR) {
                                        // Get the potion meta & it's base potion data.
                                        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
                                        PotionEffectType potionType = potionMeta.getBasePotionData().getType().getEffectType();

                                        if (potionType.getName().equals(type)) {
                                            final boolean isExtended = potionMeta.getBasePotionData().isExtended();
                                            final boolean isUpgraded = potionMeta.getBasePotionData().isUpgraded();

                                            PotionDuration relevantDuration = PotionDuration.standards.get(potionType);
                                            int duration = 0;

                                            if (isExtended) {
                                                duration = relevantDuration.getExtendedDuration() + amount;
                                            } else if (isUpgraded) {
                                                duration = relevantDuration.getUpgradedDuration() + amount;
                                            } else {
                                                duration = relevantDuration.getBaseDuration() + amount;
                                            }

                                            potionMeta.addCustomEffect(
                                                    new PotionEffect(potionType, duration, isUpgraded ? 1 : 0),
                                                    true
                                            );

                                            potionMeta.lore(
                                                    new ArrayList<TextComponent>() {{
                                                        add(
                                                                Component.text(String.format(
                                                                        "Brewed by %s %s",
                                                                        getPreceedingQualifier(profession.getProfession().name),
                                                                        profession.getProfession().name
                                                                ), NamedTextColor.GOLD)
                                                        );
                                                    }}
                                            );

                                            potion.setItemMeta(potionMeta);
                                        }
                                    }
                                }

                                if (profession.getProfession().isPaymentEnabled()) {
                                    EconomyService.RewardPlayer(brewerUuid, (basePayment * paymentModifier) * affectedCount, profession.getProfession().name);
                                }

                                WorkforceService.RewardPlayer(profession, (int) Math.ceil(baseExperience * experienceModifier) * affectedCount);
                            }
                        }


                    }
                }
            }
        }
    }

    private String getPreceedingQualifier(String word) {
        List<String> vowels = Arrays.asList(new String[] { "a", "e", "i", "o", "u" });
        return vowels.stream().anyMatch((character) -> word.toLowerCase().startsWith(character)) ? "an" : "a";
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
