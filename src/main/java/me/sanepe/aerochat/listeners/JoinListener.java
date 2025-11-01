package me.sanepe.aerochat.listeners;

import me.sanepe.aerochat.PaperBasePlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JoinListener implements Listener {

    private static final Pattern AMP_HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private final PaperBasePlugin plugin;
    private final boolean papiEnabled;
    private final LegacyComponentSerializer legacy;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public JoinListener() {
        this.plugin = PaperBasePlugin.getInstance();
        this.papiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        this.legacy = LegacyComponentSerializer.builder().character('&').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final FileConfiguration cfg = plugin.getConfig();
        final String playerColor = cfg.getString("player_color", "&f");

        boolean welcomeEnabled = cfg.getBoolean("join.welcome.enabled", true);
        if (welcomeEnabled) {
            String msg = cfg.getString("join.welcome.message", "&aWelcome to the server, &f{player}&a!");
            sendTemplated(player, msg, player, playerColor);
        }

        boolean broadcastEnabled = cfg.getBoolean("join.broadcast.enabled", true);
        if (broadcastEnabled) {
            String tmpl = cfg.getString("join.broadcast.message", "&e{player} joined the server");
            Component comp = renderTemplate(tmpl, player, playerColor);
            event.joinMessage(comp);
        } else {
            event.joinMessage(null);
        }
    }

    private void sendTemplated(Player target, String template, Player papiPlayer, String playerColor) {
        Component comp = renderTemplate(template, papiPlayer, playerColor);
        target.sendMessage(comp);
    }

    private Component renderTemplate(String template, Player papiPlayer, String playerColor) {
        if (template == null) template = "{player}";
        Pattern p = Pattern.compile("(\\{player\\})");
        Matcher m = p.matcher(template);
        int last = 0;
        Component result = Component.empty();
        while (m.find()) {
            String text = template.substring(last, m.start());
            if (!text.isEmpty()) {
                result = result.append(deserializeText(text, papiPlayer));
            }
            if (papiPlayer != null) {
                String base = (playerColor == null ? "&f" : playerColor) + papiPlayer.getName();
                result = result.append(deserializeText(base, null));
            } else {
                result = result.append(Component.text("player"));
            }
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
        if (s.indexOf('<') >= 0 && s.indexOf('>') > s.indexOf('<')) {
            s = preprocessHexGradientTags(s);
            try { return mini.deserialize(s); } catch (Throwable ignored) { }
        }
        return legacy.deserialize(translateAmpersandHex(s));
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
}
