package com.analyzer.skill;

import com.analyzer.graph.DependencyGraph;
import com.analyzer.model.ClassNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class SkillGenerator {
    private static final Logger logger = LoggerFactory.getLogger(SkillGenerator.class);
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 生成Skill文件
     */
    public void generateSkill(String projectPath, DependencyGraph graph, String modifiedClass) {
        try {
            // 创建.codeskill目录
            String skillDir = projectPath + File.separator + ".codeskill";
            new File(skillDir).mkdirs();
            
            // 生成dependency_graph.json
            generateDependencyGraphJson(skillDir, graph);
            
            // 生成impact_analysis.txt
            if (modifiedClass != null) {
                generateImpactAnalysis(skillDir, graph, modifiedClass);
            }
            
            // 生成ai_context.json
            if (modifiedClass != null) {
                generateAIContext(skillDir, graph, modifiedClass, projectPath);
            }
            
            logger.info("✓ Skill文件已生成到: {}", skillDir);
        } catch (Exception e) {
            logger.error("生成Skill文件失败", e);
        }
    }

    /**
     * 生成依赖图JSON
     */
    private void generateDependencyGraphJson(String skillDir, DependencyGraph graph) throws IOException {
        Map<String, Object> skillData = new HashMap<>();
        
        skillData.put("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date()));
        skillData.put("total_classes", graph.getNodes().size());
        skillData.put("total_dependencies", graph.getEdges().size());
        
        // 类列表
        List<Map<String, Object>> classes = new ArrayList<>();
        for (ClassNode node : graph.getNodes().values()) {
            Map<String, Object> classInfo = new HashMap<>();
            classInfo.put("id", node.fullName);
            classInfo.put("name", node.simpleName);
            classInfo.put("package", node.packageName);
            classInfo.put("file", node.filePath);
            classInfo.put("fields", node.fields);
            classInfo.put("methods", node.methods);
            classes.add(classInfo);
        }
        skillData.put("classes", classes);
        
        // 依赖关系列表
        List<Map<String, Object>> dependencies = new ArrayList<>();
        for (var edge : graph.getEdges()) {
            Map<String, Object> depInfo = new HashMap<>();
            depInfo.put("from", edge.source);
            depInfo.put("to", edge.target);
            depInfo.put("type", edge.type);
            depInfo.put("line", edge.lineNumber);
            depInfo.put("description", edge.context);
            dependencies.add(depInfo);
        }
        skillData.put("dependencies", dependencies);
        
        // 写入文件
        String filePath = skillDir + File.separator + "dependency_graph.json";
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(skillData, writer);
        }
        logger.info("生成: {}", filePath);
    }

    /**
     * 生成影响分析文本
     */
    private void generateImpactAnalysis(String skillDir, DependencyGraph graph, String modifiedClass) throws IOException {
        StringBuilder sb = new StringBuilder();
        
        sb.append("============ 影响分析报告 ============\n");
        sb.append("修改的类: ").append(modifiedClass).append("\n");
        sb.append("生成时间: ").append(new Date()).append("\n\n");
        
        // 直接受影响的类
        Set<String> directDependents = graph.findDirectDependents(modifiedClass);
        sb.append("【直接受影响的类】(").append(directDependents.size()).append("个)\n");
        for (String dependent : directDependents) {
            sb.append("  - ").append(dependent).append("\n");
        }
        sb.append("\n");
        
        // 间接受影响的类
        Set<String> allDependents = graph.findAllDependents(modifiedClass);
        allDependents.removeAll(directDependents);  // 只保留间接的
        sb.append("【间接受影响的类】(").append(allDependents.size()).append("个)\n");
        for (String dependent : allDependents) {
            sb.append("  - ").append(dependent).append("\n");
        }
        sb.append("\n");
        
        // 详细的影响点
        sb.append("【详细影响分析】\n");
        var impacts = graph.analyzeImpact(modifiedClass);
        for (var impact : impacts) {
            sb.append("  - ").append(impact.get("affected_class")).append("\n");
            sb.append("    位置: ").append(impact.get("source_file")).append(" line ").append(impact.get("line_number")).append("\n");
            sb.append("    类型: ").append(impact.get("dependency_type")).append("\n");
            sb.append("    描述: ").append(impact.get("description")).append("\n\n");
        }
        
        // 写入文件
        String filePath = skillDir + File.separator + "impact_analysis.txt";
        Files.write(Paths.get(filePath), sb.toString().getBytes());
        logger.info("生成: {}", filePath);
    }

    /**
     * 生成AI修复上下文
     */
    private void generateAIContext(String skillDir, DependencyGraph graph, String modifiedClass, String projectPath) throws IOException {
        Map<String, Object> aiContext = new HashMap<>();
        
        aiContext.put("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date()));
        aiContext.put("modified_class", modifiedClass);
        
        // 受影响的类
        Set<String> affectedClasses = graph.findDirectDependents(modifiedClass);
        aiContext.put("affected_classes", new ArrayList<>(affectedClasses));
        
        // 详细影响信息
        List<Map<String, Object>> detailedImpacts = new ArrayList<>();
        var impacts = graph.analyzeImpact(modifiedClass);
        for (var impact : impacts) {
            detailedImpacts.add(impact);
        }
        aiContext.put("detailed_impacts", detailedImpacts);
        
        // AI修复建议
        Map<String, String> repairSuggestions = new HashMap<>();
        repairSuggestions.put("summary", "修改了" + graph.getNodes().get(modifiedClass).simpleName + 
                            "类，以下是所有受影响的地方，需要逐一修复");
        repairSuggestions.put("recommendation", "请依次检查以下文件中标记的行，确保兼容性");
        aiContext.put("repair_suggestions", repairSuggestions);
        
        // 写入文件
        String filePath = skillDir + File.separator + "ai_context.json";
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(aiContext, writer);
        }
        logger.info("生成: {}", filePath);
    }
}
