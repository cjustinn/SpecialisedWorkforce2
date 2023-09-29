package io.github.cjustinn.specialisedworkforce2.services;

import org.bukkit.Bukkit;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingService {
    private static final Logger logger = Logger.getLogger("Minecraft");
    public static void WriteMessage(String content) {
        String name = Bukkit.getPluginManager().getPlugin("SpecialisedWorkforce2").getName();

        LoggingService.logger.log(
                Level.INFO,
                String.format("[%s] %s", name, content)
        );
    }

    public static void WriteError(String content) {
        String name = Bukkit.getPluginManager().getPlugin("SpecialisedWorkforce2").getName();

        LoggingService.logger.log(
                Level.SEVERE,
                String.format("[%s] %s", name, content)
        );
    }

    public static void WriteWarning(String content) {
        String name = Bukkit.getPluginManager().getPlugin("SpecialisedWorkforce2").getName();

        LoggingService.logger.log(
                Level.WARNING,
                String.format("[%s] %s", name, content)
        );
    }
}
