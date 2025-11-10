package me.sanepe.aerochat.listeners;

import me.sanepe.aerochat.PaperBasePlugin;
import me.sanepe.aerochat.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JoinListener implements Listener {

    private static final Pattern PLAYER_TOKEN = Pattern.compile("(\\{player\\})");
    private final PaperBasePlugin plugin;
    private final boolean papiEnabled;

    public JoinListener() {
        this.plugin = PaperBasePlugin.getInstance();
    this.papiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final FileConfiguration cfg = plugin.getConfig();
        final String playerColor = cfg.getString("player_color", "&f");

        // First join detection and messages
        boolean isFirstTime = false;
        int playerNumber = 0;
        try {
            int before = plugin.registerAndCount(player.getUniqueId());
            // If they weren't in the set previously, registerAndCount just added them; however, we can't know prior state directly.
            // Approximation: if player has never played before, Paper's API can tell; fallback to registry size as number.
            isFirstTime = !player.hasPlayedBefore();
            playerNumber = before; // total unique players seen by plugin
        } catch (Throwable ignored) {}

        if (isFirstTime) {
            boolean fjEnabled = cfg.getBoolean("join.first_join.enabled", true);
            if (fjEnabled) {
                String msg = cfg.getString("join.first_join.message", "&b{player}&7 is player number &e{player_number}&7 to join the server");
                msg = msg.replace("{player_number}", String.valueOf(playerNumber));
                Component comp = renderTemplate(msg, player, playerColor);
                player.sendMessage(comp);
            }
            boolean fjbEnabled = cfg.getBoolean("join.first_broadcast.enabled", true);
            if (fjbEnabled) {
                String tmpl = cfg.getString("join.first_broadcast.message", "&e{player} &7is player number &b{player_number}&7 to join the server");
                tmpl = tmpl.replace("{player_number}", String.valueOf(playerNumber));
                Component comp = renderTemplate(tmpl, player, playerColor);
                boolean showSelf = cfg.getBoolean("join.first_broadcast.show_player_their_broadcast", false);
                if (showSelf) {
                    event.joinMessage(comp); // standard broadcast includes joining player
                } else {
                    event.joinMessage(null); // suppress default
                    // Manual broadcast to others only
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!p.getUniqueId().equals(player.getUniqueId())) {
                            p.sendMessage(comp);
                        }
                    }
                }
            }
            // Suppress normal welcome/broadcast on first join
            return;
        }

        boolean welcomeEnabled = cfg.getBoolean("join.welcome.enabled", true);
        if (welcomeEnabled) {
            String msg = cfg.getString("join.welcome.message", "&aWelcome to the server, &f{player}&a!");
            sendTemplated(player, msg, player, playerColor);
        }

        boolean broadcastEnabled = cfg.getBoolean("join.broadcast.enabled", true);
        if (broadcastEnabled) {
            String tmpl = cfg.getString("join.broadcast.message", "&e{player} joined the server");
            Component comp = renderTemplate(tmpl, player, playerColor);
            boolean showSelf = cfg.getBoolean("join.broadcast.show_player_their_broadcast", true);
            if (showSelf) {
                event.joinMessage(comp);
            } else {
                event.joinMessage(null);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getUniqueId().equals(player.getUniqueId())) {
                        p.sendMessage(comp);
                    }
                }
            }
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
    Matcher m = PLAYER_TOKEN.matcher(template);
        int last = 0;
        Component result = Component.empty();
        while (m.find()) {
            String text = template.substring(last, m.start());
            if (!text.isEmpty()) {
                result = result.append(TextUtil.deserializeWithMini(text, papiEnabled, papiPlayer));
            }
            if (papiPlayer != null) {
                String base = (playerColor == null ? "&f" : playerColor) + papiPlayer.getName();
                result = result.append(TextUtil.deserializeLegacy(base, false, null));
            } else {
                result = result.append(Component.text("player"));
            }
            last = m.end();
        }
        if (last < template.length()) {
            String tail = template.substring(last);
            result = result.append(TextUtil.deserializeWithMini(tail, papiEnabled, papiPlayer));
        }
        return result;
    }
}
