package io.github.cjustinn.specialisedworkforce2.services;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class EconomyService {
    private static Economy economy;
    public static boolean economyIntegrationEnabled = false;

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
        return EvaluationService.evaluate(equation.replace("{level}", String.valueOf(level)));
    }
}