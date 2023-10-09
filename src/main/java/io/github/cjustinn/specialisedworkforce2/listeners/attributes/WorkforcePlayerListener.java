package io.github.cjustinn.specialisedworkforce2.listeners.attributes;

import io.github.cjustinn.specialisedworkforce2.enums.WorkforceAttributeType;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceAttribute;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceUserProfession;
import io.github.cjustinn.specialisedworkforce2.services.EconomyService;
import io.github.cjustinn.specialisedworkforce2.services.EvaluationService;
import io.github.cjustinn.specialisedworkforce2.services.WorkforceService;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class WorkforcePlayerListener implements Listener {
    @EventHandler
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        Material itemType = event.getItem().getType();

        List<WorkforceUserProfession> relevantProfessions = WorkforceService.GetRelevantActiveUserProfessions(
                player.getUniqueId().toString(),
                new WorkforceAttributeType[]{ WorkforceAttributeType.DURABILITY_SAVE },
                itemType.name()
        );

        if (relevantProfessions.size() > 0) {
            for (WorkforceUserProfession profession : relevantProfessions) {
                final int baseExperience = (int) EvaluationService.evaluate(
                        EvaluationService.populateEquation(
                                WorkforceService.earnedExperienceEquation,
                                new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                        )
                );
                double experienceModifier = 0.0;

                // Iterate through any relevant attributes, trigger them if they proc.
                Random generator = new Random();
                final List<WorkforceAttribute> attributes = profession.getProfession().getRelevantAttributes(
                        new WorkforceAttributeType[]{ WorkforceAttributeType.DURABILITY_SAVE },
                        profession.getLevel(),
                        itemType.name()
                );
                for (final WorkforceAttribute attribute : attributes) {
                    experienceModifier = attribute.experienceModifier > experienceModifier ? attribute.experienceModifier : experienceModifier;

                    final double activationChance = EvaluationService.evaluate(
                            EvaluationService.populateEquation(
                                    attribute.getEquation("chance"),
                                    new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                            )
                    );
                    final double activationRoll = generator.nextDouble();

                    if (activationRoll <= activationChance) {
                        event.setCancelled(true);
                    }
                }

                WorkforceService.RewardPlayer(profession, (int) Math.ceil(baseExperience * experienceModifier));
            }
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        @Nullable Item caught = event.getCaught() instanceof Item ? (Item) event.getCaught() : null;

        if (caught != null) {
            List<WorkforceUserProfession> relevantProfessions = WorkforceService.GetRelevantActiveUserProfessions(
                    player.getUniqueId().toString(),
                    new WorkforceAttributeType[]{ WorkforceAttributeType.BONUS_FISHING_DROPS, WorkforceAttributeType.FISHING_EXPERIENCE },
                    caught.getItemStack().getType().name()
            );

            if (relevantProfessions.size() > 0) {
                for (WorkforceUserProfession profession : relevantProfessions) {
                    final int baseExperience = (int) EvaluationService.evaluate(
                            EvaluationService.populateEquation(
                                    WorkforceService.earnedExperienceEquation,
                                    new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                            )
                    );
                    final double basePayment = EvaluationService.evaluate(
                            EvaluationService.populateEquation(
                                    profession.getProfession().paymentEquation,
                                    new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                            )
                    );
                    double experienceModifier = 0.0, paymentModifier = 0.0;

                    List<WorkforceAttribute> attributes = profession.getProfession().getRelevantAttributes(
                            new WorkforceAttributeType[]{ WorkforceAttributeType.BONUS_FISHING_DROPS, WorkforceAttributeType.FISHING_EXPERIENCE },
                            profession.getLevel(),
                            caught.getItemStack().getType().name()
                    );
                    Random generator = new Random();
                    for (WorkforceAttribute attribute : attributes) {
                        experienceModifier = attribute.experienceModifier > experienceModifier ? attribute.experienceModifier : experienceModifier;
                        paymentModifier = attribute.paymentModifier > paymentModifier ? attribute.paymentModifier : paymentModifier;

                        final double activationChance = EvaluationService.evaluate(
                                EvaluationService.populateEquation(
                                        attribute.getEquation("chance"),
                                        new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                                )
                        );
                        final double activationRoll = generator.nextDouble();

                        if (activationRoll <= activationChance) {
                            final int increaseAmount = (int) Math.ceil(
                                    EvaluationService.evaluate(
                                            EvaluationService.populateEquation(
                                                    attribute.getEquation("amount"),
                                                    new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                                            )
                                    )
                            );

                            if (attribute.type == WorkforceAttributeType.FISHING_EXPERIENCE) {
                                event.setExpToDrop(event.getExpToDrop() + increaseAmount);
                            } else if (attribute.type == WorkforceAttributeType.BONUS_FISHING_DROPS) {
                                ItemStack itemStack = caught.getItemStack();
                                itemStack.setAmount(itemStack.getAmount() + increaseAmount);

                                caught.setItemStack(itemStack);
                            }
                        }
                    }

                    WorkforceService.RewardPlayer(profession, (int) Math.ceil(baseExperience * experienceModifier));
                    if (profession.getProfession().isPaymentEnabled()) {
                        EconomyService.RewardPlayer(player.getUniqueId().toString(), basePayment * paymentModifier, profession.getProfession().name);
                    }
                }
            }
        }
    }
}
