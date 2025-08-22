package com.phoenixcore.pickaxes.listeners;

import com.phoenixcore.PhoenixPrisonCore;
import com.phoenixcore.pickaxes.PickaxeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.HashMap;
import java.util.UUID;

public class DropListener implements Listener {

    // Guardar la última vez que un jugador intentó tirar el pico
    private final HashMap<UUID, Long> lastDropAttempt = new HashMap<>();

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        // Solo aplicar a picos custom
        if (!PickaxeManager.isCustomPickaxe(event.getItemDrop().getItemStack())) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Tiempo de confirmación desde config.yml (por defecto 3 segundos)
        int confirmSeconds = PhoenixPrisonCore.getInstance()
                .getConfig()
                .getInt("drop-confirm-seconds", 3);

        // Si es el primer intento o ya pasó el tiempo de confirmación
        if (!lastDropAttempt.containsKey(uuid) || (now - lastDropAttempt.get(uuid)) > confirmSeconds * 1000L) {
            event.setCancelled(true); // cancelar el dropeo
            lastDropAttempt.put(uuid, now);

            // Mostrar título y mensaje de advertencia
            player.sendTitle(
                    "§4[!] §cCAREFUL",
                    "§7Presiona §cQ §7otra vez para soltar tu pico",
                    10, 40, 10
            );
            player.sendMessage("§4[!] §7Presiona §cQ §7otra vez para soltar tu pico!");
        } else {
            // Segunda vez dentro del tiempo → permitir soltar
            lastDropAttempt.remove(uuid);
            player.sendMessage("§cHas soltado tu pico.");
        }
    }
}
