package com.phoenixcore.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FarmsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando solo puede usarse en el juego.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§eUso: /" + label + " <trigo>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "trigo" -> {
                ItemStack hayBlock = new ItemStack(Material.HAY_BLOCK, 1);
                player.getInventory().addItem(hayBlock);
                player.sendMessage("§aSe te ha entregado un bloque de heno para tu granja de trigo.");
            }
            default -> {
                player.sendMessage("§cGranja desconocida: " + subCommand);
                player.sendMessage("§7Disponibles: §ftrigo");
            }
        }

        return true;
    }
}
