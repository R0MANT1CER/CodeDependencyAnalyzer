package com.analyzer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LanguageManager {
    private static final Logger logger = LoggerFactory.getLogger(LanguageManager.class);

    private static String currentLang = "English";
    private static final Map<String, String> translations = new HashMap<>();
    private static final List<Runnable> listeners = new ArrayList<>();

    static {
        String saved = Config.getString("language");
        if (saved != null && !saved.isEmpty()) currentLang = saved;
        loadTranslations();
    }

    private static void loadTranslations() {
        translations.clear();
        String file = currentLang.equals("Chinese") ? "/i18n/messages_zh.properties" : "/i18n/messages_en.properties";
        try (InputStream is = LanguageManager.class.getResourceAsStream(file)) {
            if (is == null) { logger.warn("Resource not found: {}", file); return; }
            Properties props = new Properties();
            props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
            for (String key : props.stringPropertyNames()) {
                translations.put(key, props.getProperty(key));
            }
            logger.info("Loaded {} translations for {}", translations.size(), currentLang);
        } catch (Exception e) {
            logger.error("Failed to load translations", e);
        }
    }

    public static String get(String key) {
        return translations.getOrDefault(key, key);
    }

    public static String getCurrentLang() { return currentLang; }

    public static void setLanguage(String lang) {
        if (!lang.equals(currentLang)) {
            currentLang = lang;
            Config.set("language", lang);
            loadTranslations();
            for (Runnable r : listeners) r.run();
            logger.info("Language switched to: {}", lang);
        }
    }

    public static void addListener(Runnable listener) { listeners.add(listener); }
}