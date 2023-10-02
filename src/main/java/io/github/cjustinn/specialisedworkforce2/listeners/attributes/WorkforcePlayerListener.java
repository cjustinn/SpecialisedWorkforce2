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
                final int baseExperience = (int) Math.ceil(EvaluationService.evaluate(WorkforceService.earnedExperienceEquation.replace("{level}", String.valueOf(profession.getLevel()))));
                double experienceModifier = 0.0;

                // Iterate through any relevant attributes, trigger them if they proc.
                Random generator = new Random();
                final List<WorkforceAttribute> attributes = profession.getProfession().getAttributesByType(WorkforceAttributeType.DURABILITY_SAVE, profession.getLevel())
                        .stream().filter((attribute) -> attribute.targets(itemType.name())).collect(Collectors.toList());
                for (final WorkforceAttribute attribute : attributes) {
                    experienceModifier = attribute.experienceModifier > experienceModifier ? attribute.experienceModifier : experienceModifier;

                    final double activationChance = EvaluationService.evaluate(attribute.getEquation("chance").replace("{level}", String.valueOf(profession.getLevel())));
                    final double activationRoll = generator.nextDouble();

                    if (activationRoll <= activationChance) {
                        event.setCancelled(true);
                    }
                }

                profession.addExperience((int) Math.ceil(baseExperience * experienceModifier));
            }
        }
    }
}
