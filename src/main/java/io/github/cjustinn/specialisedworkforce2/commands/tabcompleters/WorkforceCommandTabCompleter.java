package io.github.cjustinn.specialisedworkforce2.commands.tabcompleters;

import io.github.cjustinn.specialisedworkforce2.services.WorkforceService;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WorkforceCommandTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<String>();

        if (args.length == 1) {
            options.addAll(Arrays.stream(new String[] {
                    "join",
                    "status",
                    "leaderboard"
            }).collect(Collectors.toList()));
        } else if (args.length == 2 && args[0].equals("leaderboard")) {
            options.addAll(WorkforceService.professions.stream().map((profession) -> profession.id).collect(Collectors.toList()));
        } else if (args.length == 2 && args[0].equals("join")) {
            options.addAll(
                    WorkforceService.professions.stream().map((profession) -> profession.group).distinct().collect(Collectors.toList())
            );
        }

        return options;
    }
}
