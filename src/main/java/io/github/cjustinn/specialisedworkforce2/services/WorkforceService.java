package io.github.cjustinn.specialisedworkforce2.services;

import io.github.cjustinn.specialisedlib.Database.DatabaseService;
import io.github.cjustinn.specialisedlib.Database.DatabaseValue;
import io.github.cjustinn.specialisedlib.Database.DatabaseValueType;
import io.github.cjustinn.specialisedlib.Logging.LoggingService;
import io.github.cjustinn.specialisedworkforce2.enums.DatabaseQuery;
import io.github.cjustinn.specialisedworkforce2.enums.WorkforceAttributeType;
import io.github.cjustinn.specialisedworkforce2.enums.WorkforceRewardType;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceProfession;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceUserProfession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class WorkforceService {
    // Workforce Settings
    public static Map<String, Integer> maximumJobs = new HashMap<String, Integer>();
    public static String requiredExperienceEquation = "1";
    public static String earnedExperienceEquation = "1";
    public static double jobQuitLossRate = 0.2;
    public static int maximumLevel = 1;
    public static int maxDescriptionLength = 150;

    // Lists
    public static List<WorkforceProfession> professions = new ArrayList<WorkforceProfession>();
    public static List<WorkforceUserProfession> userProfessions = new ArrayList<WorkforceUserProfession>();

    //
    public static boolean UserHasActiveProfession(String user, String job) {
        return userProfessions.stream().anyMatch(
                (prof) ->
                        prof.employsPlayer(user) && prof.jobId.equalsIgnoreCase(job) && prof.isActive()
        );
    }

    public static List<WorkforceUserProfession> GetActiveUserProfessionsWithAttribute(String user, WorkforceAttributeType type) {
        return userProfessions.stream().filter(
                (userProfession) ->
                        userProfession.isActive() && userProfession.employsPlayer(user) && userProfession.getProfession().hasAttributeOfType(type, userProfession.getLevel())
        ).collect(Collectors.toList());
    }

    public static List<WorkforceUserProfession> GetRelevantActiveUserProfessions(String user, WorkforceAttributeType[] types, String target) {
        return new ArrayList<WorkforceUserProfession>() {{
            for (WorkforceAttributeType type : types) {
                addAll(
                        WorkforceService.GetActiveUserProfessionsWithAttribute(user, type).stream().filter(
                                (userProfession) -> {
                                    return userProfession.getProfession().getAttributesByType(type).stream().anyMatch(
                                            (attribute) -> attribute.targets(target)
                                    );
                                }
                        ).collect(Collectors.toList())
                );
            }
        }};
    }

    public static List<WorkforceUserProfession> GetUserProfessions(String user) {
        return userProfessions.stream().filter(
                (profession) -> {
                    return profession.employsPlayer(user);
                }
        ).collect(Collectors.toList());
    }

    public static List<WorkforceUserProfession> GetActiveUserProfessions(String user) {
        return userProfessions.stream().filter(
                (profession) -> {
                    return profession.employsPlayer(user) && profession.isActive();
                }
        ).collect(Collectors.toList());
    }

    public static List<WorkforceUserProfession> GetActiveUserProfessions() {
        return userProfessions.stream().filter((profession) -> profession.isActive()).collect(Collectors.toList());
    }

    public static int GetActiveProfessionsInGroup(String user, String group) {
        return (int) GetUserProfessions(user).stream().filter((profession) -> profession.isActive() && profession.getProfession().group.equalsIgnoreCase(group)).count();
    }

    public static int GetMaxJobsForGroup(String group) {
        return maximumJobs.containsKey(group) ? maximumJobs.get(group) : 1;
    }

    public static int GetIndexOfUserProfession(String user, String job) {
        int index = -1;

        for (int i = 0; i < userProfessions.size() && index < 0; i++) {
            if (userProfessions.get(i).employsPlayer(user) && userProfessions.get(i).jobId.equals(job)) {
                index = i;
            }
        }

        return index;
    }

    public static @Nullable WorkforceProfession GetProfessionById(String id) {
        WorkforceProfession profession = null;

        for (WorkforceProfession prof : professions) {
            if (prof.id.equalsIgnoreCase(id))
                profession = prof;
        }

        return profession;
    }

    public static boolean AddProfessionToUser(String user, String job) {
        final List<WorkforceUserProfession> existingProfessionRecords = GetUserProfessions(user);
        final boolean recordExists = existingProfessionRecords.stream().anyMatch((prof) -> prof.jobId.equals(job));
        final boolean recordExistsAndActive = existingProfessionRecords.stream().anyMatch((prof) -> prof.jobId.equals(job) && prof.isActive());

        if (recordExists) {
            final int existingIndex = GetIndexOfUserProfession(user, job);

            if (!recordExistsAndActive) {
                WorkforceUserProfession profession = userProfessions.get(existingIndex);
                profession.setActive(true);

                profession.update();

                userProfessions.set(existingIndex, profession);

                Bukkit.getPlayer(UUID.fromString(user)).sendMessage(
                        Component.text("You are now a level ")
                                .append(Component.text(String.format("%d %s", profession.getLevel(), profession.getProfession().name), NamedTextColor.GOLD))
                                .append(Component.text("!"))
                );
            }
        } else {
            // Add the entry to the DB and store a new object.
            final boolean success = DatabaseService.RunUpdate(DatabaseQuery.InsertUser, new DatabaseValue[]{
               new DatabaseValue(1, user, DatabaseValueType.String),
               new DatabaseValue(2, job, DatabaseValueType.String)
            });

            if (success) {
                final WorkforceUserProfession profession = new WorkforceUserProfession(user, 1, 0, job, 1);
                userProfessions.add(profession);

                Bukkit.getPlayer(UUID.fromString(user)).sendMessage(
                        Component.text("You are now a level ")
                                .append(Component.text(String.format("%d %s", 1, profession.getProfession().name), NamedTextColor.GOLD))
                                .append(Component.text("!"))
                );
            } else {
                Bukkit.getPlayer(UUID.fromString(user)).sendMessage(Component.text("Something went wrong! You were not assigned to the selected profession. Please try again.", NamedTextColor.RED));
            }
        }

        return true;
    }

    public static void RewardPlayerMinecraftExperience(String recipient, int amount, String cause) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(recipient));
        if (offlinePlayer.isOnline()) {
            offlinePlayer.getPlayer().giveExp(amount);
        } else {
            if (!AttributeLoggingService.BacklogReward(WorkforceRewardType.PROFESSION_EXPERIENCE, recipient, amount, cause)) {
                LoggingService.writeLog(
                        Level.SEVERE,
                        String.format(
                                "Unable to backlog minecraft experience reward for user %s: %d for %s profession.",
                                recipient,
                                amount,
                                cause
                        )
                );
            }
        }
    }

    public static void RewardPlayer(WorkforceUserProfession profession, int amount) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(profession.uuid));
        if (offlinePlayer.isOnline()) {
            profession.addExperience(amount);
        } else {
            if (!AttributeLoggingService.BacklogReward(WorkforceRewardType.PROFESSION_EXPERIENCE, profession.uuid, amount, profession.getProfession().name)) {
                LoggingService.writeLog(
                        Level.SEVERE,
                        String.format(
                                "Unable to backlog profession experience reward for user %s: %d for %s profession.",
                                profession.uuid,
                                amount,
                                profession.getProfession().name
                        )
                );
            }
        }
    }

    public static void RewardPlayer(String professionName, String recipient, int amount) {
        WorkforceUserProfession relevantProfession = GetActiveUserProfessions(recipient).stream().filter((p) -> p.getProfession().name.equals(professionName)).collect(Collectors.toList()).get(0);
        if (relevantProfession != null) {
            relevantProfession.addExperience(amount);
        }
    }

    public static List<TextComponent> ConformStringToMaxLength(String original, @Nullable NamedTextColor color) {
        List<TextComponent> lines = new ArrayList<TextComponent>();
        String[] originalParts = original.split(" ");

        String current = "";
        for (int i = 0; i < originalParts.length; i++) {
            if (current.equals("")) {
                current = originalParts[i];
            } else if (String.format("%s %s", current, originalParts[i]).length() > maxDescriptionLength) {
                lines.add(Component.text(current, color));
                current = originalParts[i];
            } else if (String.format("%s %s", current, originalParts[i]).length() == maxDescriptionLength) {
                current = String.format("%s %s", current, originalParts[i]);
                lines.add(Component.text(current, color));

                current = "";
            } else {
                current = String.format("%s %s", current, originalParts[i]);
            }

            if (i == originalParts.length - 1 && !current.trim().equals("")) {
                lines.add(Component.text(current, color));
            }
        }

        return lines;
    }

    public static List<TextComponent> ConformStringToMaxLength(String original) {
        return ConformStringToMaxLength(original, null);
    }
}
