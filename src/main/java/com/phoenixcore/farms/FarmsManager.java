package com.phoenixcore.farms;

import com.phoenixcore.PhoenixPrisonCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Maneja la persistencia de las farms (Wheat Farms) en un archivo farms.yml
 * ubicado en la carpeta de datos del plugin.
 */
public class FarmsManager {

    private static final Map<Location, Integer> farms = new HashMap<>();
    private static File file;
    private static FileConfiguration config;

    /**
     * Carga las farms desde farms.yml a memoria.
     */
    public static void loadFarms() {
        file = new File(PhoenixPrisonCore.getInstance().getDataFolder(), "farms.yml");

        if (!file.exists()) {
            try {
                boolean created = file.createNewFile();
                if (created) {
                    PhoenixPrisonCore.getInstance().getLogger().info("§7Archivo farms.yml creado.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        farms.clear();

        if (config.isConfigurationSection("farms")) {
            for (String key : config.getConfigurationSection("farms").getKeys(false)) {
                String path = "farms." + key;
                int count = config.getInt(path);

                // Parsear clave: world,x,y,z
                String[] parts = key.split(",");
                if (parts.length == 4) {
                    World world = Bukkit.getWorld(parts[0]);
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);

                    if (world != null) {
                        Location loc = new Location(world, x, y, z);
                        farms.put(loc, count);
                    }
                }
            }
        }

        PhoenixPrisonCore.getInstance().getLogger().info("§e[Farms] Cargadas " + farms.size() + " farms desde farms.yml");
    }

    /**
     * Guarda todas las farms en farms.yml
     */
    public static void saveFarms() {
        if (config == null) return;
        config.set("farms", null); // limpiar sección

        for (Map.Entry<Location, Integer> entry : farms.entrySet()) {
            Location loc = entry.getKey();
            String key = loc.getWorld().getName() + "," +
                    loc.getBlockX() + "," +
                    loc.getBlockY() + "," +
                    loc.getBlockZ();
            config.set("farms." + key, entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ────────────────
    // Métodos de uso
    // ────────────────

    /**
     * Registrar/actualizar una farm en una ubicación
     */
    public static void setFarm(Location loc, int count) {
        farms.put(loc, count);
        saveFarms();
    }

    /**
     * Eliminar una farm en una ubicación
     */
    public static void removeFarm(Location loc) {
        farms.remove(loc);
        saveFarms();
    }

    /**
     * Obtener el número de farms en una ubicación
     */
    public static int getFarm(Location loc) {
        return farms.getOrDefault(loc, 1);
    }

    /**
     * Verificar si en una ubicación existe una farm
     */
    public static boolean isFarm(Location loc) {
        return farms.containsKey(loc);
    }
}
