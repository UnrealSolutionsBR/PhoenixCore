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
import org.bukkit.plugin.java.JavaPlugin;
import com.phoenixcore.economy.EconomyHook;

import java.io.File;

public class PhoenixPrisonCore extends JavaPlugin {

    private static PhoenixPrisonCore instance;

    public static PhoenixPrisonCore getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Guardar config.yml por defecto
        saveDefaultConfig();
        LocaleManager.loadLocale();

        // Economy (Vault) - opcional
        EconomyHook.init();

        // Crear carpetas de módulos si no existen
        createModuleFolder("pickaxes");
        createModuleFolder("economy");
        createModuleFolder("mines");
        createModuleFolder("farms");

        // 🔹 Cargar farms persistentes
        getServer().getWorlds().forEach(FarmsManager::reloadWorld);

        // 🔹 Cargar configuración de tipos de farms (definiciones)
        FarmsConfig.get(); // fuerza la carga inicial de farms/farms.yml

        // Cargar módulos iniciales
        loadPickaxesModule();
        // (futuro) loadEconomyModule();
        // (futuro) loadMinesModule();

        // Registrar comando principal
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

        getLogger().info("§aPhoenixPrisonCore habilitado correctamente.");
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

        getLogger().info("§e[Módulo] Pickaxes cargado correctamente.");
    }

    // ─────────────────────────────
    //   Utilidad
    // ─────────────────────────────

    private void createModuleFolder(String name) {
        File folder = new File(getDataFolder(), name);
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (created) {
                getLogger().info("§7Carpeta creada: " + name);
            }
        }
    }
}
