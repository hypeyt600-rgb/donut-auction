package com.donutauction;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MoneyUtil {
    private static final Pattern P = Pattern.compile("^\\$?\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([kKmMbB])?$");

    private MoneyUtil() {}

    /** Parses "5000", "5k", "1.5m", "2b", "$1,000". Returns -1 if invalid. */
    public static long parse(String s) {
        if (s == null) return -1;
        Matcher m = P.matcher(s.trim());
        if (!m.matches()) return -1;
        double v = Double.parseDouble(m.group(1).replace(",", ""));
        String suf = m.group(2);
        if (suf != null) {
            switch (suf.toLowerCase()) {
                case "k" -> v *= 1_000d;
                case "m" -> v *= 1_000_000d;
                case "b" -> v *= 1_000_000_000d;
            }
        }
        return Math.round(v);
    }

    public static String format(long v) {
        long a = Math.abs(v);
        String sign = v < 0 ? "-" : "";
        if (a >= 1_000_000_000L) return sign + trim(a / 1_000_000_000d) + "B";
        if (a >= 1_000_000L) return sign + trim(a / 1_000_000d) + "M";
        if (a >= 1_000L) return sign + trim(a / 1_000d) + "K";
        return sign + a;
    }

    private static String trim(double d) {
        String s = String.format("%.2f", d);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}
