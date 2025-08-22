package com.phoenixcore.commands;

import com.phoenixcore.farms.FarmsConfig;
import com.phoenixcore.pickaxes.SkinManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class CoreTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        String cmd = command.getName().toLowerCase();

        if (cmd.equals("phoenixcore")) {
            if (args.length == 1) {
                if (sender.hasPermission("phoenixcore.reload")) {
                    suggestions.add("reload");
                }
                if (sender.hasPermission("phoenixcore.setskin")) {
                    suggestions.add("setskin");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("setskin")) {
                // Autocompletar skins disponibles
                suggestions.addAll(SkinManager.getAllSkinIds());
            }
        }

        if (cmd.equals("farms")) {
            if (args.length == 1) {
                // Primera sugerencia = tipos de farms
                suggestions.addAll(FarmsConfig.get().allFarms()
                        .stream().map(FarmsConfig.FarmDef::type).toList());
            }
        }

        return suggestions;
    }
}
