package io.github.cjustinn.specialisedworkforce2.listeners.attributes;

import io.github.cjustinn.specialisedworkforce2.enums.WorkforceAttributeType;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceAttribute;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceUserProfession;
import io.github.cjustinn.specialisedworkforce2.services.EconomyService;
import io.github.cjustinn.specialisedworkforce2.services.EvaluationService;
import io.github.cjustinn.specialisedworkforce2.services.WorkforceService;
import org.bukkit.Location;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class WorkforceEntityListener implements Listener {
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();

        if (killer != null) {
            List<WorkforceUserProfession> relevantProfessions = WorkforceService.GetRelevantActiveUserProfessions(
                    killer.getUniqueId().toString(),
                    new WorkforceAttributeType[] { WorkforceAttributeType.BONUS_MOB_DROPS },
                    event.getEntity().getType().name()
            );

            if (relevantProfessions.size() > 0) {
                for (final WorkforceUserProfession profession : relevantProfessions) {
                    final double basePayment = EconomyService.CalculateMonetaryReward(profession.getProfession().paymentEquation, profession.getLevel());
                    final int baseExperience = (int) EvaluationService.evaluate(
                            EvaluationService.populateEquation(
                                    WorkforceService.earnedExperienceEquation,
                                    new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                            )
                    );
                    double paymentModifier = 0.0, experienceModifier = 0.0;

                    // Iterate through any relevant attributes, activate them as necessary.
                    Random generator = new Random();
                    final List<WorkforceAttribute> attributes = profession.getProfession().getRelevantAttributes(
                            new WorkforceAttributeType[]{ WorkforceAttributeType.BONUS_MOB_DROPS },
                            profession.getLevel(),
                            event.getEntity().getType().name()
                    );
                    for (WorkforceAttribute attribute : attributes) {
                        paymentModifier = attribute.paymentModifier > paymentModifier ? attribute.paymentModifier : paymentModifier;
                        experienceModifier = attribute.experienceModifier > experienceModifier ? attribute.experienceModifier : experienceModifier;

                        final double activationRoll = generator.nextDouble();
                        final double activationChance = EvaluationService.evaluate(
                                EvaluationService.populateEquation(
                                        attribute.getEquation("chance"),
                                        new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                                )
                        );

                        if (activationRoll <= activationChance) {
                            final int amount = (int) Math.ceil(EvaluationService.evaluate(
                                    EvaluationService.populateEquation(
                                            attribute.getEquation("amount"),
                                            new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                                    )
                            ));

                            for (ItemStack drop : event.getDrops()) {
                                drop.setAmount(drop.getAmount() + amount);
                            }
                        }
                    }

                    // Pay the player (if payment / economic integration is enabled).
                    if (profession.getProfession().isPaymentEnabled()) {
                        EconomyService.RewardPlayer(killer.getUniqueId().toString(), basePayment * paymentModifier, profession.getProfession().name);
                    }

                    // Add job experience to the relevant user profession.
                    WorkforceService.RewardPlayer(profession, (int) Math.ceil(baseExperience * experienceModifier));
                }
            }
        }
    }

    @EventHandler
    public void onEntityBreed(EntityBreedEvent event) {
        if (event.getBreeder() != null && event.getBreeder() instanceof Player) {
            Player player = (Player) event.getBreeder();

            List<WorkforceUserProfession> relevantProfessions = WorkforceService.GetRelevantActiveUserProfessions(
                    player.getUniqueId().toString(),
                    new WorkforceAttributeType[]{ WorkforceAttributeType.ANIMAL_BREED_EXPERIENCE, WorkforceAttributeType.ANIMAL_BREED_BONUS },
                    event.getEntity().getType().name()
            );

            if (relevantProfessions.size() > 0) {
                for (final WorkforceUserProfession profession : relevantProfessions) {
                    final double basePayment = EconomyService.CalculateMonetaryReward(profession.getProfession().paymentEquation, profession.getLevel());
                    final int baseExperience = (int) EvaluationService.evaluate(
                            EvaluationService.populateEquation(
                                    WorkforceService.earnedExperienceEquation,
                                    new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                            )
                    );
                    double paymentModifier = 0.0, experienceModifier = 0.0;

                    Random generator = new Random();
                    final List<WorkforceAttribute> attributes = profession.getProfession().getRelevantAttributes(
                            new WorkforceAttributeType[]{ WorkforceAttributeType.ANIMAL_BREED_EXPERIENCE, WorkforceAttributeType.ANIMAL_BREED_BONUS },
                            profession.getLevel(),
                            event.getEntity().getType().name()
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
                            final int amount = (int) Math.ceil(EvaluationService.evaluate(
                                    EvaluationService.populateEquation(
                                            attribute.getEquation("amount"),
                                            new HashMap<String, Object>() {{ put("level", profession.getLevel()); }}
                                    )
                            ));
                            if (attribute.type == WorkforceAttributeType.ANIMAL_BREED_EXPERIENCE) {
                                event.setExperience(event.getExperience() + amount);
                            } else {
                                Location location = event.getEntity().getLocation();
                                if (location != null) {
                                    for (int i = 0; i < amount; i++) {
                                        Ageable mob = (Ageable) location.getWorld().spawnEntity(location, event.getEntity().getType());
                                        mob.setBaby();
                                    }
                                }
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
