package com.phoenixcore.pickaxes;
import com.phoenixcore.PhoenixPrisonCore;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;
public class SkinManager {
    private static final Map<String, SkinData> skins = new HashMap<>();
    private static File file;
    private static FileConfiguration config;
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
                double bonus = config.getDouble("skins." + key + ".bonus", 1.0); // ahora double
                String materialStr = config.getString("skins." + key + ".material", "WOODEN_PICKAXE");

                Material material;
                try {
                    material = Material.matchMaterial(materialStr.toUpperCase());
                } catch (Exception e) {
                    material = Material.WOODEN_PICKAXE;
                }

                skins.put(key.toLowerCase(), new SkinData(key.toLowerCase(), display, bonus, material));
            }
        }

        PhoenixPrisonCore.getInstance().getLogger().info("§e[Pickaxes] Loaded " + skins.size() + " skins from skins.yml");
    }
    public static SkinData getSkin(String id) {
        return skins.getOrDefault(id.toLowerCase(), new SkinData("wooden", "Wooden", 1.0, Material.WOODEN_PICKAXE));
    }
    public static List<String> getAllSkinIds() {
        return new ArrayList<>(skins.keySet());
    }
    public record SkinData(String id, String display, double bonus, Material material) {}
}
