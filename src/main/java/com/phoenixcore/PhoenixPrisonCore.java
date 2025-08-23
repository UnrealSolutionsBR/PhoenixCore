package com.phoenixcore;

import com.phoenixcore.pickaxes.SkinManager;
import com.phoenixcore.pickaxes.listeners.BlockBreakListener;
import com.phoenixcore.pickaxes.listeners.DropListener;
import com.phoenixcore.pickaxes.BlockValueManager;
import com.phoenixcore.farms.FarmsConfig;
import com.phoenixcore.commands.CoreCommand;
import com.phoenixcore.commands.FarmsCommand;
import com.phoenixcore.commands.CoreTabCompleter;
import com.phoenixcore.farms.FarmsListener;
import com.phoenixcore.locale.LocaleManager;
import com.phoenixcore.farms.FarmsManager;
import com.phoenixcore.economy.EconomyHook;
import com.phoenixcore.utils.ConsoleLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class PhoenixPrisonCore extends JavaPlugin {

    private static PhoenixPrisonCore instance;

    public static PhoenixPrisonCore getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        long start = System.currentTimeMillis();
        ConsoleLogger.bannerStart();

        // Config y locales
        long t0 = System.currentTimeMillis();
        saveDefaultConfig();
        LocaleManager.loadLocale();
        ConsoleLogger.success("Locales", System.currentTimeMillis() - t0);

        // Economy (Vault) - opcional
        t0 = System.currentTimeMillis();
        EconomyHook.init();
        ConsoleLogger.success("Economy (Vault Hook)", System.currentTimeMillis() - t0);

        // Crear carpetas de módulos
        t0 = System.currentTimeMillis();
        createModuleFolder("pickaxes");
        createModuleFolder("economy");
        createModuleFolder("mines");
        createModuleFolder("farms");
        ConsoleLogger.success("Folders", System.currentTimeMillis() - t0);

        // Farms persistentes + config
        t0 = System.currentTimeMillis();
        getServer().getWorlds().forEach(FarmsManager::reloadWorld);
        FarmsConfig.get();
        ConsoleLogger.success("Farms", System.currentTimeMillis() - t0);

        // Pickaxes
        t0 = System.currentTimeMillis();
        loadPickaxesModule();
        ConsoleLogger.success("Pickaxes", System.currentTimeMillis() - t0);

        // Registrar comandos
        t0 = System.currentTimeMillis();
        if (getCommand("phoenixcore") != null) {
            getCommand("phoenixcore").setExecutor(new CoreCommand());
            getCommand("phoenixcore").setTabCompleter(new CoreTabCompleter());
        }
        if (getCommand("pickaxe") != null) {
            getCommand("pickaxe").setExecutor(new com.phoenixcore.pickaxes.PickaxeCommand());
        }
        if (getCommand("farms") != null) {
            getCommand("farms").setExecutor(new FarmsCommand());
        }
        ConsoleLogger.success("Commands", System.currentTimeMillis() - t0);

        ConsoleLogger.done();
        getLogger().info("§7Total startup time: §e" + (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    public void onDisable() {
        // 🔹 Guardar farms al apagar
        FarmsManager.saveAllNow();
        getLogger().info("§cPhoenixPrisonCore deshabilitado.");
    }

    // ─────────────────────────────
    //   Métodos de carga de módulos
    // ─────────────────────────────

    private void loadPickaxesModule() {
        // Registrar listeners
        getServer().getPluginManager().registerEvents(new BlockBreakListener(), this);
        getServer().getPluginManager().registerEvents(new DropListener(), this);
        getServer().getPluginManager().registerEvents(new FarmsListener(), this);

        // Cargar configuraciones
        SkinManager.loadSkins();
        BlockValueManager.loadValues();
    }

    // ─────────────────────────────
    //   Utilidad
    // ─────────────────────────────

    private void createModuleFolder(String name) {
        File folder = new File(getDataFolder(), name);
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (created) {
                getLogger().info("Folder created: " + name);
            }
        }
    }
}
