package com.analyzer.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".code-analyzer";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.json";
    
    private static Map<String, Object> config = new HashMap<>();
    
    static {
        loadConfig();
    }
    
    /**
     * 加载配置
     */
    private static void loadConfig() {
        try {
            new File(CONFIG_DIR).mkdirs();
            
            if (new File(CONFIG_FILE).exists()) {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    config = gson.fromJson(reader, Map.class);
                }
            } else {
                // 默认配置
                config.put("skillDir", CONFIG_DIR + File.separator + "skills");
                config.put("lastProject", "");
                config.put("autoMonitor", true);
                config.put("theme", "light");
                config.put("windowWidth", 1400.0);
                config.put("windowHeight", 900.0);
                saveConfig();
            }
            
            logger.info("✓ 配置已加载");
        } catch (Exception e) {
            logger.error("加载配置失败", e);
        }
    }
    
    /**
     * 保存配置
     */
    public static void saveConfig() {
        try {
            new File(CONFIG_DIR).mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                gson.toJson(config, writer);
            }
            logger.info("✓ 配置已保存");
        } catch (Exception e) {
            logger.error("保存配置失败", e);
        }
    }
    
    /**
     * 获取值
     */
    public static Object get(String key) {
        return config.get(key);
    }
    
    /**
     * 设置值
     */
    public static void set(String key, Object value) {
        config.put(key, value);
        saveConfig();
    }
    
    /**
     * 获取字符串
     */
    public static String getString(String key) {
        Object value = config.get(key);
        return value != null ? value.toString() : "";
    }
    
    /**
     * 获取布尔值
     */
    public static boolean getBoolean(String key) {
        Object value = config.get(key);
        return value instanceof Boolean ? (Boolean) value : false;
    }
    
    /**
     * 获取双精度数
     */
    public static double getDouble(String key) {
        Object value = config.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }
    
    public static String getConfigDir() {
        return CONFIG_DIR;
    }
    
    public static String getSkillDir() {
        return getString("skillDir");
    }
    
    public static void setSkillDir(String dir) {
        set("skillDir", dir);
    }
}
