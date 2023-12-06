package io.github.cjustinn.specialisedworkforce2.listeners;

import io.github.cjustinn.specialisedworkforce2.models.WorkforceRewardBacklogItem;
import io.github.cjustinn.specialisedworkforce2.services.AttributeLoggingService;
import io.github.cjustinn.specialisedworkforce2.services.EconomyService;
import io.github.cjustinn.specialisedworkforce2.services.WorkforceService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.stream.Collectors;

public class WorkforceJoinLeaveListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        List<WorkforceRewardBacklogItem> backloggedRewards = AttributeLoggingService.rewardBacklog.stream().filter((reward) -> reward.recipient.equals(event.getPlayer().getUniqueId().toString())).collect(Collectors.toList());

        if (backloggedRewards.size() > 0) {
            for (WorkforceRewardBacklogItem reward : backloggedRewards) {
                final String rewardCauseMessage = reward.rewardingProfession != null ? String.format("your %s profession", reward.rewardingProfession) : "one of your professions";
                switch (reward.type) {
                    case ECONOMIC:
                        event.getPlayer().sendMessage(Component.text(
                                String.format("You earned %s while offline from %s.", EconomyService.getEconomy().format(reward.amount), rewardCauseMessage),
                                NamedTextColor.GRAY
                        ));
                        EconomyService.RewardPlayer(event.getPlayer().getUniqueId().toString(), reward.amount, reward.rewardingProfession);
                        break;
                    case EXPERIENCE:
                        event.getPlayer().sendMessage(Component.text(
                                String.format("You earned %d experience while offline from %s.", (int) Math.floor(reward.amount), rewardCauseMessage),
                                NamedTextColor.GRAY
                        ));
                        WorkforceService.RewardPlayerMinecraftExperience(event.getPlayer().getUniqueId().toString(), (int) Math.floor(reward.amount), reward.rewardingProfession);
                        break;
                    case PROFESSION_EXPERIENCE:
                        event.getPlayer().sendMessage(Component.text(
                                String.format("You earned %d profession experience while offline from %s.", (int) Math.floor(reward.amount), rewardCauseMessage),
                                NamedTextColor.GRAY
                        ));
                        WorkforceService.RewardPlayer(reward.rewardingProfession, reward.recipient, (int) Math.floor(reward.amount));
                        break;
                }

                reward.removeBackloggedReward();
            }
        }
    }
}
