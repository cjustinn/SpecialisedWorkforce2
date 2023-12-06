package io.github.cjustinn.specialisedworkforce2.services;

import io.github.cjustinn.specialisedlib.Logging.LoggingService;
import io.github.cjustinn.specialisedworkforce2.enums.WorkforceRewardType;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public class EconomyService {
    public static String fallbackCurrencySymbol = "$";
    private static Economy economy;
    public static boolean economyIntegrationEnabled = false;
    public static boolean enableSpecialisedEconomicsFeatures = false;

    public static Economy getEconomy() { return economy; }
    public static void SetEconomy(Economy economy) {
        EconomyService.economy = economy;
    }

    public static void ModifyFunds(Player player, final double amount) {
        if (amount > 0)
            economy.depositPlayer(player, amount);
        else
            economy.withdrawPlayer(player, Math.abs(amount));
    }

    public static double CalculateMonetaryReward(final String equation, final int level) {
        return EvaluationService.evaluate(
                EvaluationService.populateEquation(
                        equation,
                        new HashMap<String, Object>() {{ put("level", level); }}
                )
        );
    }

    public static void RewardPlayer(String recipient, double amount, String cause) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(recipient));
        if (offlinePlayer.isOnline()) {
            ModifyFunds(offlinePlayer.getPlayer(), amount);
        } else {
            if (!AttributeLoggingService.BacklogReward(WorkforceRewardType.ECONOMIC, recipient, amount, cause)) {
                LoggingService.writeLog(
                        Level.SEVERE,
                        String.format(
                                "Unable to backlog economic payment to user %s: %s from %s profession.",
                                recipient,
                                EconomyService.economy.format(amount),
                                cause
                        )
                );
            }
        }
    }
}