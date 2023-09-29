package io.github.cjustinn.specialisedworkforce2.commands.handlers;

import io.github.cjustinn.specialisedworkforce2.models.WorkforceProfession;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceUserProfession;
import io.github.cjustinn.specialisedworkforce2.services.WorkforceService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WorkforceAdminCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 5) {
            if (addSetSubcommandProvided(args[0], sender)
                    && levelsExperienceSubcommandProvided(args[1], sender)
                    && userIsValid(args[2], sender)
                    && userHasProfession(args[2], args[3], sender)
                    && valueIsNumeric(args[4], sender)
            ) {
                final OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[2]);
                final String targetUserId = targetPlayer.getUniqueId().toString();
                final int professionIndex = WorkforceService.GetIndexOfUserProfession(targetUserId, args[3]);

                if (args[0].equals("add") && args[1].equals("level")) {
                    WorkforceService.userProfessions.get(professionIndex).addLevel(Integer.parseInt(args[4]));
                } else if (args[0].equals("add") && args[1].equals("experience")) {
                    WorkforceService.userProfessions.get(professionIndex).addExperience(Integer.parseInt(args[4]));
                } else if (args[0].equals("set") && args[1].equals("level")) {
                    WorkforceService.userProfessions.get(professionIndex).setLevel(Integer.parseInt(args[4]));
                } else if (args[0].equals("set") && args[1].equals("experience")) {
                    WorkforceService.userProfessions.get(professionIndex).setExperience(Integer.parseInt(args[4]));
                }

                if (args[0].equals("add")) {
                    sender.sendMessage(Component.text(
                            String.format("You have added %d %s to %s's %s profession.", Integer.parseInt(args[4]), args[1].equals("level") ? "level" + (Integer.parseInt(args[4]) == 1 ? "" : 's'): "experience", targetPlayer.getName(), WorkforceService.userProfessions.get(professionIndex).getProfession().name),
                            NamedTextColor.GREEN
                    ));
                } else {
                    sender.sendMessage(Component.text(
                            String.format("You have set %s's %s %s to %d.", targetPlayer.getName(), WorkforceService.userProfessions.get(professionIndex).getProfession().name, args[1], Integer.parseInt(args[4])),
                            NamedTextColor.GREEN
                    ));
                }
            }
        } else {
            sender.sendMessage(Component.text("That is not a valid admin sub-command.", NamedTextColor.RED));
        }

        return true;
    }

    private boolean addSetSubcommandProvided(String subcommand, CommandSender sender) {
        final String[] commandKeywords = new String[] { "add", "set" };
        final boolean isValid = Arrays.stream(commandKeywords).anyMatch((keyword) -> keyword.equals(subcommand));

        if (!isValid) {
            sender.sendMessage(Component.text(
                    String.format("That is not a valid sub-command. Expected: <%s>", Arrays.stream(commandKeywords).reduce("", (acc, curr) -> {
                        return acc.isEmpty() ? curr : String.format("/%s", curr);
                    }))
            , NamedTextColor.RED));
        }

        return isValid;
    }

    private boolean levelsExperienceSubcommandProvided(String subcommand, CommandSender sender) {
        final String[] commandKeywords = new String[] { "level", "experience" };
        final boolean isValid = Arrays.stream(commandKeywords).anyMatch((keyword) -> keyword.equals(subcommand));

        if (!isValid) {
            sender.sendMessage(Component.text(
                    String.format("That is not a valid sub-command. Expected: <%s>", Arrays.stream(commandKeywords).reduce("", (acc, curr) -> {
                        return acc.isEmpty() ? curr : String.format("/%s", curr);
                    }))
                    , NamedTextColor.RED));
        }

        return isValid;
    }

    private boolean userIsValid(String user, CommandSender sender) {
        final String targetUserId = Bukkit.getOfflinePlayer(user).getUniqueId().toString();
        final boolean isValid = WorkforceService.userProfessions.stream().anyMatch((prof) -> prof.uuid.equals(targetUserId));

        if (!isValid) {
            sender.sendMessage(Component.text("The provided user does not exist, or has no professions.", NamedTextColor.RED));
        }

        return isValid;
    }

    private boolean userHasProfession(String user, String job, CommandSender sender) {
        final String targetUserId = Bukkit.getOfflinePlayer(user).getUniqueId().toString();
        final boolean isValid = WorkforceService.UserHasActiveProfession(targetUserId, job);

        if (!isValid) {
            sender.sendMessage(Component.text(String.format("The provided user is not a member of the specified profession!"), NamedTextColor.RED));
        }

        return isValid;
    }

    private boolean valueIsNumeric(String value, CommandSender sender) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException err) {
            sender.sendMessage(Component.text(String.format("The provided amount must be a number!"), NamedTextColor.RED));
            return false;
        }
    }
}
