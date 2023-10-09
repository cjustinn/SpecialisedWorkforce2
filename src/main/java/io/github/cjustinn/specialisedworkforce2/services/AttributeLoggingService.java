package io.github.cjustinn.specialisedworkforce2.services;

import io.github.cjustinn.specialisedworkforce2.enums.AttributeLogInteractionMode;
import io.github.cjustinn.specialisedworkforce2.enums.AttributeLogType;
import io.github.cjustinn.specialisedworkforce2.enums.WorkforceRewardType;
import io.github.cjustinn.specialisedworkforce2.models.SQL.MySQLProperty;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceInteractionLogValue;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceRewardBacklogItem;
import org.bukkit.Location;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                success = SQLService.RunUpdate(
                        "UPDATE workforce_reward_backlog SET amount = ? WHERE rewardType = ? AND uuid = ? AND rewardFrom = ?;",
                        new MySQLProperty[]{
                                new MySQLProperty("double", amount + existingBacklogItem.amount, 1),
                                new MySQLProperty("integer", type.id, 2),
                                new MySQLProperty("string", recipient, 3),
                                new MySQLProperty("string", cause, 4)
                        }
                );

                if (success) {
                    existingBacklogItem.amount += amount;
                    rewardBacklog.set(matchIndex, existingBacklogItem);
                }
            }
        } else {
            success = SQLService.RunUpdate(
                    "INSERT INTO workforce_reward_backlog (rewardType, uuid, amount, rewardFrom) VALUES (?, ?, ?, ?);",
                    new MySQLProperty[]{
                            new MySQLProperty("integer", type.id, 1),
                            new MySQLProperty("string", recipient, 2),
                            new MySQLProperty("double", amount, 3),
                            new MySQLProperty("string", cause, 4)
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
            if (type == AttributeLogType.FURNACE) {
                final boolean existingLogMatchesValue = logAlreadyExists && logs.get(location).uuid.equals(uuid);
                String query = "";

                if (logAlreadyExists && !existingLogMatchesValue) {
                    query = "UPDATE workforce_interaction_log SET uuid = ? WHERE world = ? AND x = ? AND y = ? AND z = ?;";
                    success = SQLService.RunUpdate(query, new MySQLProperty[] {
                            new MySQLProperty("string", uuid, 1),
                            new MySQLProperty("string", location.getWorld().getName(), 2),
                            new MySQLProperty("integer", location.getBlockX(), 3),
                            new MySQLProperty("integer", location.getBlockY(), 4),
                            new MySQLProperty("integer", location.getBlockZ(), 5)
                    });
                } else if (!logAlreadyExists) {
                    query = "INSERT INTO workforce_interaction_log (interactionType, world, x, y, z, uuid) VALUES (?, ?, ?, ?, ?, ?);";
                    success = SQLService.RunUpdate(query, new MySQLProperty[] {
                            new MySQLProperty("integer", type.value, 1),
                            new MySQLProperty("string", location.getWorld().getName(), 2),
                            new MySQLProperty("integer", location.getBlockX(), 3),
                            new MySQLProperty("integer", location.getBlockY(), 4),
                            new MySQLProperty("integer", location.getBlockZ(), 5),
                            new MySQLProperty("string", uuid, 6)
                    });
                }

                if (success) {
                    AttributeLoggingService.logs.put(location, new WorkforceInteractionLogValue(uuid, type));
                }
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
            success = SQLService.RunUpdate("DELETE FROM workforce_interaction_log WHERE world = ? AND x = ? AND y = ? AND z = ?", new MySQLProperty[] {
                    new MySQLProperty("string", location.getWorld().getName(), 1),
                    new MySQLProperty("integer", location.getBlockX(), 2),
                    new MySQLProperty("integer", location.getBlockY(), 3),
                    new MySQLProperty("integer", location.getBlockZ(), 4),
            });
        } else {
            LoggingService.WriteError("Attempted to add a valid interaction to a log without required values.");
        }

        return success;
    }
}
