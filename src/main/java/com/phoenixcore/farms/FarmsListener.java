package com.phoenixcore.farms;

import com.phoenixcore.PhoenixPrisonCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class FarmsListener implements Listener {

    // ─────────────────────────────
    // Colocar farm
    // ─────────────────────────────
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();

        if (block.getType() == Material.HAY_BLOCK) {
            Block above = block.getRelative(0, 1, 0);

            if (above.getType() == Material.AIR) {
                // Colocar trigo un tick después para evitar problemas de física
                Bukkit.getScheduler().runTaskLater(PhoenixPrisonCore.getInstance(), () -> {
                    above.setType(Material.WHEAT);

                    if (above.getBlockData() instanceof Ageable ageable) {
                        ageable.setAge(ageable.getMaximumAge()); // crecer al máximo
                        above.setBlockData(ageable);
                    }
                }, 100L);

                player.sendMessage("§eYou placed a §6Wheat Farm§e!");
            } else {
                player.sendMessage("§cYou need an empty block above to place the farm.");
            }
        }
    }

    // ─────────────────────────────
    // Romper trigo
    // ─────────────────────────────
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // Romper trigo
        if (block.getType() == Material.WHEAT) {
            Player player = event.getPlayer();

            // Crear el item custom "Wheat x1"
            ItemStack reward = new ItemStack(Material.DIAMOND, 1);
            ItemMeta meta = reward.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6Wheat x1");
                reward.setItemMeta(meta);
            }

            // Verificar espacio en inventario
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(reward);
            } else {
                player.sendMessage("§cYour inventory is full! You cannot harvest until you make space.");
                event.setCancelled(true);
                return;
            }

            // Evitar drop vanilla
            event.setDropItems(false);

            // Guardar referencia del hay bale debajo
            Block hayBlock = block.getRelative(0, -1, 0);

            // Regenerar el trigo en 5 segundos solo si el hay bale sigue ahí
            Block wheatBlock = block;
            Bukkit.getScheduler().runTaskLater(PhoenixPrisonCore.getInstance(), () -> {
                if (hayBlock.getType() == Material.HAY_BLOCK && wheatBlock.getType() == Material.AIR) {
                    wheatBlock.setType(Material.WHEAT);
                    if (wheatBlock.getBlockData() instanceof Ageable ageable) {
                        ageable.setAge(ageable.getMaximumAge());
                        wheatBlock.setBlockData(ageable);
                    }
                }
            }, 100L);
        }

        // Romper hay bale (farm)
        if (block.getType() == Material.HAY_BLOCK) {
            Player player = event.getPlayer();

            // Evitar drops vanilla
            event.setDropItems(false);

            // Eliminar trigo de arriba si existe
            Block above = block.getRelative(0, 1, 0);
            if (above.getType() == Material.WHEAT) {
                above.setType(Material.AIR);
            }

            // Dar al jugador la Wheat Farm item
            ItemStack farmItem = new ItemStack(Material.HAY_BLOCK, 1);
            ItemMeta meta = farmItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§eWheat Farm");
                meta.setLore(List.of(
                        "§7Place this block on the ground",
                        "§7to create a wheat farm"
                ));
                farmItem.setItemMeta(meta);
            }

            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(farmItem);
            } else {
                player.sendMessage("§cYour inventory is full! You cannot break this farm until you make space.");
                event.setCancelled(true); // bloquear el break si no hay espacio
            }
        }
    }

    // ─────────────────────────────
    // Evitar que wheat se rompa por física al poner farms lado a lado
    // ─────────────────────────────
    @EventHandler
    public void onPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.WHEAT) {
            Block below = block.getRelative(0, -1, 0);
            if (below.getType() == Material.HAY_BLOCK) {
                event.setCancelled(true);
            }
        }
    }
}
