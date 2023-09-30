package io.github.cjustinn.specialisedworkforce2.listeners.attributes;

import io.github.cjustinn.specialisedworkforce2.enums.WorkforceAttributeType;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceAttribute;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceUserProfession;
import io.github.cjustinn.specialisedworkforce2.services.EvaluationService;
import io.github.cjustinn.specialisedworkforce2.services.WorkforceService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class WorkforcePlayerListener implements Listener {
    @EventHandler
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        Material itemType = event.getItem().getType();

        List<WorkforceUserProfession> relevantProfessions =
                WorkforceService.GetActiveUserProfessionsWithAttribute(player.getUniqueId().toString(), WorkforceAttributeType.DURABILITY_SAVE)
                        .stream().filter(
                                (userProfession) -> {
                                    return userProfession.getProfession().getAttributesByType(WorkforceAttributeType.DURABILITY_SAVE, userProfession.getLevel())
                                            .stream().anyMatch((attribute) -> attribute.targets(itemType.name()));
                                }
                        ).collect(Collectors.toList());

        if (relevantProfessions.size() > 0) {
            for (WorkforceUserProfession profession : relevantProfessions) {
                // Give the user profession experience - at a quarter the configured rate.
                final int experienceToAdd = (int) Math.ceil(EvaluationService.evaluate(
                        WorkforceService.earnedExperienceEquation.replace("{level}", String.valueOf(profession.getLevel()))
                ) * 0.25);

                profession.addExperience(experienceToAdd);

                // Iterate through any relevant attributes, trigger them if they proc.
                Random generator = new Random();
                for (final WorkforceAttribute attribute : profession.getProfession().getAttributesByType(WorkforceAttributeType.DURABILITY_SAVE, profession.getLevel())) {
                    final double activationChance = EvaluationService.evaluate(attribute.getEquation("chance").replace("{level}", String.valueOf(profession.getLevel())));
                    final double activationRoll = generator.nextDouble();

                    if (activationRoll <= activationChance) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
