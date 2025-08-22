package com.phoenixcore;

import com.phoenixcore.pickaxes.SkinManager;
import com.phoenixcore.pickaxes.listeners.BlockBreakListener;
import com.phoenixcore.pickaxes.listeners.DropListener;
import com.phoenixcore.pickaxes.BlockValueManager;
import com.phoenixcore.commands.CoreCommand;
import com.phoenixcore.commands.CoreTabCompleter;
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

        // Guardar config.yml por defecto
        saveDefaultConfig();

        // Crear carpetas de módulos si no existen
        createModuleFolder("pickaxes");
        createModuleFolder("economy");
        createModuleFolder("mines");

        // Cargar módulos iniciales
        loadPickaxesModule();
        // (futuro) loadEconomyModule();
        // (futuro) loadMinesModule();

        // Registrar comando principal
        if (getCommand("phoenixcore") != null) {
            getCommand("phoenixcore").setExecutor(new CoreCommand());
            getCommand("phoenixcore").setTabCompleter(new CoreTabCompleter());
        }

        getLogger().info("§aPhoenixPrisonCore habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        getLogger().info("§cPhoenixPrisonCore deshabilitado.");
    }

    // ─────────────────────────────
    //   Métodos de carga de módulos
    // ─────────────────────────────

    private void loadPickaxesModule() {
        // Registrar listeners de pickaxes
        getServer().getPluginManager().registerEvents(new BlockBreakListener(), this);
        getServer().getPluginManager().registerEvents(new DropListener(), this);

        // Cargar configuraciones
        SkinManager.loadSkins();
        BlockValueManager.loadValues();

        getLogger().info("§e[Módulo] Pickaxes cargado correctamente.");
    }

    // Aquí después puedes añadir loadEconomyModule() y loadMinesModule()

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
