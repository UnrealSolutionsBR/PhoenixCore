package com.phoenixcore.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class FarmsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used in-game.");
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("wheat")) {
            player.sendMessage("§eUsage: /" + label + " wheat [amount]");
            return true;
        }

        int amount = 1; // por defecto 1
        if (args.length >= 2) {
            try {
                amount = Math.min(Integer.parseInt(args[1]), 64); // limitar a 64 por stack
                if (amount <= 0) amount = 1;
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount. Using 1.");
            }
        }

        ItemStack farmItem = new ItemStack(Material.HAY_BLOCK, amount);
        ItemMeta meta = farmItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eWheat Farm");
            meta.setLore(List.of(
                    "§7Place this block on the ground",
                    "§7to create a wheat farm"
            ));
            farmItem.setItemMeta(meta);
        }

        player.getInventory().addItem(farmItem);
        player.sendMessage("§aYou received §f" + amount + " §eWheat Farm(s)§a!");
        return true;
    }
}
