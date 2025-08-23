package com.phoenixcore.pickaxes;

import com.phoenixcore.PhoenixPrisonCore;
import com.phoenixcore.utils.ConsoleLogger;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class BlockValueManager {

    private static final Map<Material, Double> values = new HashMap<>();
    private static File file;
    private static FileConfiguration config;

    /**
     * Cargar valores desde pickaxes/blocks.yml
     */
    public static void loadValues() {
        file = new File(PhoenixPrisonCore.getInstance().getDataFolder(), "pickaxes/blocks.yml");

        if (!file.exists()) {
            PhoenixPrisonCore.getInstance().saveResource("pickaxes/blocks.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
        values.clear();

        if (config.isConfigurationSection("blocks")) {
            for (String key : config.getConfigurationSection("blocks").getKeys(false)) {
                Material mat = Material.matchMaterial(key.toUpperCase());
                double value = config.getDouble("blocks." + key, 1.0);

                if (mat != null) {
                    values.put(mat, value);
                } else {
                    ConsoleLogger.warn("Invalid block in blocks.yml: " + key);
                }
            }
        }

        // Mensaje limpio sin prefijos
        ConsoleLogger.info("Loaded " + values.size() + " blocks from blocks.yml");
    }

    /**
     * Obtener el valor de XP base de un bloque
     */
    public static double getValue(Material material) {
        return values.getOrDefault(material, 1.0); // Si no está configurado → vale 1
    }
}
