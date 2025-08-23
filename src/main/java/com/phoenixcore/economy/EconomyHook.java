package com.phoenixcore.economy;

import com.phoenixcore.PhoenixPrisonCore;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Logger;

/**
 * Enlace ligero a Vault Economy.
 * - Si no existe Vault o no hay un proveedor de Economy, queda desactivado de forma segura.
 */
public final class EconomyHook {

    private static Economy econ;
    private static boolean enabled;
    private static Logger log() { return PhoenixPrisonCore.getInstance().getLogger(); }

    private EconomyHook() {}

    /** Intenta inicializar la economía vía Vault. */
    public static void init() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            enabled = false;
            log().warning("§e[Economy] Vault no encontrado. Economía deshabilitada.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            enabled = false;
            log().warning("§e[Economy] Ningún proveedor de Economy encontrado vía Vault.");
            return;
        }

        econ = rsp.getProvider();
        enabled = (econ != null);
        if (enabled) {
            log().info("§a[Economy] Conectado a Vault con proveedor: " + econ.getName());
        } else {
            log().warning("§e[Economy] No fue posible enlazar con un proveedor de Economy.");
        }
    }

    /** ¿Economía disponible? */
    public static boolean isEnabled() {
        return enabled && econ != null;
    }

    /** Formatea cantidades con el formato del proveedor. */
    public static String format(double amount) {
        return isEnabled() ? econ.format(amount) : String.format("%.2f", amount);
    }

    /** Saldo de un jugador. */
    public static double getBalance(OfflinePlayer player) {
        if (!isEnabled()) return 0D;
        return econ.getBalance(player);
    }

    /** Deposita dinero al jugador. Devuelve true si fue exitoso. */
    public static boolean deposit(OfflinePlayer player, double amount) {
        if (!isEnabled()) return false;
        return econ.depositPlayer(player, amount).transactionSuccess();
    }

    /** Retira dinero del jugador. Devuelve true si fue exitoso. */
    public static boolean withdraw(OfflinePlayer player, double amount) {
        if (!isEnabled()) return false;
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }
}
