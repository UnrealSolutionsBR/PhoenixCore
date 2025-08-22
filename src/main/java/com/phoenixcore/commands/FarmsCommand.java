package com.phoenixcore.commands;

import com.phoenixcore.farms.FarmsConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FarmsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used in-game.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§eUsage: /" + label + " <farmType> [amount]");
            player.sendMessage("§7Available farms: §f" + String.join(", ",
                    FarmsConfig.get().allFarms().stream().map(FarmsConfig.FarmDef::type).toList()));
            return true;
        }

        String farmType = args[0].toLowerCase();
        FarmsConfig.FarmDef def = FarmsConfig.get().getFarm(farmType);
        if (def == null) {
            player.sendMessage("§cUnknown farm type: §f" + farmType);
            player.sendMessage("§7Available: §f" + String.join(", ",
                    FarmsConfig.get().allFarms().stream().map(FarmsConfig.FarmDef::type).toList()));
            return true;
        }

        int amount = 1; // default
        if (args.length >= 2) {
            try {
                amount = Math.min(Integer.parseInt(args[1]), FarmsConfig.get().getStackLimit());
                if (amount <= 0) amount = 1;
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount. Using 1.");
            }
        }

        ItemStack farmItem = FarmsConfig.get().buildFarmItem(farmType, amount);
        if (farmItem == null) {
            player.sendMessage("§cError creating farm item for type: " + farmType);
            return true;
        }

        player.getInventory().addItem(farmItem);
        player.sendMessage("§aYou received §f" + amount + " §e" + def.itemName() + "§a!");
        return true;
    }
}
