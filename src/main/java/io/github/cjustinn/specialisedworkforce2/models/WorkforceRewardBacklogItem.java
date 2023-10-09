package io.github.cjustinn.specialisedworkforce2.models;

import io.github.cjustinn.specialisedworkforce2.enums.WorkforceRewardType;
import io.github.cjustinn.specialisedworkforce2.models.SQL.MySQLProperty;
import io.github.cjustinn.specialisedworkforce2.services.AttributeLoggingService;
import io.github.cjustinn.specialisedworkforce2.services.SQLService;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class WorkforceRewardBacklogItem {
    public final @Nullable WorkforceRewardType type;
    public final String recipient;
    public double amount;
    public final String rewardingProfession;

    public WorkforceRewardBacklogItem(int type, String recipient, double amount, String cause) {
        this.recipient = recipient;
        this.amount = amount;
        this.rewardingProfession = cause;

        Optional<WorkforceRewardType> optionalType = Arrays.stream(WorkforceRewardType.values()).filter((t) -> t.id == type).findFirst();
        this.type = optionalType.isPresent() ? optionalType.get() : null;
    }

    public boolean removeBackloggedReward() {
        AttributeLoggingService.rewardBacklog = AttributeLoggingService.rewardBacklog.stream().filter(
                (backlog) -> !(backlog.recipient.equals(this.recipient) && backlog.type == this.type && backlog.rewardingProfession.equals(this.rewardingProfession))
        ).collect(Collectors.toList());

        return SQLService.RunUpdate(
                "DELETE FROM workforce_reward_backlog WHERE rewardType = ? AND uuid = ? AND rewardFrom = ?;",
                new MySQLProperty[] {
                        new MySQLProperty("integer", this.type.id, 1),
                        new MySQLProperty("string", this.recipient, 2),
                        new MySQLProperty("string", this.rewardingProfession, 3),
                }
        );
    }
}
