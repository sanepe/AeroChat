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
    private java.util.Set<java.util.UUID> knownPlayers = new java.util.HashSet<>();
    private org.bukkit.configuration.file.FileConfiguration playersData;
    private java.io.File playersFile;

    @Override
    public void onEnable() {
        instance = this;
        // Friendly colored startup banner (console supports colors); avoid emojis for compatibility
        final boolean papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        final boolean lp = Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
    final String version = getPluginMeta().getVersion();

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

    // Load players registry
    loadPlayersRegistry();

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
        savePlayersRegistry();
        instance = null;
    }

    public static PaperBasePlugin getInstance() {
        return instance;
    }

    private void loadPlayersRegistry() {
        try {
            if (!getDataFolder().exists()) getDataFolder().mkdirs();
            playersFile = new java.io.File(getDataFolder(), "players.yml");
            if (!playersFile.exists()) {
                playersFile.createNewFile();
            }
            playersData = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(playersFile);
            java.util.List<String> list = playersData.getStringList("players");
            for (String s : list) {
                try { knownPlayers.add(java.util.UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
            }
        } catch (Exception ex) {
            getLogger().warning("Failed to load players registry: " + ex.getMessage());
        }
    }

    private void savePlayersRegistry() {
        if (playersData == null) return;
        try {
            java.util.List<String> out = new java.util.ArrayList<>();
            for (java.util.UUID u : knownPlayers) out.add(u.toString());
            playersData.set("players", out);
            playersData.save(playersFile);
        } catch (Exception ex) {
            getLogger().warning("Failed to save players registry: " + ex.getMessage());
        }
    }

    /**
     * Register a player's UUID if first time, returning the total count of unique players seen by the plugin.
     */
    public int registerAndCount(java.util.UUID uuid) {
        if (!knownPlayers.contains(uuid)) {
            knownPlayers.add(uuid);
            savePlayersRegistry();
        }
        return knownPlayers.size();
    }
}
