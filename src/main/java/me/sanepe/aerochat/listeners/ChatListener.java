package me.sanepe.aerochat.listeners;

import me.sanepe.aerochat.PaperBasePlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.cacheddata.CachedMetaData;

public class ChatListener implements Listener {

    private final PaperBasePlugin plugin;
    private final boolean papiEnabled;
    private final LegacyComponentSerializer legacy;
    private static final Pattern AMP_HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private LuckPerms luckPerms = null;
    private boolean lpEnabled = false;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public ChatListener(PaperBasePlugin plugin) {
        this.plugin = plugin;
        this.papiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        this.legacy = LegacyComponentSerializer.builder()
                .character('&')
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build();
        try {
            this.luckPerms = LuckPermsProvider.get();
            this.lpEnabled = (this.luckPerms != null);
        } catch (Exception ignored) {
            this.lpEnabled = false;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncChat(AsyncChatEvent event) {
        final Player player = event.getPlayer();
        final FileConfiguration cfg = plugin.getConfig();
        final String playerColor = cfg.getString("player_color", "&f");

        String group = "default";
        if (lpEnabled) {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                String primary = user.getPrimaryGroup();
                if (isValid(primary)) {
                    String cand = primary.toLowerCase();
                    group = cand;
                }
            }
        }
        if (!lpEnabled && papiEnabled) {
            try {
                String primary = PlaceholderAPI.setPlaceholders(player, "%luckperms_primary_group_name%");
                if (isValid(primary)) group = primary.toLowerCase();
            } catch (Throwable ignored) {}
        }

        String prefixStr = cfg.getString("prefix." + group, null);
        if ((prefixStr == null || prefixStr.isBlank()) && lpEnabled) {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                CachedMetaData meta = user.getCachedData().getMetaData();
                if (meta != null && isValid(meta.getPrefix())) {
                    prefixStr = meta.getPrefix();
                }
            }
        }
        if ((prefixStr == null || prefixStr.isBlank())) {
            prefixStr = cfg.getString("prefix.default", "");
        }
        if (papiEnabled && prefixStr != null) {
            try { prefixStr = PlaceholderAPI.setPlaceholders(player, prefixStr); } catch (Throwable ignored) {}
        }

        Component prefix = Component.empty();
        if (prefixStr != null && !prefixStr.isBlank()) {
            prefix = deserializeText(prefixStr, papiEnabled ? player : null);
        }

        final Component finalPrefix = prefix;
        final String templateRaw = cfg.getString("chat-format", "{prefix} &7{player}&7: &f{message}");
        event.renderer((source, displayName, message, viewer) -> renderTemplate(templateRaw, finalPrefix, displayName, message, papiEnabled ? player : null, player, playerColor));
    }

    private Component renderTemplate(String template, Component prefix, Component playerName, Component message, Player papiPlayer, Player rawPlayer, String playerColor) {
        if (template == null) template = "{prefix} {player}: {message}";
        Pattern p = Pattern.compile("(\\{prefix\\}|\\{player\\}|\\{message\\})");
        Matcher m = p.matcher(template);
        int last = 0;
        Component result = Component.empty();
        while (m.find()) {
            String text = template.substring(last, m.start());
            if (!text.isEmpty()) {
                result = result.append(deserializeText(text, papiPlayer));
            }
            String token = m.group();
            if ("{prefix}".equals(token)) result = result.append(prefix);
            else if ("{player}".equals(token)) {
                String colored = translateAmpersandHex((playerColor == null ? "&f" : playerColor) + rawPlayer.getName());
                result = result.append(legacy.deserialize(colored));
            } else if ("{message}".equals(token)) result = result.append(message);
            last = m.end();
        }
        if (last < template.length()) {
            String tail = template.substring(last);
            result = result.append(deserializeText(tail, papiPlayer));
        }
        return result;
    }

    private Component deserializeText(String input, Player papiPlayer) {
        if (input == null || input.isEmpty()) return Component.empty();
        String s = input;
        if (papiPlayer != null) {
            try { s = PlaceholderAPI.setPlaceholders(papiPlayer, s); } catch (Throwable ignored) {}
        }
        if (looksLikeMiniMessage(s)) {
            s = preprocessHexGradientTags(s);
            try { return mini.deserialize(s); } catch (Throwable ignored) {}
        }
        return legacy.deserialize(translateAmpersandHex(s));
    }

    private boolean looksLikeMiniMessage(String s) {
        return s.indexOf('<') >= 0 && s.indexOf('>') > s.indexOf('<');
    }

    private String preprocessHexGradientTags(String s) {
        try {
            Pattern pat = Pattern.compile("<#([A-Fa-f0-9]{6})>(.*?)</#([A-Fa-f0-9]{6})>", Pattern.DOTALL);
            Matcher m = pat.matcher(s);
            StringBuffer out = new StringBuffer();
            while (m.find()) {
                String rep = "<gradient:#" + m.group(1) + ":#" + m.group(3) + ">" + m.group(2) + "</gradient>";
                m.appendReplacement(out, Matcher.quoteReplacement(rep));
            }
            m.appendTail(out);
            return out.toString();
        } catch (Throwable ignored) {
            return s;
        }
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

    private static boolean isValid(String s) {
        return s != null && !s.isBlank() && !"null".equalsIgnoreCase(s);
    }
}
