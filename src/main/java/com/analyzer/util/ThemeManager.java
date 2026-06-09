package com.analyzer.util;

import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);

    public static final String LIGHT_THEME = "light";
    public static final String DARK_THEME = "dark";

    private static String currentTheme = LIGHT_THEME;
    private static final List<ThemeChangeListener> listeners = new ArrayList<>();

    static {
        String savedTheme = Config.getString("theme");
        if (savedTheme != null && !savedTheme.isEmpty()) {
            currentTheme = savedTheme;
        }
        logger.info("当前主题: {}", currentTheme);
    }

    public static String getCurrentTheme() {
        return currentTheme;
    }

    public static void switchTheme(String theme) {
        if (!theme.equals(currentTheme)) {
            currentTheme = theme;
            Config.set("theme", theme);
            logger.info("✓ 主题已切换: {}", theme);
            for (ThemeChangeListener listener : listeners) {
                listener.onThemeChanged(theme);
            }
        }
    }

    public static void setDarkTheme() {
        switchTheme(DARK_THEME);
    }

    public static void setLightTheme() {
        switchTheme(LIGHT_THEME);
    }

    public static boolean isDarkTheme() {
        return DARK_THEME.equals(currentTheme);
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) return;
        // Remove all previous theme stylesheets
        scene.getStylesheets().removeIf(s -> s.contains("dark-theme.css") || s.contains("light-theme.css"));
        // Load the current theme
        String cssFile = isDarkTheme() ? "/css/dark-theme.css" : "/css/light-theme.css";
        try {
            String cssUrl = ThemeManager.class.getResource(cssFile).toExternalForm();
            scene.getStylesheets().add(cssUrl);
            logger.info("✓ 主题已应用: {}", currentTheme);
        } catch (Exception e) {
            logger.warn("加载主题CSS失败: {}", cssFile);
        }
    }

    public static void addThemeChangeListener(ThemeChangeListener listener) {
        listeners.add(listener);
    }

    public static void removeThemeChangeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    public interface ThemeChangeListener {
        void onThemeChanged(String theme);
    }
}
