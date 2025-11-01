package me.sanepe.aerochat.commands;

import me.sanepe.aerochat.PaperBasePlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClearChatCommand implements CommandExecutor, TabCompleter {

    private static final Pattern AMP_HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private final PaperBasePlugin plugin;
    private final boolean papiEnabled;
    private final LegacyComponentSerializer legacy;

    public ClearChatCommand(PaperBasePlugin plugin) {
        this.plugin = plugin;
        this.papiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        this.legacy = LegacyComponentSerializer.builder()
                .character('&')
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aerochat.clearchat")) {
            sender.sendMessage("§cYou don't have permission for this.");
            return true;
        }

        FileConfiguration cfg = plugin.getConfig();
        int defaultLines = Math.max(10, cfg.getInt("clearchat.lines", 100));
        int lines = defaultLines;
        if (args.length >= 1) {
            try {
                lines = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }
        lines = Math.max(10, Math.min(lines, 500));

        // Push blank lines for all players
        Component blank = Component.text(" ");
        for (int i = 0; i < lines; i++) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(blank);
            }
        }

        // Broadcast message
        if (cfg.getBoolean("clearchat.broadcast.enabled", true)) {
            String msg = cfg.getString("clearchat.broadcast.message", "&7Chat was cleared by &e{sender}");
            msg = msg.replace("{sender}", sender.getName());
            if (papiEnabled && sender instanceof Player) {
                try { msg = PlaceholderAPI.setPlaceholders((Player) sender, msg); } catch (Throwable ignored) {}
            }
            msg = translateAmpersandHex(msg);
            Component comp = legacy.deserialize(msg);
            Bukkit.getServer().broadcast(comp);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> sug = new ArrayList<>();
            Collections.addAll(sug, "50", "100", "150", "200");
            return sug;
        }
        return Collections.emptyList();
    }

    private static String translateAmpersandHex(String input) {
        if (input == null || input.isEmpty()) return input;
        Matcher m = AMP_HEX_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            StringBuilder rep = new StringBuilder("&x");
            for (char c : hex.toCharArray()) {
                rep.append('&').append(c);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(rep.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
