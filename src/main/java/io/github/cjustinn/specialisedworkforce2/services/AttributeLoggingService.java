package io.github.cjustinn.specialisedworkforce2.services;

import io.github.cjustinn.specialisedlib.Database.DatabaseService;
import io.github.cjustinn.specialisedlib.Database.DatabaseValue;
import io.github.cjustinn.specialisedlib.Database.DatabaseValueType;
import io.github.cjustinn.specialisedlib.Logging.LoggingService;
import io.github.cjustinn.specialisedworkforce2.enums.AttributeLogInteractionMode;
import io.github.cjustinn.specialisedworkforce2.enums.AttributeLogType;
import io.github.cjustinn.specialisedworkforce2.enums.DatabaseQuery;
import io.github.cjustinn.specialisedworkforce2.enums.WorkforceRewardType;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceInteractionLogValue;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceRewardBacklogItem;
import org.bukkit.Location;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class AttributeLoggingService {
    public static List<WorkforceRewardBacklogItem> rewardBacklog = new ArrayList<>();
    public static Map<Location, WorkforceInteractionLogValue> logs = new HashMap<>();

    public static boolean BacklogReward(WorkforceRewardType type, String recipient, double amount, @Nullable String cause) {
        final boolean existingMatchingBacklogItem = rewardBacklog.stream().anyMatch((backlog) -> backlog.type == type && backlog.recipient.equals(recipient) && backlog.rewardingProfession.equals(cause));
        boolean success = false;

        if (existingMatchingBacklogItem) {
            int matchIndex = -1;
            for (int i = 0; i < rewardBacklog.size() && matchIndex < 0; i++) {
                WorkforceRewardBacklogItem backlog = rewardBacklog.get(i);
                if (
                        backlog.type == type && backlog.recipient.equals(recipient) && backlog.rewardingProfession.equals(cause)
                ) {
                    matchIndex = i;
                }
            }

            if (matchIndex >= 0) {
                WorkforceRewardBacklogItem existingBacklogItem = rewardBacklog.get(matchIndex);
                success = DatabaseService.RunUpdate(
                        DatabaseQuery.UpdateBackloggedReward,
                        new DatabaseValue[]{
                                new DatabaseValue(1, amount + existingBacklogItem.amount, DatabaseValueType.Double),
                                new DatabaseValue(2, type.id, DatabaseValueType.Integer),
                                new DatabaseValue(3, recipient, DatabaseValueType.String),
                                new DatabaseValue(4, cause, DatabaseValueType.String)
                        }
                );

                if (success) {
                    existingBacklogItem.amount += amount;
                    rewardBacklog.set(matchIndex, existingBacklogItem);
                }
            }
        } else {
            success = DatabaseService.RunUpdate(
                    DatabaseQuery.InsertBacklogReward,
                    new DatabaseValue[]{
                            new DatabaseValue(1, type.id, DatabaseValueType.Integer),
                            new DatabaseValue(2, recipient, DatabaseValueType.String),
                            new DatabaseValue(3, amount, DatabaseValueType.Double),
                            new DatabaseValue(4, cause, DatabaseValueType.String)
                    }
            );

            if (success) {
                rewardBacklog.add(new WorkforceRewardBacklogItem(
                        type.id,
                        recipient,
                        amount,
                        cause
                ));
            }
        }

        return success;
    }

    public static boolean LogExists(Location location, AttributeLogType type) {
        return logs.containsKey(location) && logs.get(location).type == type;
    }

    public static boolean LogExists(Location location) {
        return logs.containsKey(location);
    }

    public static boolean LogInteraction(AttributeLogType type, AttributeLogInteractionMode mode, Location location, String uuid) {
        boolean success = false;

        final boolean logAlreadyExists = LogExists(location, type);

        if (mode == AttributeLogInteractionMode.CREATE) {
            final boolean existingLogMatchesValue = logAlreadyExists && logs.get(location).uuid.equals(uuid);
            String query = "";

            if (logAlreadyExists && !existingLogMatchesValue) {
                success = DatabaseService.RunUpdate(DatabaseQuery.UpdateLog, new DatabaseValue[] {
                        new DatabaseValue(1, uuid, DatabaseValueType.String),
                        new DatabaseValue(2, location.getWorld().getName(), DatabaseValueType.String),
                        new DatabaseValue(3, location.getBlockX(), DatabaseValueType.Integer),
                        new DatabaseValue(4, location.getBlockY(), DatabaseValueType.Integer),
                        new DatabaseValue(5, location.getBlockZ(), DatabaseValueType.Integer)
                });
            } else if (!logAlreadyExists) {
                success = DatabaseService.RunUpdate(DatabaseQuery.InsertLog, new DatabaseValue[] {
                        new DatabaseValue(1, type.value, DatabaseValueType.Integer),
                        new DatabaseValue(2, location.getWorld().getName(), DatabaseValueType.String),
                        new DatabaseValue(3, location.getBlockX(), DatabaseValueType.Integer),
                        new DatabaseValue(4, location.getBlockY(), DatabaseValueType.Integer),
                        new DatabaseValue(5, location.getBlockZ(), DatabaseValueType.Integer),
                        new DatabaseValue(6, uuid, DatabaseValueType.String)
                });
            }

            if (success) {
                AttributeLoggingService.logs.put(location, new WorkforceInteractionLogValue(uuid, type));
            }
        } else {
            success = LogInteraction(mode, location);
        }

        return success;
    }

    public static boolean LogInteraction(AttributeLogInteractionMode mode, Location location) {
        boolean success = false;

        if (mode == AttributeLogInteractionMode.REMOVE) {
            AttributeLoggingService.logs.remove(location);
            success = DatabaseService.RunUpdate(DatabaseQuery.DeleteLog, new DatabaseValue[] {
                    new DatabaseValue(1, location.getWorld().getName(), DatabaseValueType.String),
                    new DatabaseValue(2, location.getBlockX(), DatabaseValueType.Integer),
                    new DatabaseValue(3, location.getBlockY(), DatabaseValueType.Integer),
                    new DatabaseValue(4, location.getBlockZ(), DatabaseValueType.Integer),
            });
        } else {
            LoggingService.writeLog(Level.SEVERE, "Attempted to add a valid interaction to a log without required values.");
        }

        return success;
    }
}
