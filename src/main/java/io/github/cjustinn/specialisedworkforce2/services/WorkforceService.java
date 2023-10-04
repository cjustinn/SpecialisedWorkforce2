package io.github.cjustinn.specialisedworkforce2.services;

import io.github.cjustinn.specialisedworkforce2.enums.WorkforceAttributeType;
import io.github.cjustinn.specialisedworkforce2.models.SQL.MySQLProperty;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceProfession;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceUserProfession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class WorkforceService {
    // Workforce Settings
    public static Map<String, Integer> maximumJobs = new HashMap<String, Integer>();
    public static String requiredExperienceEquation = "1";
    public static String earnedExperienceEquation = "1";
    public static double jobQuitLossRate = 0.2;
    public static int maximumLevel = 1;

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
            final String query = "INSERT INTO workforce_employment (uuid, jobId) VALUES (?, ?)";
            final boolean success = SQLService.RunUpdate(query, new MySQLProperty[]{
               new MySQLProperty("string", user, 1),
               new MySQLProperty("string", job, 2)
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
}
