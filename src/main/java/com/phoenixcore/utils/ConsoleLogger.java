package com.phoenixcore.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class ConsoleLogger {

    private static void out(String msg) {
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    public static void bannerStart() {
        out("---------------------------------------------------");
        out("██████╗  ██████╗ ██████╗ ██████╗ ███████╗");
        out("██╔══██╗██╔════╝██╔═══██╗██╔══██╗██╔════╝");
        out("██████╔╝██║     ██║   ██║██████╔╝█████╗  ");
        out("██╔═══╝ ██║     ██║   ██║██╔══██╗██╔══╝  ");
        out("██║     ╚██████╗╚██████╔╝██║  ██║███████╗");
        out("╚═╝      ╚═════╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝");
        out("---------------------------------------------------");
    }

    public static void section(String name) {
        out(ChatColor.AQUA + name + ChatColor.GRAY + "...");
    }

    public static void info(String message) {
        out(ChatColor.WHITE + message);
    }

    public static void success(String name, long tookMs) {
        out(ChatColor.GREEN + name + ChatColor.GRAY + " (took " + tookMs + "ms)");
    }

    public static void warn(String message) {
        out(ChatColor.YELLOW + message);
    }

    public static void error(String message) {
        out(ChatColor.RED + message);
    }

    public static void done() {
        out(ChatColor.GREEN + "PhoenixPrisonCore habilitado correctamente.");
        out(ChatColor.DARK_GRAY + "---------------------------------------------------");
    }
}
