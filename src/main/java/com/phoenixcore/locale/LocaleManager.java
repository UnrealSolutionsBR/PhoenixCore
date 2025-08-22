package com.phoenixcore.locale;

import com.phoenixcore.PhoenixPrisonCore;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class LocaleManager {

    private static FileConfiguration localeConfig;

    /**
     * Carga el archivo de idioma configurado en config.yml
     */
    public static void loadLocale() {
        String lang = PhoenixPrisonCore.getInstance().getConfig().getString("Language", "EN");
        File localeFile = new File(PhoenixPrisonCore.getInstance().getDataFolder(), "locales/" + lang + ".yml");

        if (!localeFile.exists()) {
            PhoenixPrisonCore.getInstance().saveResource("locales/" + lang + ".yml", false);
        }

        localeConfig = YamlConfiguration.loadConfiguration(localeFile);
        PhoenixPrisonCore.getInstance().getLogger().info("§e[Locale] Loaded locale: " + lang);
    }

    /**
     * Devuelve un mensaje traducido con el prefijo configurado
     * @param path ruta dentro del archivo de idioma (ej: "messages.reload")
     */
    public static String getMessage(String path) {
        if (localeConfig == null) {
            return ChatColor.RED + "Locale not loaded!";
        }

        String prefix = localeConfig.getString("prefix", "&7[PhoenixCore] ");
        String message = localeConfig.getString("messages." + path, path);

        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    /**
     * Devuelve un mensaje sin prefijo (útil para títulos o actionbars)
     */
    public static String getRawMessage(String path) {
        if (localeConfig == null) {
            return ChatColor.RED + "Locale not loaded!";
        }

        String message = localeConfig.getString("messages." + path, path);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
