package io.github.cjustinn.specialisedworkforce2.listeners.attributes;

import io.github.cjustinn.specialisedworkforce2.enums.WorkforceAttributeType;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceAttribute;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceUserProfession;
import io.github.cjustinn.specialisedworkforce2.services.EconomyService;
import io.github.cjustinn.specialisedworkforce2.services.EvaluationService;
import io.github.cjustinn.specialisedworkforce2.services.WorkforceService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class WorkforceEntityListener implements Listener {
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();

        if (killer != null) {
            List<WorkforceUserProfession> relevantProfessions =
                    WorkforceService.GetActiveUserProfessionsWithAttribute(killer.getUniqueId().toString(), WorkforceAttributeType.BONUS_MOB_DROPS)
                            .stream().filter(
                                    (userProfession) -> {
                                        return userProfession.getProfession().getAttributesByType(WorkforceAttributeType.BONUS_MOB_DROPS, userProfession.getLevel()).stream().anyMatch(
                                                (attribute) -> attribute.targets(event.getEntity().getType().name())
                                        );
                                    }
                            ).collect(Collectors.toList());

            if (relevantProfessions.size() > 0) {
                for (final WorkforceUserProfession profession : relevantProfessions) {
                    final double basePayment = EconomyService.CalculateMonetaryReward(profession.getProfession().paymentEquation, profession.getLevel());
                    final int baseExperience = (int) EvaluationService.evaluate(
                            WorkforceService.earnedExperienceEquation.replace("{level}", String.valueOf(profession.getLevel()))
                    );
                    double paymentModifier = 0.0, experienceModifier = 0.0;

                    // Iterate through any relevant attributes, activate them as necessary.
                    Random generator = new Random();
                    final List<WorkforceAttribute> attributes = profession.getProfession().getAttributesByType(WorkforceAttributeType.BONUS_MOB_DROPS, profession.getLevel())
                            .stream().filter((attribute) -> attribute.targets(event.getEntity().getType().name())).collect(Collectors.toList());
                    for (WorkforceAttribute attribute : attributes) {
                        paymentModifier = attribute.paymentModifier > paymentModifier ? attribute.paymentModifier : paymentModifier;
                        experienceModifier = attribute.experienceModifier > experienceModifier ? attribute.experienceModifier : experienceModifier;

                        final double activationRoll = generator.nextDouble();
                        final double activationChance = EvaluationService.evaluate(
                                attribute.getEquation("chance").replace("{level}", String.valueOf(profession.getLevel()))
                        );

                        if (activationRoll <= activationChance) {
                            final int amount = (int) Math.ceil(EvaluationService.evaluate(
                                    attribute.getEquation("amount").replace("{level}", String.valueOf(profession.getLevel()))
                            ));

                            for (ItemStack drop : event.getDrops()) {
                                drop.setAmount(drop.getAmount() + amount);
                            }
                        }
                    }

                    // Pay the player (if payment / economic integration is enabled).
                    if (profession.getProfession().isPaymentEnabled()) {
                        EconomyService.ModifyFunds(killer, basePayment * paymentModifier);
                    }

                    // Add job experience to the relevant user profession.
                    profession.addExperience((int) Math.ceil(baseExperience * experienceModifier));
                }
            }
        }
    }

    @EventHandler
    public void onEntityBreed(EntityBreedEvent event) {
        if (event.getBreeder() != null && event.getBreeder() instanceof Player) {
            Player player = (Player) event.getBreeder();

            List<WorkforceUserProfession> experienceProfessions = WorkforceService.GetActiveUserProfessionsWithAttribute(player.getUniqueId().toString(), WorkforceAttributeType.ANIMAL_BREED_EXPERIENCE)
                    .stream().filter(
                            (userProfession) -> {
                                return userProfession.getProfession().getAttributesByType(WorkforceAttributeType.ANIMAL_BREED_EXPERIENCE, userProfession.getLevel()).stream().anyMatch(
                                        (attribute) -> attribute.targets(event.getEntity().getType().name())
                                );
                            }
                    ).collect(Collectors.toList());

            List<WorkforceUserProfession> bonusProfessions = WorkforceService.GetActiveUserProfessionsWithAttribute(player.getUniqueId().toString(), WorkforceAttributeType.ANIMAL_BREED_BONUS)
                    .stream().filter(
                            (userProfession) -> {
                                return userProfession.getProfession().getAttributesByType(WorkforceAttributeType.ANIMAL_BREED_BONUS, userProfession.getLevel()).stream().anyMatch(
                                        (attribute) -> attribute.targets(event.getEntity().getType().name())
                                );
                            }
                    ).collect(Collectors.toList());

            List<WorkforceUserProfession> relevantProfessions = new ArrayList<WorkforceUserProfession>() {{
                addAll(bonusProfessions);
                addAll(experienceProfessions);
            }};

            if (relevantProfessions.size() > 0) {
                for (final WorkforceUserProfession profession : relevantProfessions) {
                    final double basePayment = EconomyService.CalculateMonetaryReward(profession.getProfession().paymentEquation, profession.getLevel());
                    final int baseExperience = (int) Math.ceil(EvaluationService.evaluate(WorkforceService.earnedExperienceEquation.replace("{level}", String.valueOf(profession.getLevel()))));
                    double paymentModifier = 0.0, experienceModifier = 0.0;

                    Random generator = new Random();
                    List<WorkforceAttribute> attributes = new ArrayList<WorkforceAttribute>() {{
                        addAll(profession.getProfession().getAttributesByType(WorkforceAttributeType.ANIMAL_BREED_EXPERIENCE, profession.getLevel())
                                .stream().filter(
                                        (attribute) -> attribute.targets(event.getEntity().getType().name())
                                ).collect(Collectors.toList()));
                        addAll(profession.getProfession().getAttributesByType(WorkforceAttributeType.ANIMAL_BREED_BONUS, profession.getLevel())
                                .stream().filter(
                                        (attribute) -> attribute.targets(event.getEntity().getType().name())
                                ).collect(Collectors.toList()));
                    }};
                    for (WorkforceAttribute attribute : attributes) {
                        paymentModifier = attribute.paymentModifier > paymentModifier ? attribute.paymentModifier : paymentModifier;
                        experienceModifier = attribute.experienceModifier > experienceModifier ? attribute.experienceModifier : experienceModifier;

                        final double activationChance = EvaluationService.evaluate(attribute.getEquation("chance").replace("{level}", String.valueOf(profession.getLevel())));
                        final double activationRoll = generator.nextDouble();

                        if (activationRoll <= activationChance) {
                            final int amount = (int) Math.ceil(EvaluationService.evaluate(attribute.getEquation("amount").replace("{level}", String.valueOf(profession.getLevel()))));
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

                    profession.addExperience((int) Math.ceil(baseExperience * experienceModifier));
                    if (profession.getProfession().isPaymentEnabled()) {
                        EconomyService.ModifyFunds(player, basePayment * paymentModifier);
                    }
                }
            }
        }
    }
}
