# BugScope - 代码依赖分析器

> **Demo/原型状态** -- 这是一个开发中的工具。部分功能尚未完成或存在已知 Bug。详见下方"已知问题"。

一款 JavaFX 桌面应用，用于扫描 Java 项目，构建类级别和方法级别的依赖图，生成交互式可视化图表（旭日图、热力图、力导向图、和弦图、调用树、影响分析），支持中英文切换。

## 功能特性

### 核心功能
- **项目扫描** -- 使用 JavaParser + ASM 字节码分析递归解析 .java 源文件
- **类依赖图** -- 有向图展示类关系（字段类型、方法参数、局部变量、导入、继承）
- **方法级分析** -- 提取方法调用、字段访问，构建方法级依赖图（基于 ASM 访问器）
- **文件监听** -- 监控文件变更并自动刷新依赖图
- **Skill 导出** -- 将依赖图序列化为 .codeskill JSON 格式

### 可视化图表（类级别）
- **力导向图** -- 交互式 HTML/Cytoscape.js 蜘蛛网依赖可视化，支持拖拽、缩放、点击高亮
- **旭日图 (Sunburst)** -- 分层包/类依赖视图（SVG/Canvas）
- **热力图 (Heatmap)** -- 依赖密度矩阵热力图
- **和弦图 (Chord)** -- 跨类依赖流向图
- **层级树 (Tree)** -- 分层依赖树视图

### 可视化图表（方法级别）
- **方法调用树** -- 从任意方法出发的深度限制调用树
- **方法依赖图** -- 完整的方法间依赖网络
- **方法影响分析** -- 修改某个方法会影响到哪些其他方法

### UI
- JavaFX 原生桌面界面，窗口可调整大小
- 设置对话框：主题切换（亮色/暗色）+ 语言切换（English/简体中文）
- 窗口尺寸记忆

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| UI 框架 | JavaFX (OpenJFX) | 21.0.2 |
| 源码解析 | JavaParser | 3.25.1 |
| 字节码分析 | ASM | 9.6 |
| JSON 处理 | Gson | 2.10.1 |
| 可视化 | Cytoscape.js / Canvas / SVG | CDN |
| 构建 | Maven + jpackage | -- |
| 日志 | SLF4J | 2.0.7 |

## 构建与运行

### 环境要求
- JDK 21+
- Maven 3.8+
- JavaFX SDK（通过 Maven 自动引入）

### 编译
```
mvn clean package -DskipTests
```

### 运行 (JAR)
```
java -jar target/code-dependency-analyzer-1.0.0.jar
```

### 打包为 EXE (Windows)
```
jlink --module-path $JAVAFX_HOME/lib --add-modules javafx.controls,javafx.fxml,javafx.web --output fx-jre
jpackage --type app-image --name CodeDependencyAnalyzer --input target --main-jar code-dependency-analyzer-1.0.0.jar --dest dist --runtime-image fx-jre
```

## 已知问题

> 这是一个 **demo/原型**。以下特性尚未完成或存在 Bug：

### 可视化 Bug
- **旭日图**：深层包结构存在重叠；标签被截断
- **热力图**：格子大小不均匀；颜色缩放标准化有偏差
- **力导向图**：节点会聚集在一起，缩小时边线难以辨认
- **调用树**：只有一个类时只显示单个方块，看不到连线
- **长类名**：文本溢出，在节点中被截断
- **线条/边线拥挤**：缩小时边线占满视图，节点标签无法辨认

### 功能缺失
- **方法分析**：方法描述符匹配脆弱，很多情况下无法正确解析方法签名，只能产生基础匹配
- **语言切换**：只有少量 UI 标签被翻译；大部分界面仍是硬编码中文
- **主题切换**：暗色/亮色切换有时无法应用到所有 UI 组件
- **设置持久化**：主题和语言设置无法在两次会话间可靠保存/恢复
- **搜索**：没有依赖图的搜索或过滤功能
- **自动监听**：文件监听已实现但未集成到 UI 的自动刷新中
- **导出**：.codeskill 导出对方法级边数据不完整
- **错误处理**：错误处理不够完善；移动 app 目录后 EXE 会因找不到 .cfg 文件崩溃
- **加载性能**：对于中大型项目，应用启动和图表渲染较慢

### 配置文件错误
- EXE 会在 app 目录下查找 CodeDependencyAnalyzer.cfg；如果目录被移动或重命名，启动时会报 `Error opening "...cfg" file: No such file or directory` 崩溃

## 注意事项
- 本工具是针对代码依赖分析和变更影响评估的原型
- 可视化使用内存 Canvas/SVG 渲染，生成为独立的 HTML 文件
- 方法级分析（ASM 字节码访问器）是最不稳定的组件
- 国际化覆盖约 5%；大部分 UI 硬编码为中文，部分有英文回退

## 许可证
MIT（demo/原型 -- 使用风险自负）
