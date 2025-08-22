package com.phoenixcore.locale;

import com.phoenixcore.PhoenixPrisonCore;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LocaleManager {

    private static FileConfiguration localeConfig;

    /**
     * Carga el archivo de idioma configurado en config.yml
     */
    public static void loadLocale() {
        String lang = PhoenixPrisonCore.getInstance().getConfig().getString("Language", "en_US");
        File localeFile = new File(PhoenixPrisonCore.getInstance().getDataFolder(), "locales/" + lang + ".yml");

        if (!localeFile.exists()) {
            PhoenixPrisonCore.getInstance().saveResource("locales/" + lang + ".yml", false);
        }

        localeConfig = YamlConfiguration.loadConfiguration(localeFile);
        PhoenixPrisonCore.getInstance().getLogger().info("[Locale] Loaded locale: " + lang);
    }

    /**
     * Devuelve un mensaje traducido con el prefijo configurado
     */
    public static String getMessage(String path) {
        if (localeConfig == null) {
            return ChatColor.RED + "[Locale not loaded]";
        }

        String prefix = localeConfig.getString("prefix", "&7[PhoenixCore] ");
        String message = localeConfig.getString("messages." + path, "[MISSING: " + path + "]");
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    /**
     * Devuelve un mensaje sin prefijo (útil para títulos o actionbars)
     */
    public static String getRawMessage(String path) {
        if (localeConfig == null) {
            return ChatColor.RED + "[Locale not loaded]";
        }

        String message = localeConfig.getString("messages." + path, "[MISSING: " + path + "]");
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Devuelve una lista de mensajes desde el locale (con prefijo).
     * Útil para mensajes multilínea (ej: "core-info")
     */
    public static List<String> getMessageList(String path) {
        if (localeConfig == null) {
            return List.of("[Locale not loaded]");
        }

        List<String> list = localeConfig.getStringList("messages." + path);
        if (list == null || list.isEmpty()) {
            return List.of("[MISSING: " + path + "]");
        }

        String prefix = localeConfig.getString("prefix", "&7[PhoenixCore] ");
        List<String> colored = new ArrayList<>();
        for (String line : list) {
            colored.add(ChatColor.translateAlternateColorCodes('&', prefix + line));
        }
        return colored;
    }
}
