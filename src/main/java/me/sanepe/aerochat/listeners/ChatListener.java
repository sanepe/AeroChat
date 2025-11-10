package me.sanepe.aerochat.listeners;

import me.sanepe.aerochat.PaperBasePlugin;
import me.sanepe.aerochat.TextUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.cacheddata.CachedMetaData;

public class ChatListener implements Listener {

    private final PaperBasePlugin plugin;
    private final boolean papiEnabled;
    private LuckPerms luckPerms = null;
    private boolean lpEnabled = false;

    public ChatListener(PaperBasePlugin plugin) {
        this.plugin = plugin;
        this.papiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        // Only touch LuckPerms classes if the plugin is actually present to avoid CNFE/NCDFE
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                this.luckPerms = LuckPermsProvider.get();
                this.lpEnabled = (this.luckPerms != null);
            } catch (Throwable ignored) {
                this.lpEnabled = false;
            }
        } else {
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
            prefix = TextUtil.deserializeWithMini(prefixStr, papiEnabled, papiEnabled ? player : null);
        }

        final Component finalPrefix = prefix;
        final String templateRaw = cfg.getString("chat-format", "{prefix} &7{player}&7: ");
        final String msgColorTemplate = resolveMessageColorTemplate(cfg, group);
        final Player papiPlayer = papiEnabled ? player : null;
        final boolean allowLegacyColors = isColorFormattingAllowed(cfg, group);
        event.renderer((source, displayName, message, viewer) -> {
            if (templateRaw.contains("{message}")) {
                Component processedMessage = processMessageComponent(message, allowLegacyColors);
                return renderTemplate(templateRaw, finalPrefix, displayName, processedMessage, papiPlayer, player, playerColor);
            }
            Component head = renderTemplateNoMessage(templateRaw, finalPrefix, displayName, papiPlayer, player, playerColor);
            Component processedMessage = processMessageComponent(message, allowLegacyColors);
            Component coloredMsg = renderColoredMessage(msgColorTemplate, processedMessage, papiPlayer);
            return head.append(coloredMsg);
        });
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
                result = result.append(TextUtil.deserializeWithMini(text, papiPlayer != null, papiPlayer));
            }
            String token = m.group();
            if ("{prefix}".equals(token)) result = result.append(prefix);
            else if ("{player}".equals(token)) {
                String base = (playerColor == null ? "&f" : playerColor) + rawPlayer.getName();
                result = result.append(TextUtil.deserializeLegacy(base, false, null));
            } else if ("{message}".equals(token)) result = result.append(message);
            last = m.end();
        }
        if (last < template.length()) {
            String tail = template.substring(last);
            result = result.append(TextUtil.deserializeWithMini(tail, papiPlayer != null, papiPlayer));
        }
        return result;
    }

    private Component renderTemplateNoMessage(String template, Component prefix, Component playerName, Player papiPlayer, Player rawPlayer, String playerColor) {
        if (template == null) template = "{prefix} {player}: ";
        Pattern p = Pattern.compile("(\\{prefix\\}|\\{player\\})");
        Matcher m = p.matcher(template);
        int last = 0;
        Component result = Component.empty();
        while (m.find()) {
            String text = template.substring(last, m.start());
            if (!text.isEmpty()) {
                result = result.append(TextUtil.deserializeWithMini(text, papiPlayer != null, papiPlayer));
            }
            String token = m.group();
            if ("{prefix}".equals(token)) result = result.append(prefix);
            else if ("{player}".equals(token)) {
                String base = (playerColor == null ? "&f" : playerColor) + rawPlayer.getName();
                result = result.append(TextUtil.deserializeLegacy(base, false, null));
            }
            last = m.end();
        }
        if (last < template.length()) {
            String tail = template.substring(last);
            result = result.append(TextUtil.deserializeWithMini(tail, papiPlayer != null, papiPlayer));
        }
        return result;
    }

    private String resolveMessageColorTemplate(FileConfiguration cfg, String group) {
        String s = cfg.getString("message-color.group." + group, null);
        if (s == null || s.isBlank()) s = cfg.getString("message-color.group.default", null);
        if (s == null || s.isBlank()) s = "&f{message}";
        return s;
    }

    private Component renderColoredMessage(String mapping, Component userMessageComp, Player papiPlayer) {
        if (mapping == null) mapping = "{message}";
        Pattern p = Pattern.compile("(\\{message\\})");
        Matcher m = p.matcher(mapping);
        int last = 0;
        Component result = Component.empty();
        while (m.find()) {
            String text = mapping.substring(last, m.start());
            if (!text.isEmpty()) {
                result = result.append(TextUtil.deserializeWithMini(text, papiPlayer != null, papiPlayer));
            }
            result = result.append(userMessageComp);
            last = m.end();
        }
        if (last < mapping.length()) {
            String tail = mapping.substring(last);
            result = result.append(TextUtil.deserializeWithMini(tail, papiPlayer != null, papiPlayer));
        }
        return result;
    }

    private boolean isColorFormattingAllowed(FileConfiguration cfg, String group) {
        String path = "color-formating." + group;
        if (cfg.isSet(path)) return cfg.getBoolean(path, true);
        // Missing group -> default true
        return true;
    }

    private Component processMessageComponent(Component original, boolean allowLegacyColors) {
        String raw = PlainTextComponentSerializer.plainText().serialize(original);
        if (allowLegacyColors) {
            // Interpret legacy & codes and hex in user's message
            return TextUtil.deserializeLegacy(raw, false, null);
        }
        // No color formatting allowed: return plain text as-is
        return Component.text(raw);
    }
    private static boolean isValid(String s) {
        return s != null && !s.isBlank() && !"null".equalsIgnoreCase(s);
    }
}
