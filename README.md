# BugScope - Code Dependency Analyzer / 代码依赖分析器

> ⚠️ **Demo/Prototype Status / 原型状态** — This is a work-in-progress tool. Several features are incomplete or have known bugs. See [Known Issues](#known-issues) below.
> 这是一个开发中的工具，部分功能尚未完成或存在已知 Bug，详见下方已知问题。

A JavaFX desktop application that scans Java projects, builds class-level and method-level dependency graphs, generates interactive visualizations (Sunburst, Heatmap, Force Graph, Chord Diagram, Call Tree, Impact Analysis), and supports Chinese/English localization.

一款 JavaFX 桌面应用，用于扫描 Java 项目，构建类级别和方法级别的依赖图，生成交互式可视化图表，支持中英文切换。

## Features / 功能特性

### Core / 核心功能
- **Project Scanner / 项目扫描** — Recursively parses `.java` source files using JavaParser + ASM bytecode analysis / 使用 JavaParser + ASM 字节码分析递归解析源文件
- **Class Dependency Graph / 类依赖图** — Directed graph of class relationships (field types, method params, local vars, imports, inheritance) / 有向图展示类关系（字段类型、方法参数、局部变量、导入、继承）
- **Method-Level Analysis / 方法级分析** — Extracts method calls, field accesses, and builds method-level dependency graph (ASM-based visitor) / 提取方法调用、字段访问，构建方法级依赖图
- **File Watcher / 文件监听** — Monitors file changes and auto-refreshes the dependency graph / 监控文件变更并自动刷新依赖图
- **Skill Export / Skill 导出** — Serializes the dependency graph to `.codeskill` JSON format / 将依赖图序列化为 `.codeskill` JSON 格式
- **AI-Assisted Generation / AI 辅助生成** — Integrates AI-powered visualization generation and analysis / 集成 AI 辅助生成可视化图表和分析

### Visualizations / 可视化图表 (Class-Level / 类级别)
- **Force-Directed Graph / 力导向图** — Interactive HTML/Cytoscape.js spider-web dependency visualization with drag, zoom, click-to-highlight / 交互式蜘蛛网依赖可视化，支持拖拽、缩放、点击高亮
- **Sunburst Chart / 旭日图** — Hierarchical package/class dependency view (SVG/Canvas) / 分层包/类依赖视图
- **Heatmap / 热力图** — Dependency density matrix heatmap / 依赖密度矩阵热力图
- **Chord Diagram / 和弦图** — Cross-class dependency flow diagram / 跨类依赖流向图
- **Hierarchical Tree / 层级树** — Layered dependency tree view / 分层依赖树视图

### Visualizations / 可视化图表 (Method-Level / 方法级别)
- **Method Call Tree / 方法调用树** — Depth-limited call tree from any method / 从任意方法出发的深度限制调用树
- **Method Dependency Graph / 方法依赖图** — Full method-to-method dependency network / 完整的方法间依赖网络
- **Method Impact Analysis / 方法影响分析** — What methods are impacted by changes to a given method / 修改某个方法会影响到哪些其他方法

### UI / 界面
- JavaFX native desktop interface with resizable window / JavaFX 原生桌面界面，窗口可调整大小
- Settings dialog: Theme (Light/Dark) + Language (English/简体中文) / 设置对话框：主题切换 + 语言切换
- Window size persistence / 窗口尺寸记忆

## Tech Stack / 技术栈

| Component / 组件 | Technology / 技术 | Version / 版本 |
|-----------|-----------|---------|
| Language / 语言 | Java | 21 |
| UI Framework / UI 框架 | JavaFX (OpenJFX) | 21.0.2 |
| Source Parsing / 源码解析 | JavaParser | 3.25.1 |
| Bytecode Analysis / 字节码分析 | ASM | 9.6 |
| JSON | Gson | 2.10.1 |
| Visualization / 可视化 | Cytoscape.js / Canvas / SVG | CDN |
| Build / 构建 | Maven + jpackage | — |
| Logging / 日志 | SLF4J | 2.0.7 |

## Project Structure / 项目结构

```
src/main/java/com/analyzer/
├── Main.java                  # Application entry point / 应用入口 (JavaFX)
├── controller/
│   ├── MainController.java    # Primary UI controller / 主界面控制器
│   └── SettingsController.java# Settings dialog controller / 设置对话框控制器
├── model/
│   ├── ClassNode.java         # Class metadata model / 类元数据模型
│   ├── DependencyEdge.java    # Class-level edge model / 类级边模型
│   ├── MethodNode.java        # Method metadata model / 方法元数据模型
│   ├── MethodCallEdge.java    # Method call edge model / 方法调用边模型
│   ├── FieldNode.java         # Field metadata model / 字段元数据模型
│   └── FieldAccessEdge.java   # Field access edge model / 字段访问边模型
├── parser/
│   ├── JavaCodeParser.java    # JavaParser-based source scanner / 源码扫描器
│   ├── MethodDependencyExtractor.java  # Method extraction orchestrator / 方法提取编排
│   └── MethodDependencyVisitor.java    # ASM visitor for method analysis / ASM 方法分析访问器
├── graph/
│   ├── DependencyGraph.java   # Class-level directed graph / 类级有向图
│   └── MethodGraph.java       # Method-level directed graph / 方法级有向图
├── ui/
│   └── GraphVisualizer.java   # HTML/Cytoscape.js visualization generator / HTML 可视化生成器
├── util/
│   ├── Config.java            # Persistent configuration / 持久化配置
│   ├── ThemeManager.java      # Light/Dark theme switching / 主题切换管理
│   ├── LanguageManager.java   # i18n Chinese/English / 国际化中英文管理
│   ├── VisualizationManager.java         # Class-level chart generator / 类级图表生成器
│   ├── MethodVisualizationManager.java   # Method-level chart generator / 方法级图表生成器
│   └── UltraClearMethodVisualizationManager.java  # Enhanced method visualization / 增强方法可视化
├── monitor/
│   └── FileChangeMonitor.java # WatchService file watcher / 文件监听器
├── skill/
│   └── SkillGenerator.java   # .codeskill JSON exporter / .codeskill 导出器
└── visualization/engine/
    └── SuperClearLayoutEngine.java  # Layout algorithms for canvas/SVG / 布局算法引擎
```

## Build & Run / 构建与运行

### Prerequisites / 环境要求
- JDK 21+
- Maven 3.8+
- JavaFX SDK (bundled via Maven / 通过 Maven 自动引入)

### Compile / 编译
```bash
mvn clean package -DskipTests
```

### Run / 运行 (JAR)
```bash
java -jar target/code-dependency-analyzer-1.0.0.jar
```

### Package as Native EXE / 打包为 EXE (Windows)
```bash
# Create JavaFX runtime image / 创建 JavaFX 运行时镜像
jlink --module-path $JAVAFX_HOME/lib --add-modules javafx.controls,javafx.fxml,javafx.web --output fx-jre
# Package / 打包
jpackage --type app-image --name CodeDependencyAnalyzer --input target --main-jar code-dependency-analyzer-1.0.0.jar --dest dist --runtime-image fx-jre
```

## Known Issues / 已知问题

> This is a **demo/prototype**. The following features are incomplete or buggy.
> 这是一个 **demo/原型**。以下特性尚未完成或存在 Bug：

### Visualization Bugs / 可视化 Bug
- **Sunburst Chart / 旭日图**: Layout overlaps for deep package hierarchies; labels get truncated / 深层包结构存在重叠，标签被截断
- **Heatmap / 热力图**: Cell sizes are uneven; color scale normalization is off / 格子大小不均匀，颜色缩放有偏差
- **Force-Directed Graph / 力导向图**: Nodes can cluster together making edges unreadable at small zoom levels / 节点聚集，缩小时边线难以辨认
- **Call Tree / 调用树**: Only shows a single block, no visible edges between nodes when there is one class / 只有一个类时只显示单个方块
- **Long Class Names / 长类名**: Text overflows and gets cut off in graph nodes / 文本溢出被截断
- **Line/Edge Overcrowding / 线条拥挤**: When zoomed out, edges dominate the view and node labels become unreadable / 缩小时边线占满视图

### Functionality Gaps / 功能缺失
- **Method Analysis / 方法分析**: Method descriptor matching is fragile — does not resolve method signatures correctly in many cases / 方法描述符匹配脆弱，很多情况下无法正确解析
- **Language Switching / 语言切换**: Only a small subset of UI labels are translated; most UI text remains hardcoded in Chinese / 只有少量标签被翻译
- **Theme Switching / 主题切换**: Dark/Light toggle sometimes fails to apply to all UI components / 有时无法应用到所有组件
- **Settings Persistence / 设置持久化**: Theme and language settings are not reliably saved/restored between sessions / 设置无法可靠保存
- **Search / 搜索**: No search or filter functionality for the dependency graph / 没有搜索或过滤功能
- **Auto-Monitor / 自动监听**: File watcher is implemented but not integrated into auto-refresh in the UI / 文件监听未集成到自动刷新
- **Export / 导出**: `.codeskill` export generates partial/incomplete data for method-level edges / 导出数据不完整
- **Error Handling / 错误处理**: Minimal error handling; the EXE may crash with `No such file or directory` for the `.cfg` file if the app directory is moved / 移动目录后崩溃
- **Loading Performance / 加载性能**: Application startup and graph rendering are slow for moderate-to-large projects / 中大项目加载慢

### Configuration File Error / 配置文件错误
- The EXE looks for `CodeDependencyAnalyzer.cfg` in its app directory; if the directory is moved or renamed, it will crash on startup / 移动或重命名目录后启动崩溃

## Notes / 注意事项
- This tool was built as a prototype for analyzing code dependencies and assessing change impact / 本工具是针对代码依赖分析和变更影响评估的原型
- Visualizations use in-memory Canvas/SVG rendering, generated as standalone HTML files / 可视化使用内存渲染，生成为独立 HTML 文件
- The method-level analysis (ASM bytecode visitor) is the most unstable component / 方法级分析是最不稳定的组件
- i18n coverage is ~5%; most of the UI is hardcoded in Chinese with English fallbacks / 国际化覆盖约 5%

## License / 许可证
MIT (demo/prototype — use at your own risk / 原型项目，使用风险自负)
