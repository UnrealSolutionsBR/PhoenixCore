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

        String cmd = command.getName();

        // ───────────────
        // phoenixcore
        // ───────────────
        if (cmd.equalsIgnoreCase("phoenixcore")) {
            if (args.length == 1) {
                if (sender.hasPermission("phoenixcore.reload")) {
                    suggestions.add("reload");
                }
                if (sender.hasPermission("phoenixcore.setskin")) {
                    suggestions.add("setskin");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("setskin")) {
                if (sender.hasPermission("phoenixcore.setskin")) {
                    // Autocompletar skins disponibles
                    suggestions.addAll(SkinManager.getAllSkinIds());
                }
            }
        }

        // ───────────────
        // farms
        // ───────────────
        if (cmd.equalsIgnoreCase("farms")) {
            if (args.length == 1) {
                if (sender.hasPermission("phoenixcore.farms.give")) {
                    // Primera sugerencia = tipos de farms
                    suggestions.addAll(FarmsConfig.get().allFarms()
                            .stream().map(FarmsConfig.FarmDef::type).toList());
                }
            } else if (args.length == 2) {
                // Cantidad sugerida
                suggestions.add("1");
                suggestions.add("16");
                suggestions.add("64");
            }
        }

        return suggestions;
    }
}
