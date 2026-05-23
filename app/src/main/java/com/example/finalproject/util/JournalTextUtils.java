package com.example.finalproject.util;

import android.text.Html;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JournalTextUtils {
    private static final String ALIGN_LEFT = "<!--journal-align:left-->";
    private static final String ALIGN_CENTER = "<!--journal-align:center-->";
    private static final String ALIGN_RIGHT = "<!--journal-align:right-->";
    private static final Pattern TEXT_SIZE_MARKER = Pattern.compile("<!--journal-size:(\\d+)-->");

    private JournalTextUtils() {
    }

    public static Spanned fromStoredContent(String value) {
        if (isBlank(value)) {
            return new SpannableString("");
        }
        return Html.fromHtml(stripFormattingMarkers(value), Html.FROM_HTML_MODE_LEGACY);
    }

    public static String toStoredContent(Spanned value) {
        if (value == null || value.toString().trim().isEmpty()) {
            return "";
        }
        return Html.toHtml(value, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
    }

    public static String plainText(String value) {
        return fromStoredContent(value).toString().trim();
    }

    public static String withTextSizeMarker(String value, int textSizeSp) {
        String cleanValue = stripTextSizeMarker(value);
        if (isBlank(cleanValue)) {
            return "";
        }
        return "<!--journal-size:" + textSizeSp + "-->" + cleanValue;
    }

    public static int textSizeFromStoredContent(String value, int defaultTextSizeSp) {
        if (value == null) {
            return defaultTextSizeSp;
        }
        Matcher matcher = TEXT_SIZE_MARKER.matcher(value);
        if (!matcher.find()) {
            return defaultTextSizeSp;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return defaultTextSizeSp;
        }
    }

    public static String withAlignmentMarker(String value, Layout.Alignment alignment) {
        String cleanValue = stripAlignmentMarker(value);
        if (alignment == Layout.Alignment.ALIGN_CENTER) {
            return ALIGN_CENTER + cleanValue;
        }
        if (alignment == Layout.Alignment.ALIGN_OPPOSITE) {
            return ALIGN_RIGHT + cleanValue;
        }
        return ALIGN_LEFT + cleanValue;
    }

    public static Layout.Alignment alignmentFromStoredContent(String value) {
        if (value == null) {
            return Layout.Alignment.ALIGN_NORMAL;
        }
        String cleanValue = stripTextSizeMarker(value);
        if (cleanValue.startsWith(ALIGN_CENTER)) {
            return Layout.Alignment.ALIGN_CENTER;
        }
        if (cleanValue.startsWith(ALIGN_RIGHT)) {
            return Layout.Alignment.ALIGN_OPPOSITE;
        }
        return Layout.Alignment.ALIGN_NORMAL;
    }

    private static String stripFormattingMarkers(String value) {
        return stripTextSizeMarker(stripAlignmentMarker(value));
    }

    private static String stripAlignmentMarker(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace(ALIGN_LEFT, "")
                .replace(ALIGN_CENTER, "")
                .replace(ALIGN_RIGHT, "");
    }

    private static String stripTextSizeMarker(String value) {
        if (value == null) {
            return "";
        }
        return TEXT_SIZE_MARKER.matcher(value).replaceAll("");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
