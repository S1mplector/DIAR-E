package dev.diar.ui;

import java.util.prefs.Preferences;

public final class AppSettings {
    private static final Preferences PREFS = Preferences.userNodeForPackage(AppSettings.class);

    private static final String KEY_ACCENT = "accent";
    private static final String KEY_UI_SCALE = "ui_scale";
    private static final String KEY_CONFIRM_EXIT = "confirm_exit";
    private static final String KEY_AUTO_UPDATE = "auto_update";
    private static final String KEY_FIRST_RUN = "first_run";

    public enum Accent {
        COCOA("#7a6a5a", "#f4e4c1"),
        SAGE("#7a9b8e", "#f4e4c1"),
        OCEAN("#6a8e9b", "#f4e4c1"),
        AMBER("#FFC107", "#3a2f27");

        public final String highlight;
        public final String text;
        Accent(String highlight, String text){ this.highlight = highlight; this.text = text; }

        public static Accent from(String name) {
            try { return Accent.valueOf(name); } catch (Exception ignored) { return COCOA; }
        }
    }

    public static Accent getAccent() {
        return Accent.from(PREFS.get(KEY_ACCENT, Accent.COCOA.name()));
    }

    public static void setAccent(Accent accent) {
        if (accent != null) PREFS.put(KEY_ACCENT, accent.name());
    }

    public static double getUiScale() {
        return PREFS.getDouble(KEY_UI_SCALE, 1.0);
    }

    public static void setUiScale(double scale) {
        if (scale < 0.75) scale = 0.75; if (scale > 2.0) scale = 2.0;
        PREFS.putDouble(KEY_UI_SCALE, scale);
    }

    public static boolean isConfirmOnExit() {
        return PREFS.getBoolean(KEY_CONFIRM_EXIT, false);
    }

    public static void setConfirmOnExit(boolean value) {
        PREFS.putBoolean(KEY_CONFIRM_EXIT, value);
    }

    public static boolean isAutoUpdateEnabled() {
        return PREFS.getBoolean(KEY_AUTO_UPDATE, true);
    }

    public static void setAutoUpdateEnabled(boolean value) {
        PREFS.putBoolean(KEY_AUTO_UPDATE, value);
    }

    public static boolean isFirstRun() {
        return PREFS.getBoolean(KEY_FIRST_RUN, true);
    }

    public static void setFirstRun(boolean value) {
        PREFS.putBoolean(KEY_FIRST_RUN, value);
    }

    private AppSettings() {}
}
