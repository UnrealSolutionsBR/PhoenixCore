package com.phoenixcore.farms;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TrigoListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();

        if (block.getType() == Material.HAY_BLOCK) {
            Block above = block.getRelative(0, 1, 0);

            // Solo si el bloque arriba está vacío
            if (above.getType() == Material.AIR) {
                // Ejecutar un tick después para que no haya conflicto con la colocación
                Bukkit.getScheduler().runTaskLater(
                        Bukkit.getPluginManager().getPlugin("PhoenixPrisonCore"),
                        () -> {
                            above.setType(Material.WHEAT);

                            // Forzar el wheat a crecer en su estado máximo
                            if (above.getBlockData() instanceof Ageable ageable) {
                                ageable.setAge(ageable.getMaximumAge());
                                above.setBlockData(ageable);
                            }
                        },
                        1L // esperar 1 tick
                );

                player.sendMessage("§eHas colocado una §6Granja de Trigo§e, el trigo crecerá encima.");
            } else {
                player.sendMessage("§cNecesitas un bloque libre arriba del heno para la granja.");
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.WHEAT) {
            // Dropear diamante custom
            ItemStack trigoItem = new ItemStack(Material.DIAMOND, 1);
            ItemMeta meta = trigoItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6Trigo x1");
                trigoItem.setItemMeta(meta);
            }

            block.getWorld().dropItemNaturally(block.getLocation(), trigoItem);

            // Evitar drop vanilla
            event.setDropItems(false);

            // Guardar referencia al bloque roto
            Block brokenBlock = block;

            // Programar regeneración en 5 segundos (100 ticks)
            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugin("PhoenixPrisonCore"),
                    () -> {
                        brokenBlock.setType(Material.WHEAT);
                        if (brokenBlock.getBlockData() instanceof Ageable ageable) {
                            ageable.setAge(ageable.getMaximumAge());
                            brokenBlock.setBlockData(ageable);
                        }
                    },
                    100L // 5 segundos
            );
        }
    }
}
