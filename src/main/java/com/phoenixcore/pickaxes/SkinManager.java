package com.phoenixcore.pickaxes;

import com.phoenixcore.PhoenixPrisonCore;
import com.phoenixcore.utils.ConsoleLogger; // ← importar
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class SkinManager {

    private static final Map<String, SkinData> skins = new HashMap<>();
    private static File file;
    private static FileConfiguration config;

    /**
     * Load skins from pickaxes/skins.yml
     */
    public static void loadSkins() {
        file = new File(PhoenixPrisonCore.getInstance().getDataFolder(), "pickaxes/skins.yml");

        if (!file.exists()) {
            PhoenixPrisonCore.getInstance().saveResource("pickaxes/skins.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
        skins.clear();

        if (config.isConfigurationSection("skins")) {
            for (String key : config.getConfigurationSection("skins").getKeys(false)) {
                String display = config.getString("skins." + key + ".display", key);
                double bonus = config.getDouble("skins." + key + ".bonus", 1.0); // multiplier
                String materialStr = config.getString("skins." + key + ".material", "WOODEN_PICKAXE");

                Material material;
                try {
                    material = Material.matchMaterial(materialStr.toUpperCase());
                    if (material == null) {
                        // SIN prefijos ni Logger del plugin
                        ConsoleLogger.warn("Invalid material for skin: " + key + " (" + materialStr + ")");
                        material = Material.WOODEN_PICKAXE;
                    }
                } catch (Exception e) {
                    material = Material.WOODEN_PICKAXE;
                }

                skins.put(key.toLowerCase(), new SkinData(key.toLowerCase(), display, bonus, material));
            }
        }

        // Mensaje limpio, sin [PhoenixPrisonCore] ni [Pickaxes]
        ConsoleLogger.info("Loaded " + skins.size() + " skins from skins.yml");
    }

    /** Get skin by ID */
    public static SkinData getSkin(String id) {
        return skins.getOrDefault(id.toLowerCase(),
                new SkinData("wooden", "Wooden", 1.0, Material.WOODEN_PICKAXE));
    }

    /** Get all available skin IDs */
    public static List<String> getAllSkinIds() {
        return new ArrayList<>(skins.keySet());
    }

    /** Skin data record */
    public record SkinData(String id, String display, double bonus, Material material) {}
}
