package com.phoenixcore.pickaxes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PickaxeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used in-game.");
            return true;
        }

        // Crear y dar el pico inicial con la skin por defecto
        player.getInventory().addItem(PickaxeManager.createPickaxe(
                player,
                0,      // blocksBroken
                1,      // level
                0.0,    // xp inicial
                0,      // fortune
                0,      // explosive
                "wooden" // skinId por defecto (puede leerse de config más adelante)
        ));

        player.sendMessage("§aYou have received your starter pickaxe!");
        return true;
    }
}
