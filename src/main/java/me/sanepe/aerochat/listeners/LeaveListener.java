package me.sanepe.aerochat.listeners;

import me.sanepe.aerochat.PaperBasePlugin;
import me.sanepe.aerochat.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles leave/quit broadcast messages.
 */
public class LeaveListener implements Listener {

    private final PaperBasePlugin plugin;
    private final boolean papiEnabled;

    public LeaveListener(PaperBasePlugin plugin) {
        this.plugin = plugin;
        this.papiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("leave.broadcast.enabled", true)) {
            // Respect server's default leave message when disabled
            return;
        }
        String template = cfg.getString("leave.broadcast.message", "&c{player} left the server");
        Component comp = render(template, player);
        // Player is leaving; only broadcast to others.
        event.quitMessage(null); // prevent default
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(player.getUniqueId())) {
                p.sendMessage(comp);
            }
        }
    }

    private Component render(String template, Player player) {
        if (template == null) template = "{player}";
        String playerColor = plugin.getConfig().getString("player_color", "&f");
        String baseName = (playerColor == null ? "&f" : playerColor) + player.getName();
        // Simple replacement; allow MiniMessage/legacy in surrounding text
        String replaced = template.replace("{player}", baseName);
        return TextUtil.deserializeWithMini(replaced, papiEnabled, papiEnabled ? player : null);
    }
}
