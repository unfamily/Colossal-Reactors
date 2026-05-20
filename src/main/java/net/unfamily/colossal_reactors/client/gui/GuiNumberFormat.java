package net.unfamily.colossal_reactors.client.gui;

/**
 * Compact number display for reactor GUI stats (e.g. 1500 → 1.5K). Not used for rod counts or build material lines.
 */
public final class GuiNumberFormat {

    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "P", "E"};

    private GuiNumberFormat() {
    }

    public static String format(long value) {
        return format((double) value);
    }

    public static String format(int value) {
        return format((double) value);
    }

    public static String format(double value) {
        double abs = Math.abs(value);
        if (abs < 1000) {
            if (value == Math.floor(value)) {
                return String.valueOf((long) value);
            }
            String s = String.format("%.1f", value);
            return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
        }
        int suffixIndex = 0;
        double scaled = value;
        while (Math.abs(scaled) >= 1000 && suffixIndex < SUFFIXES.length - 1) {
            scaled /= 1000;
            suffixIndex++;
        }
        if (scaled == Math.floor(scaled)) {
            return String.format("%.0f%s", scaled, SUFFIXES[suffixIndex]);
        }
        return String.format("%.1f%s", scaled, SUFFIXES[suffixIndex]);
    }
}
