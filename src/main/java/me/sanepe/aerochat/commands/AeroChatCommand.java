package me.sanepe.aerochat.commands;

import me.sanepe.aerochat.PaperBasePlugin;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class AeroChatCommand implements CommandExecutor, TabCompleter {

    private final PaperBasePlugin plugin;

    public AeroChatCommand(PaperBasePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            String ver = plugin.getDescription().getVersion();
            sender.sendMessage("§aAeroChat §ev" + ver + " §7running");
            sender.sendMessage("§7Use §e/" + label + " reload §7to reload the configuration.");
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("aerochat.reload")) {
                sender.sendMessage("§cYou don't have permission for this.");
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage("§aAeroChat: configuration reloaded.");
            return true;
        }
        sender.sendMessage("§cUsage: /" + label + " reload");
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            if ("reload".startsWith(args[0].toLowerCase())) {
                return java.util.Collections.singletonList("reload");
            }
        }
        return Collections.emptyList();
    }
}
