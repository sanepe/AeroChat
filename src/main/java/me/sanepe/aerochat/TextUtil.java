package me.sanepe.aerochat;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared text utilities for AeroChat: placeholder expansion, MiniMessage/legacy deserialization,
 * and helper transforms (HEX via & syntax, gradient shortcut).
 */
public final class TextUtil {

    private TextUtil() {}

    private static final Pattern AMP_HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_SHORTCUT = Pattern.compile("<#([A-Fa-f0-9]{6})>(.*?)</#([A-Fa-f0-9]{6})>", Pattern.DOTALL);

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    public static Component deserializeWithMini(String input, boolean papiEnabled, Player papiPlayer) {
        if (input == null || input.isEmpty()) return Component.empty();
        String s = input;
        if (papiEnabled && papiPlayer != null && s.indexOf('%') >= 0) {
            try {
                s = PlaceholderAPI.setPlaceholders(papiPlayer, s);
            } catch (Throwable t) {
                logFine("PAPI expansion failed (mini): " + safeSnippet(s), t);
            }
        }
        if (looksLikeMiniMessage(s)) {
            s = preprocessHexGradientTags(s);
            try {
                return MINI.deserialize(s);
            } catch (Throwable t) {
                logFine("MiniMessage parse failed, falling back to legacy for: " + safeSnippet(s), t);
            }
        }
        return LEGACY.deserialize(translateAmpersandHex(s));
    }

    /** Legacy-only path (used e.g. for ClearChat broadcast where MiniMessage is disabled). */
    public static Component deserializeLegacy(String input, boolean papiEnabled, Player papiPlayer) {
        if (input == null || input.isEmpty()) return Component.empty();
        String s = input;
        if (papiEnabled && papiPlayer != null && s.indexOf('%') >= 0) {
            try {
                s = PlaceholderAPI.setPlaceholders(papiPlayer, s);
            } catch (Throwable t) {
                logFine("PAPI expansion failed (legacy): " + safeSnippet(s), t);
            }
        }
        return LEGACY.deserialize(translateAmpersandHex(s));
    }

    public static String translateAmpersandHex(String input) {
        if (input == null || input.isEmpty()) return input;
        Matcher m = AMP_HEX_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            StringBuilder rep = new StringBuilder("&x");
            for (char c : hex.toCharArray()) rep.append('&').append(c);
            m.appendReplacement(sb, Matcher.quoteReplacement(rep.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static boolean looksLikeMiniMessage(String s) {
        return s != null && s.indexOf('<') >= 0 && s.indexOf('>') > s.indexOf('<');
    }

    public static String preprocessHexGradientTags(String s) {
        if (s == null || s.isEmpty()) return s;
        try {
            Matcher m = GRADIENT_SHORTCUT.matcher(s);
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

    private static void logFine(String msg, Throwable t) {
        PaperBasePlugin inst = PaperBasePlugin.getInstance();
        if (inst != null) inst.getLogger().log(Level.FINE, msg, t);
    }

    private static String safeSnippet(String s) {
        if (s == null) return "null";
        s = s.replace('\n', ' ').replace('\r', ' ');
        return s.length() > 120 ? s.substring(0, 120) + "…" : s;
    }
}
