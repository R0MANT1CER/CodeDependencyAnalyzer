# BugScope - Code Dependency Analyzer

> **Demo/Prototype Status** -- This is a work-in-progress tool. Several features are incomplete or have known bugs. See Known Issues below.

A JavaFX desktop application that scans Java projects, builds class-level and method-level dependency graphs, generates interactive visualizations (Sunburst, Heatmap, Force Graph, Chord Diagram, Call Tree, Impact Analysis), and supports Chinese/English localization.

## Features

### Core
- **Project Scanner** -- Recursively parses .java source files using JavaParser + ASM bytecode analysis
- **Class Dependency Graph** -- Directed graph of class relationships (field types, method params, local vars, imports, inheritance)
- **Method-Level Analysis** -- Extracts method calls, field accesses, and builds method-level dependency graph (ASM-based visitor)
- **File Watcher** -- Monitors file changes and auto-refreshes the dependency graph
- **Skill Export** -- Serializes the dependency graph to .codeskill JSON format

### Visualizations (Class-Level)
- **Force-Directed Graph** -- Interactive HTML/Cytoscape.js spider-web dependency visualization with drag, zoom, click-to-highlight
- **Sunburst Chart** -- Hierarchical package/class dependency view (SVG/Canvas)
- **Heatmap** -- Dependency density matrix heatmap
- **Chord Diagram** -- Cross-class dependency flow diagram
- **Hierarchical Tree** -- Layered dependency tree view

### Visualizations (Method-Level)
- **Method Call Tree** -- Depth-limited call tree from any method
- **Method Dependency Graph** -- Full method-to-method dependency network
- **Method Impact Analysis** -- What methods are impacted by changes to a given method

### UI
- JavaFX native desktop interface with resizable window
- Settings dialog: Theme (Light/Dark) + Language (English/SimplifiedChinese)
- Window size persistence

## Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 21 |
| UI Framework | JavaFX (OpenJFX) | 21.0.2 |
| Source Parsing | JavaParser | 3.25.1 |
| Bytecode Analysis | ASM | 9.6 |
| JSON | Gson | 2.10.1 |
| Visualization | Cytoscape.js / Canvas / SVG | CDN |
| Build | Maven + jpackage | -- |
| Logging | SLF4J | 2.0.7 |

## Build & Run

### Prerequisites
- JDK 21+
- Maven 3.8+
- JavaFX SDK (bundled via Maven)

### Compile
```
mvn clean package -DskipTests
```

### Run (JAR)
```
java -jar target/code-dependency-analyzer-1.0.0.jar
```

### Package as Native EXE (Windows)
```
jlink --module-path $JAVAFX_HOME/lib --add-modules javafx.controls,javafx.fxml,javafx.web --output fx-jre
jpackage --type app-image --name CodeDependencyAnalyzer --input target --main-jar code-dependency-analyzer-1.0.0.jar --dest dist --runtime-image fx-jre
```

## Known Issues

> This is a **demo/prototype**. The following features are incomplete or buggy:

### Visualization Bugs
- **Sunburst Chart**: Layout overlaps for deep package hierarchies; labels get truncated
- **Heatmap**: Cell sizes are uneven; color scale normalization is off
- **Force-Directed Graph**: Nodes can cluster together making edges unreadable at small zoom levels
- **Call Tree**: Only shows a single block, no visible edges between nodes when there is one class
- **Long Class Names**: Text overflows and gets cut off in graph nodes
- **Line/Edge Overcrowding**: When zoomed out, edges dominate the view and node labels become unreadable

### Functionality Gaps
- **Method Analysis**: Method descriptor matching is fragile -- does not resolve method signatures correctly in many cases, producing only basic matches
- **Language Switching**: Only a small subset of UI labels are translated; most UI text remains hardcoded in Chinese
- **Theme Switching**: Dark/Light toggle sometimes fails to apply to all UI components
- **Settings Persistence**: Theme and language settings are not reliably saved/restored between sessions
- **Search**: No search or filter functionality for the dependency graph
- **Auto-Monitor**: File watcher is implemented but not integrated into auto-refresh in the UI
- **Export**: .codeskill export generates partial/incomplete data for method-level edges
- **Error Handling**: Minimal error handling; the EXE may crash with No such file or directory for the .cfg file if the app directory is moved
- **Loading Performance**: Application startup and graph rendering are slow for moderate-to-large projects

### Configuration File Error
- The EXE looks for CodeDependencyAnalyzer.cfg in its app directory; if the directory is moved or renamed, it will crash on startup

## Notes
- This tool was built as a prototype for analyzing code dependencies and assessing change impact
- Visualizations use in-memory Canvas/SVG rendering, generated as standalone HTML files
- The method-level analysis (ASM bytecode visitor) is the most unstable component
- i18n coverage is ~5%; most of the UI is hardcoded in Chinese with English fallbacks

## License
MIT (demo/prototype -- use at your own risk)
