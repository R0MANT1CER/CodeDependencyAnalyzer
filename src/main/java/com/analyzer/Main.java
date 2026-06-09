package com.analyzer;

import com.analyzer.util.Config;
import com.analyzer.util.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("========== CodeDependencyAnalyzer v1.0 ==========");

        // Load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();

        // Create scene
        double width = Config.getDouble("windowWidth");
        double height = Config.getDouble("windowHeight");
        Scene scene = new Scene(root, width > 0 ? width : 1280, height > 0 ? height : 800);

        // Apply saved theme
        ThemeManager.applyTheme(scene);

        // Window title
        primaryStage.setTitle("BugScope - 代码依赖分析与影响评估工具");
        primaryStage.setScene(scene);

        // Try loading icon
        try {
            Image icon = new Image(getClass().getResourceAsStream("/images/icon.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            logger.warn("图标未找到，使用默认图标");
        }

        // Save window size on change
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) ->
            Config.set("windowWidth", newVal.doubleValue())
        );
        primaryStage.heightProperty().addListener((obs, oldVal, newVal) ->
            Config.set("windowHeight", newVal.doubleValue())
        );

        // Register scene for theme switching
        primaryStage.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                ThemeManager.applyTheme(newScene);
            }
        });

        primaryStage.show();
        logger.info("✓ 应用已启动");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
