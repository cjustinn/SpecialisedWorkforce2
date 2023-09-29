package io.github.cjustinn.specialisedworkforce2.commands.tabcompleters;

import io.github.cjustinn.specialisedworkforce2.services.WorkforceService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class WorkforceAdminTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<String>();

        if (args.length == 1) {
            // Add/Set
            suggestions.addAll(Arrays.stream(new String[] {
                    "add",
                    "set"
            }).collect(Collectors.toList()));
        } else if (args.length == 2) {
            // Level/Experience
            suggestions.addAll(Arrays.stream(new String[] {
                    "level",
                    "experience"
            }).collect(Collectors.toList()));
        } else if (args.length == 3) {
            // User
            suggestions.addAll(
                    WorkforceService.userProfessions.stream()
                            .map((userProfession) -> userProfession.uuid)
                            .distinct()
                            .map((uuid) -> {
                                return Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
                            })
                            .collect(Collectors.toList())
            );
        } else if (args.length == 4) {
            // Job
            OfflinePlayer player = Bukkit.getOfflinePlayer(args[2]);
            suggestions.addAll(WorkforceService.GetUserProfessions(player.getUniqueId().toString()).stream().map(
                    (userProfession) -> userProfession.jobId
            ).collect(Collectors.toList()));
        }

        return suggestions;
    }
}
