package io.github.cjustinn.specialisedworkforce2.listeners.attributes;

import io.github.cjustinn.specialisedworkforce2.enums.WorkforceAttributeType;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceAttribute;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceUserProfession;
import io.github.cjustinn.specialisedworkforce2.services.EconomyService;
import io.github.cjustinn.specialisedworkforce2.services.EvaluationService;
import io.github.cjustinn.specialisedworkforce2.services.WorkforceService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class WorkforceEntityListener implements Listener {
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();

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
