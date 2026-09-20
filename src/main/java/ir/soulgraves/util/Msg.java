package ir.soulgraves.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;

public final class Msg {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Msg() {}

    public static Component parse(String raw) {
        if (raw == null) return Component.empty();
        return MM.deserialize(raw);
    }

    public static Component parse(String raw, Map<String, String> placeholders) {
        if (raw == null) return Component.empty();
        String out = raw;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return MM.deserialize(out);
    }

    public static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }
}
