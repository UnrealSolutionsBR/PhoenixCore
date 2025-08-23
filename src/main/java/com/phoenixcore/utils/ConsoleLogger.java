package com.phoenixcore.utils;

import java.util.logging.Logger;

public class ConsoleLogger {

    private static final Logger CONSOLE = Logger.getLogger("Minecraft");

    private static void out(String msg) {
        CONSOLE.info(msg); // mantiene timestamp + INFO pero sin [PhoenixPrisonCore]
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
        out(name + "...");
    }

    public static void success(String name, long tookMs) {
        out( name + " §7(took " + tookMs + "ms)");
    }

    public static void warn(String message) {
        CONSOLE.warning(message);
    }

    public static void error(String message) {
        CONSOLE.severe(message);
    }

    public static void done() {
        out("§aPhoenixPrisonCore habilitado correctamente.");
        out("---------------------------------------------------");
    }
}
