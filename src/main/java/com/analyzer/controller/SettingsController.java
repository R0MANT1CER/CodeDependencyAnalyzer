package com.analyzer.controller;

import com.analyzer.util.Config;
import com.analyzer.util.ThemeManager;
import com.analyzer.util.LanguageManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML private Label lblSettingsTitle, lblTheme, lblLanguage, lblSkillPath;
    @FXML private ComboBox<String> themeCombo;
    @FXML private ComboBox<String> languageCombo;
    @FXML private CheckBox autoOpenCheck, autoMonitorCheck, autoSkillCheck;
    @FXML private TextField skillPathField;
    @FXML private TitledPane tpAppearance, tpGeneral, tpPaths;
    @FXML private Button btnBrowse, btnOK, btnCancel;

    private Scene ownerScene;

    public void setOwnerScene(Scene scene) { this.ownerScene = scene; }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        themeCombo.getItems().addAll(LanguageManager.get("settings.theme.light"), LanguageManager.get("settings.theme.dark"));
        themeCombo.setValue(ThemeManager.isDarkTheme() ? LanguageManager.get("settings.theme.dark") : LanguageManager.get("settings.theme.light"));

        languageCombo.getItems().addAll(LanguageManager.get("settings.lang.english"), LanguageManager.get("settings.lang.chinese"));
        languageCombo.setValue(LanguageManager.getCurrentLang().equals("Chinese") ? LanguageManager.get("settings.lang.chinese") : LanguageManager.get("settings.lang.english"));

        autoOpenCheck.setSelected(Config.getBoolean("autoOpenLast"));
        autoMonitorCheck.setSelected(Config.getBoolean("autoMonitor"));
        autoSkillCheck.setSelected(Config.getBoolean("autoOpenSkill"));

        String sp = Config.getString("skillPath");
        skillPathField.setText(sp.isEmpty() ? System.getProperty("user.home") + "/.code-analyzer/skills" : sp);

        LanguageManager.addListener(this::applyLanguage);
        applyLanguage();
    }

    private void applyLanguage() {
        Platform.runLater(() -> {
            if (lblSettingsTitle != null) lblSettingsTitle.setText(LanguageManager.get("settings.title"));
            if (tpAppearance != null) tpAppearance.setText(LanguageManager.get("settings.appearance"));
            if (lblTheme != null) lblTheme.setText(LanguageManager.get("settings.theme") + ":");
            if (lblLanguage != null) lblLanguage.setText(LanguageManager.get("settings.language") + ":");
            if (tpGeneral != null) tpGeneral.setText(LanguageManager.get("settings.general"));
            if (autoOpenCheck != null) autoOpenCheck.setText(LanguageManager.get("settings.autoopen"));
            if (autoMonitorCheck != null) autoMonitorCheck.setText(LanguageManager.get("settings.automonitor"));
            if (autoSkillCheck != null) autoSkillCheck.setText(LanguageManager.get("settings.autoskill"));
            if (tpPaths != null) tpPaths.setText(LanguageManager.get("settings.paths"));
            if (lblSkillPath != null) lblSkillPath.setText(LanguageManager.get("settings.skillpath"));
            if (btnBrowse != null) btnBrowse.setText(LanguageManager.get("settings.browse"));
            if (btnOK != null) btnOK.setText(LanguageManager.get("settings.ok"));
            if (btnCancel != null) btnCancel.setText(LanguageManager.get("settings.cancel"));
        });
    }

    @FXML
    private void onOK() {
        // Apply theme
        String t = themeCombo.getValue();
        if (LanguageManager.get("settings.theme.dark").equals(t)) ThemeManager.setDarkTheme();
        else ThemeManager.setLightTheme();
        if (ownerScene != null) ThemeManager.applyTheme(ownerScene);

        // Apply language
        String lang = languageCombo.getValue();
        LanguageManager.setLanguage(LanguageManager.get("settings.lang.chinese").equals(lang) ? "Chinese" : "English");

        // Save
        Config.set("autoOpenLast", autoOpenCheck.isSelected());
        Config.set("autoMonitor", autoMonitorCheck.isSelected());
        Config.set("autoOpenSkill", autoSkillCheck.isSelected());
        Config.set("skillPath", skillPathField.getText());

        closeDialog();
    }

    @FXML
    private void onCancel() { closeDialog(); }

    @FXML
    private void onBrowseSkillPath() {}

    private void closeDialog() {
        ((Stage) themeCombo.getScene().getWindow()).close();
    }
}