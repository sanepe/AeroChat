package me.sanepe.aerochat;

import me.sanepe.aerochat.commands.AeroChatCommand;
import me.sanepe.aerochat.commands.ClearChatCommand;
import me.sanepe.aerochat.listeners.JoinListener;
import me.sanepe.aerochat.listeners.ChatListener;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperBasePlugin extends JavaPlugin {

    private static PaperBasePlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        // Friendly colored startup banner (console supports colors); avoid emojis for compatibility
        final boolean papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        final boolean lp = Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
        final String version = getDescription().getVersion();

        ConsoleCommandSender console = Bukkit.getConsoleSender();
        Component header = Component.text()
            .append(Component.text("[", NamedTextColor.DARK_GRAY))
            .append(Component.text("AeroChat", NamedTextColor.AQUA, TextDecoration.BOLD))
            .append(Component.text(" v" + version, NamedTextColor.GOLD))
            .append(Component.text("]", NamedTextColor.DARK_GRAY))
            .build();

        console.sendMessage(header);
        console.sendMessage(Component.text("- Running", NamedTextColor.GREEN));
        console.sendMessage(Component.text("- PlaceholderAPI: ", NamedTextColor.GRAY)
            .append(Component.text(papi ? "OK" : "missing", papi ? NamedTextColor.GREEN : NamedTextColor.RED)));
        console.sendMessage(Component.text("- LuckPerms: ", NamedTextColor.GRAY)
            .append(Component.text(lp ? "OK" : "missing", lp ? NamedTextColor.GREEN : NamedTextColor.RED)));

        // Ensure default config
        saveDefaultConfig();

        // Register commands
        if (getCommand("aerochat") != null) {
            AeroChatCommand ac = new AeroChatCommand(this);
            getCommand("aerochat").setExecutor(ac);
            getCommand("aerochat").setTabCompleter(ac);
        } else {
            getLogger().warning("Command 'aerochat' not found in plugin.yml");
        }

        if (getCommand("clearchat") != null) {
            ClearChatCommand cc = new ClearChatCommand(this);
            getCommand("clearchat").setExecutor(cc);
            getCommand("clearchat").setTabCompleter(cc);
        } else {
            getLogger().warning("Command 'clearchat' not found in plugin.yml");
        }

        // Register listeners
        getServer().getPluginManager().registerEvents(new JoinListener(), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("AeroChat disabled.");
    }

    public static PaperBasePlugin getInstance() {
        return instance;
    }
}
