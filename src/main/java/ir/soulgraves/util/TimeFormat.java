package ir.soulgraves.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeFormat {

    private TimeFormat() {}

    public static String formatEpoch(long epochMs, String pattern) {
        return DateTimeFormatter.ofPattern(pattern)
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(epochMs));
    }

    /** Human "22h 14m" / "3m 12s" style. */
    public static String formatDuration(long millis) {
        if (millis <= 0) return "expired";
        Duration d = Duration.ofMillis(millis);
        long days = d.toDays();
        long hours = d.toHoursPart();
        long mins = d.toMinutesPart();
        long secs = d.toSecondsPart();
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + mins + "m";
        if (mins > 0) return mins + "m " + secs + "s";
        return secs + "s";
    }
}
