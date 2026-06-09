package com.analyzer.controller;

import com.analyzer.graph.DependencyGraph;
import com.analyzer.graph.MethodGraph;
import com.analyzer.model.ClassNode;
import com.analyzer.model.DependencyEdge;
import com.analyzer.model.MethodNode;
import com.analyzer.model.FieldNode;
import com.analyzer.monitor.FileChangeMonitor;
import com.analyzer.parser.JavaCodeParser;
import com.analyzer.parser.MethodDependencyExtractor;
import com.analyzer.skill.SkillGenerator;
import com.analyzer.util.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.*;
import org.slf4j.*;

import java.io.File;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class MainController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private MenuBar menuBar;
    @FXML private Menu menuFile, menuEdit, menuView, menuMethod, menuHelp;
    @FXML private MenuItem miOpen, miSettings, miExit;
    @FXML private MenuItem miRefresh, miClear;
    @FXML private CheckMenuItem miShowDetails, miAutoMonitor;
    @FXML private MenuItem miCallTree, miImpact, miCycles, miComplexity, miFields;
    @FXML private MenuItem miAbout;
    @FXML private ToolBar toolBar;
    @FXML private Button btnOpen, btnRefresh, btnAnalyze, btnExport;
    @FXML private Button btnCallTree, btnImpact, btnComplexity;
    @FXML private Button btnAnalyzeImpact, btnGenerateSkill;
    @FXML private Label lblChart, lblImpactTitle;
    @FXML private Label projectNameLabel, classCountLabel, edgeCountLabel, monitorStatusLabel;
    @FXML private Label statusLabel, fileMonitorLabel, skillPathLabel, bottomStatusLabel;
    @FXML private TreeView<String> classTreeView;
    @FXML private TextArea classDetailsArea, impactResultArea, skillInfoArea;
    @FXML private TextField searchField, impactAnalysisField;
    @FXML private WebView graphWebView;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private SplitPane splitPane;
    @FXML private TabPane tabPane;
    @FXML private Tab tabDetails, tabImpact, tabSkill;
    @FXML private TitledPane tpClassTree, tpGraph;
    @FXML private ComboBox<String> visualizationTypeCombo;
    @FXML private HBox infoBar, statusBar;

    private JavaCodeParser parser = new JavaCodeParser();
    private DependencyGraph graph;
    private SkillGenerator skillGenerator = new SkillGenerator();
    private FileChangeMonitor fileMonitor = new FileChangeMonitor();
    private VisualizationManager visualizationManager;
    private MethodVisualizationManager methodVisualizer;
    private UltraClearMethodVisualizationManager ultraViz;
    private MethodGraph methodGraph;
    private MethodDependencyExtractor methodExtractor = new MethodDependencyExtractor();
    private String currentProjectPath;
    private Map<String, ClassNode> currentClasses;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Init");
        graphWebView.getEngine().setUserAgent("Mozilla/5.0");
        graphWebView.getEngine().loadContent(VisualizationManager.getPlaceholder(ThemeManager.getCurrentTheme()));
        classTreeView.getSelectionModel().selectedItemProperty().addListener((o, old, sel) -> {
            if (sel != null) showClassDetails(sel.getValue());
        });
        LanguageManager.addListener(this::applyLanguage);
        ThemeManager.addThemeChangeListener(t -> Platform.runLater(() -> { onVizChanged(); ThemeManager.applyTheme(statusLabel != null ? statusLabel.getScene() : null); }));
        applyLanguage();
        logger.info("Ready");
    }

    private void applyLanguage() {
        Platform.runLater(() -> {
            if (statusLabel != null && statusLabel.getScene() != null && statusLabel.getScene().getWindow() != null)
                ((Stage) statusLabel.getScene().getWindow()).setTitle(LanguageManager.get("app.title"));
            if (menuFile != null) menuFile.setText(LanguageManager.get("menu.file"));
            if (miOpen != null) miOpen.setText(LanguageManager.get("menu.file.open"));
            if (miSettings != null) miSettings.setText(LanguageManager.get("menu.file.settings"));
            if (miExit != null) miExit.setText(LanguageManager.get("menu.file.exit"));
            if (menuEdit != null) menuEdit.setText(LanguageManager.get("menu.edit"));
            if (miRefresh != null) miRefresh.setText(LanguageManager.get("menu.edit.refresh"));
            if (miClear != null) miClear.setText(LanguageManager.get("menu.edit.clear"));
            if (menuView != null) menuView.setText(LanguageManager.get("menu.view"));
            if (miShowDetails != null) miShowDetails.setText(LanguageManager.get("menu.view.details"));
            if (miAutoMonitor != null) miAutoMonitor.setText(LanguageManager.get("menu.view.automonitor"));
            if (btnOpen != null) btnOpen.setText(LanguageManager.get("btn.open"));
            if (btnRefresh != null) btnRefresh.setText(LanguageManager.get("btn.refresh"));
            if (btnAnalyze != null) btnAnalyze.setText(LanguageManager.get("btn.analyze"));
            if (btnExport != null) btnExport.setText(LanguageManager.get("btn.export"));
            if (btnCallTree != null) btnCallTree.setText(LanguageManager.get("btn.calltree"));
            if (btnImpact != null) btnImpact.setText(LanguageManager.get("btn.impact"));
            if (btnComplexity != null) btnComplexity.setText(LanguageManager.get("btn.complexity"));
            if (lblChart != null) lblChart.setText(LanguageManager.get("label.chart"));
            if (tabDetails != null) tabDetails.setText(LanguageManager.get("tab.details"));
            if (tabImpact != null) tabImpact.setText(LanguageManager.get("tab.impact"));
            if (tabSkill != null) tabSkill.setText(LanguageManager.get("tab.skill"));
            if (tpClassTree != null) tpClassTree.setText(LanguageManager.get("panel.classtree"));
            if (tpGraph != null) tpGraph.setText(LanguageManager.get("panel.graph"));
            if (menuMethod != null) menuMethod.setText(LanguageManager.get("menu.method"));
            if (miCallTree != null) miCallTree.setText(LanguageManager.get("menu.method.calltree"));
            if (miImpact != null) miImpact.setText(LanguageManager.get("menu.method.impact"));
            if (miCycles != null) miCycles.setText(LanguageManager.get("menu.method.cycles"));
            if (miComplexity != null) miComplexity.setText(LanguageManager.get("menu.method.complexity"));
            if (miFields != null) miFields.setText(LanguageManager.get("menu.method.fields"));
            if (btnGenerateSkill != null) btnGenerateSkill.setText(LanguageManager.get("btn.generateskill"));
            if (btnAnalyzeImpact != null) btnAnalyzeImpact.setText(LanguageManager.get("btn.analyze"));
            if (lblImpactTitle != null) lblImpactTitle.setText(LanguageManager.get("label.impact"));
            if (miAbout != null) miAbout.setText(LanguageManager.get("menu.help.about"));
            if (menuHelp != null) menuHelp.setText(LanguageManager.get("menu.help"));
        });
    }

    @FXML public void onOpenProject() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(LanguageManager.get("dialog.open.title"));
        File dir = chooser.showDialog(new Stage());
        if (dir != null) loadProject(dir.getAbsolutePath());
    }

    private void loadProject(String path) {
        currentProjectPath = path;
        new Thread(() -> {
            Platform.runLater(() -> {
                if (loadingIndicator != null) loadingIndicator.setVisible(true);
                if (statusLabel != null) statusLabel.setText(LanguageManager.get("status.scanning"));
            });
            try {
                Map<String, ClassNode> classes = parser.scanProject(path);
                graph = new DependencyGraph();
                for (ClassNode cn : classes.values()) graph.addNode(cn.fullName, cn);
                List<File> javaFiles = findJavaFiles(new File(path));
                for (File f : javaFiles) {
                    try {
                        List<DependencyEdge> edges = parser.extractDependencies(f, classes);
                        for (DependencyEdge e : edges) graph.addEdge(e);
                    } catch (Exception ex) { logger.warn("Deps: {}", f, ex.getMessage()); }
                }
                methodGraph = methodExtractor.extractDependencies(path, classes);
                methodVisualizer = new MethodVisualizationManager(methodGraph);
                ultraViz = new UltraClearMethodVisualizationManager(methodGraph, ThemeManager.getCurrentTheme());
                currentClasses = classes;
                visualizationManager = new VisualizationManager(graph, classes);
                Platform.runLater(() -> {
                    updateClassTree(classes);
                    if (projectNameLabel != null) projectNameLabel.setText(new File(path).getName());
                    if (classCountLabel != null) classCountLabel.setText(String.valueOf(classes.size()));
                    if (edgeCountLabel != null) {
                        long ec = graph.getEdges() != null ? graph.getEdges().size() : 0;
                        edgeCountLabel.setText(String.valueOf(ec));
                    }
                    updateVizOptions();
                    onVizChanged();
                    long mc = methodGraph != null ? methodGraph.getMethods().size() : 0;
                    if (statusLabel != null) statusLabel.setText(LanguageManager.get("status.ready")
                        + " | " + LanguageManager.get("status.methods") + ": " + mc + " | " + LanguageManager.get("status.classes") + " " + classes.size());
                    if (loadingIndicator != null) loadingIndicator.setVisible(false);
                });
            } catch (Exception e) {
                logger.error("Load failed", e);
                Platform.runLater(() -> { err(e); if (loadingIndicator != null) loadingIndicator.setVisible(false); });
            }
        }).start();
    }

    private List<File> findJavaFiles(File dir) {
        List<File> result = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return result;
        for (File f : files) {
            if (f.isDirectory() && !f.getName().startsWith(".")) result.addAll(findJavaFiles(f));
            else if (f.isFile() && f.getName().endsWith(".java")) result.add(f);
        }
        return result;
    }

    private void updateClassTree(Map<String, ClassNode> classes) {
        if (classTreeView == null) return;
        TreeItem<String> root = new TreeItem<>(LanguageManager.get("tree.root"));
        root.setExpanded(true);
        Map<String, TreeItem<String>> pkgNodes = new TreeMap<>();
        for (ClassNode cn : classes.values()) {
            String pkg = cn.packageName != null ? cn.packageName : "default";
            TreeItem<String> pkgNode = pkgNodes.computeIfAbsent(pkg, k -> {
                TreeItem<String> n = new TreeItem<>(k);
                root.getChildren().add(n);
                return n;
            });
            pkgNode.getChildren().add(new TreeItem<>(cn.simpleName));
        }
        classTreeView.setRoot(root);
    }

    private void showClassDetails(String simpleName) {
        if (currentClasses == null || classDetailsArea == null) return;
        ClassNode cn = currentClasses.values().stream()
            .filter(c -> c.simpleName.equals(simpleName)).findFirst().orElse(null);
        if (cn == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Class: ").append(cn.fullName).append("\n");
        sb.append("Package: ").append(cn.packageName).append("\n");
        sb.append("File: ").append(cn.filePath).append("\n\n");
        sb.append("--- Fields ---\n");
        for (String f : cn.fields) sb.append("  ").append(f).append("\n");
        sb.append("\n--- Methods ---\n");
        for (String m : cn.methods) sb.append("  ").append(m).append("\n");
        classDetailsArea.setText(sb.toString());
    }

    @FXML private void onAnalyzeImpact() {
        if (graph == null || impactAnalysisField == null || impactResultArea == null) return;
        String cn = impactAnalysisField.getText().trim();
        if (cn.isEmpty()) return;
        String fullName = cn;
        if (currentClasses != null && !cn.contains(".")) {
            for (ClassNode node : currentClasses.values()) {
                if (node.simpleName.equals(cn)) { fullName = node.fullName; break; }
            }
        }
        List<Map<String, Object>> impacts = graph.analyzeImpact(fullName);
        if (impacts.isEmpty()) { impactResultArea.setText("No impact found for: " + cn); return; }
        StringBuilder sb = new StringBuilder("Impact for: " + cn + "\n\n");
        for (Map<String, Object> i : impacts) {
            sb.append("- ").append(i.get("affected_class")).append("\n");
            sb.append("  Type: ").append(i.get("dependency_type")).append("\n");
            sb.append("  ").append(i.get("description")).append("\n\n");
        }
        impactResultArea.setText(sb.toString());
    }

    @FXML private void onGenerateSkill() {
        try {
            skillGenerator.generateSkill(currentProjectPath, graph, "");
            skillInfoArea.setText("OK: " + currentProjectPath + "/.codeskill/");
        } catch (Exception e) { err(e); }
    }

    @FXML private void onRefreshGraph() { if (currentProjectPath != null) loadProject(currentProjectPath); }
    @FXML private void onSearch() {}
    @FXML private void onAutoMonitorChanged() {}
    @FXML private void onChangeSkillPath() {}
    @FXML private void onOpenSkillFolder() {}

    @FXML private void onOpenSettings() {
        try {
            javafx.fxml.FXMLLoader ldr = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent r = ldr.load();
            SettingsController c = ldr.getController();
            if (statusLabel != null) c.setOwnerScene(statusLabel.getScene());
            Stage s = new Stage();
            s.setTitle(LanguageManager.get("settings.title"));
            s.setScene(new Scene(r));
            s.initModality(Modality.APPLICATION_MODAL);
            s.showAndWait();
            if (statusLabel != null) ThemeManager.applyTheme(statusLabel.getScene());
        } catch (Exception e) { err(e); }
    }

    @FXML private void onClearCache() {}
    @FXML private void onExit() { Platform.exit(); }

    private void updateVizOptions() {
        if (visualizationTypeCombo == null) return;
        visualizationTypeCombo.setItems(FXCollections.observableArrayList(
            "Class Graph", "Force Graph", "Hierarchy", "Sunburst", "Heatmap"));
        visualizationTypeCombo.getSelectionModel().select(0);
        visualizationTypeCombo.setOnAction(e -> onVizChanged());
    }

    @FXML private void onVizChanged() {
        if (graphWebView == null) return;
        if (visualizationManager == null) { graphWebView.getEngine().loadContent(VisualizationManager.getPlaceholder(ThemeManager.getCurrentTheme())); return; }
        String s = visualizationTypeCombo.getValue(); if (s == null) return;
        String t = ThemeManager.getCurrentTheme(); String h = null;
        if (s.contains("Class Graph")) h = visualizationManager.generateCytoscapeGraph(t);
        else if (s.contains("Force")) h = visualizationManager.generateForceGraph(t);
        else if (s.contains("Hierarchy")) h = visualizationManager.generateHierarchicalGraph(t);
        else if (s.contains("Sunburst")) h = visualizationManager.generateSunburstChart(t);
        else if (s.contains("Heatmap")) h = visualizationManager.generateHeatmapMatrix(t);
        if (h != null) graphWebView.getEngine().loadContent(h);
    }

    @FXML private void onExportVisualization() {
        FileChooser c = new FileChooser(); c.setTitle("Export");
        c.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML", "*.html"));
        File f = c.showSaveDialog(new Stage());
        if (f != null && visualizationManager != null) {
            try {
                String t = ThemeManager.getCurrentTheme();
                String h = visualizationManager.generateCytoscapeGraph(t);
                if (h != null) Files.write(f.toPath(), h.getBytes());
            } catch (Exception e) { err(e); }
        }
    }

    @FXML private void onAbout() { info(LanguageManager.get("about.title"), LanguageManager.get("about.text")); }

    // ==================== METHOD ANALYSIS (ULTRA CLEAR SVG) ====================

    @FXML private void onGenerateMethodCallTree() {
        if (methodGraph == null) { showNotReady(); return; }
        String mid = pickMethod();
        if (mid == null) return;
        try {
            ultraViz = new UltraClearMethodVisualizationManager(methodGraph, ThemeManager.getCurrentTheme());
            graphWebView.getEngine().loadContent(ultraViz.generateUltraClearCallTree(mid, 5));
            if (statusLabel != null) statusLabel.setText("Call Tree: " + simpleName(mid));
        } catch (Exception e) { err(e); }
    }

    @FXML private void onGenerateImpactAnalysis() {
        if (methodGraph == null) { showNotReady(); return; }
        String mid = pickMethod();
        if (mid == null) return;
        try {
            ultraViz = new UltraClearMethodVisualizationManager(methodGraph, ThemeManager.getCurrentTheme());
            graphWebView.getEngine().loadContent(ultraViz.generateUltraClearImpactAnalysis(mid));
            if (statusLabel != null) statusLabel.setText("Impact: " + simpleName(mid));
        } catch (Exception e) { err(e); }
    }

    @FXML private void onDetectCycles() {
        if (methodGraph == null || methodVisualizer == null) { showNotReady(); return; }
        try {
            graphWebView.getEngine().loadContent(methodVisualizer.generateCycleDetection(ThemeManager.getCurrentTheme()));
            int n = methodGraph.findCyclicCalls().size();
            if (statusLabel != null) statusLabel.setText("Cycles found: " + n);
        } catch (Exception e) { err(e); }
    }

    @FXML private void onShowComplexityAnalysis() {
        if (methodGraph == null || methodVisualizer == null) { showNotReady(); return; }
        try {
            graphWebView.getEngine().loadContent(methodVisualizer.generateComplexityMatrix(ThemeManager.getCurrentTheme()));
            if (statusLabel != null) statusLabel.setText("Complexity analysis ready");
        } catch (Exception e) { err(e); }
    }

    @FXML private void onTrackFieldAccess() {
        if (methodGraph == null || methodVisualizer == null) { showNotReady(); return; }
        try {
            graphWebView.getEngine().loadContent(methodVisualizer.generateFieldAccessTracking(ThemeManager.getCurrentTheme()));
            if (statusLabel != null) statusLabel.setText("Field access tracking ready");
        } catch (Exception e) { err(e); }
    }

    // ==================== HELPERS ====================

    private void showNotReady() {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(LanguageManager.get("warning.title"));
        a.setHeaderText(null);
        a.setContentText(LanguageManager.get("warning.no.project"));
        a.showAndWait();
    }

    private String pickMethod() {
        if (methodGraph == null || methodGraph.getMethods().isEmpty()) return null;
        Map<String, String> displayToId = new LinkedHashMap<>();
        methodGraph.getMethods().values().stream()
            .sorted(Comparator.comparing(m -> simpleName(m.methodId)))
            .forEach(m -> {
                String d = simpleName(m.methodId);
                if (displayToId.containsKey(d)) d = shortCN(m.className) + "." + d;
                displayToId.put(d, m.methodId);
            });
        List<String> names = new ArrayList<>(displayToId.keySet());
        ChoiceDialog<String> dlg = new ChoiceDialog<>(names.get(0), names);
        dlg.setTitle("Select Method");
        dlg.setHeaderText("Choose a method to visualize");
        dlg.setContentText("Method:");
        dlg.setWidth(550);
        dlg.setResizable(true);
        return dlg.showAndWait().map(displayToId::get).orElse(null);
    }

    private static String simpleName(String id) {
        if (id == null) return "?";
        int h = id.indexOf('#');
        if (h > 0) {
            String r = id.substring(h + 1);
            int p = r.indexOf('(');
            return p > 0 ? r.substring(0, p) + "()" : r + "()";
        }
        int d = id.lastIndexOf('.');
        return d > 0 ? id.substring(d + 1) : id;
    }

    private static String shortCN(String fqcn) {
        if (fqcn == null) return "";
        int d = fqcn.lastIndexOf('.');
        return d > 0 ? fqcn.substring(d + 1) : fqcn;
    }

    private void info(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t);
        a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
    private void err(Exception e) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle("Error");
        a.setHeaderText(null); a.setContentText(e.getMessage() != null ? e.getMessage() : e.toString());
        a.showAndWait();
    }
}