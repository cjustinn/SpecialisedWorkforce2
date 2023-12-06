package io.github.cjustinn.specialisedworkforce2.models;

import io.github.cjustinn.specialisedlib.Database.DatabaseService;
import io.github.cjustinn.specialisedlib.Database.DatabaseValue;
import io.github.cjustinn.specialisedlib.Database.DatabaseValueType;
import io.github.cjustinn.specialisedworkforce2.enums.DatabaseQuery;
import io.github.cjustinn.specialisedworkforce2.enums.WorkforceRewardType;
import io.github.cjustinn.specialisedworkforce2.services.AttributeLoggingService;

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

        return DatabaseService.RunUpdate(
                DatabaseQuery.DeleteBackloggedReward,
                new DatabaseValue[] {
                        new DatabaseValue(1, this.type.id, DatabaseValueType.Integer),
                        new DatabaseValue(2, this.recipient, DatabaseValueType.String),
                        new DatabaseValue(3, this.rewardingProfession, DatabaseValueType.String),
                }
        );
    }
}
