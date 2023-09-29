package io.github.cjustinn.specialisedworkforce2.commands.handlers;

import io.github.cjustinn.specialisedworkforce2.enums.CustomInventoryType;
import io.github.cjustinn.specialisedworkforce2.services.CustomInventoryService;
import io.github.cjustinn.specialisedworkforce2.services.LoggingService;
import io.github.cjustinn.specialisedworkforce2.services.WorkforceService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class WorkforceCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length == 0 || ((args.length == 1 || args.length == 2) && args[0].equals("join"))) {
                final @Nullable String target = args.length == 2 ? args[1] : null;
                if (target != null && !WorkforceService.professions.stream().anyMatch((prof) -> prof.group.equalsIgnoreCase(target))) {
                    player.sendMessage(Component.text("That is not a profession group!").color(NamedTextColor.RED));
                } else {
                    Inventory inv = CustomInventoryService.BuildCustomInventory(CustomInventoryType.JOIN, player.getUniqueId().toString(), new HashMap<String, Object>() {{
                        if (target != null) {
                            put("group", target);
                        }
                    }});

                    if (inv != null) {
                        player.openInventory(inv);
                    }
                }
            } else if (args.length == 1 && args[0].equals("status")) {
                if (WorkforceService.GetActiveUserProfessions(player.getUniqueId().toString()).size() < 1) {
                    player.sendMessage(Component.text("You are not a member of any professions!").color(NamedTextColor.RED));
                } else {
                    Inventory inv = CustomInventoryService.BuildCustomInventory(CustomInventoryType.STATUS, player.getUniqueId().toString());
                    if (inv != null) {
                        player.openInventory(inv);
                    }
                }
            } else if ((args.length == 1 || args.length == 2) && args[0].equals("leaderboard")) {
                final @Nullable String target = args.length == 2 ? args[1] : null;
                if (target != null && WorkforceService.GetProfessionById(target) == null) {
                    player.sendMessage(Component.text("That is not a valid job!").color(NamedTextColor.RED));
                } else {
                    Inventory inv = CustomInventoryService.BuildCustomInventory(CustomInventoryType.LEADERBOARD, player.getUniqueId().toString(), new HashMap<String, Object>() {{
                        if (target != null) {
                            put("job", target);
                        }
                    }});

                    if (inv != null) {
                        player.openInventory(inv);
                    }
                }
            } else {
                player.sendMessage(Component.text("That is not a valid workforce sub-command!").color(NamedTextColor.RED));
            }
        } else {
            LoggingService.WriteWarning("The /workforce command cannot be run via console!");
            return false;
        }

        return true;
    }
}
