package com.analyzer.graph;

import com.analyzer.model.ClassNode;
import com.analyzer.model.DependencyEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class DependencyGraph {
    private static final Logger logger = LoggerFactory.getLogger(DependencyGraph.class);
    
    // 所有类节点
    private Map<String, ClassNode> nodes = new HashMap<>();
    
    // 所有依赖关系
    private List<DependencyEdge> edges = new ArrayList<>();
    
    // 快速查询：正向依赖（A依赖B）
    private Map<String, List<DependencyEdge>> forwardEdges = new HashMap<>();
    
    // 快速查询：反向依赖（谁依赖A）
    private Map<String, List<DependencyEdge>> reverseEdges = new HashMap<>();

    /**
     * 添加类节点
     */
    public void addNode(String fullName, ClassNode classNode) {
        nodes.put(fullName, classNode);
        forwardEdges.putIfAbsent(fullName, new ArrayList<>());
        reverseEdges.putIfAbsent(fullName, new ArrayList<>());
    }

    /**
     * 添加依赖关系
     */
    public void addEdge(DependencyEdge edge) {
        // 避免重复
        if (edges.stream().anyMatch(e -> 
            e.source.equals(edge.source) && 
            e.target.equals(edge.target) && 
            e.type.equals(edge.type))) {
            return;
        }
        
        edges.add(edge);
        
        forwardEdges.computeIfAbsent(edge.source, k -> new ArrayList<>()).add(edge);
        reverseEdges.computeIfAbsent(edge.target, k -> new ArrayList<>()).add(edge);
    }

    /**
     * 查询：谁引用了这个类？（直接依赖）
     */
    public Set<String> findDirectDependents(String targetClass) {
        return reverseEdges.getOrDefault(targetClass, new ArrayList<>())
                .stream()
                .map(edge -> edge.source)
                .collect(Collectors.toSet());
    }

    /**
     * 查询：这个类引用了谁？
     */
    public Set<String> findDependencies(String sourceClass) {
        return forwardEdges.getOrDefault(sourceClass, new ArrayList<>())
                .stream()
                .map(edge -> edge.target)
                .collect(Collectors.toSet());
    }

    /**
     * 查询：所有间接受影响的类（传递依赖）
     */
    public Set<String> findAllDependents(String targetClass) {
        Set<String> result = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        queue.add(targetClass);
        visited.add(targetClass);
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            
            for (DependencyEdge edge : reverseEdges.getOrDefault(current, new ArrayList<>())) {
                if (!visited.contains(edge.source)) {
                    visited.add(edge.source);
                    result.add(edge.source);
                    queue.add(edge.source);
                }
            }
        }
        
        return result;
    }

    /**
     * 查询：影响分析 - 修改某个类会影响哪些地方
     */
    public List<Map<String, Object>> analyzeImpact(String modifiedClass) {
        List<Map<String, Object>> impacts = new ArrayList<>();
        Set<String> affectedClasses = findDirectDependents(modifiedClass);
        
        for (String affectedClass : affectedClasses) {
            // 找出具体的引用位置
            List<DependencyEdge> relatedEdges = reverseEdges
                    .getOrDefault(modifiedClass, new ArrayList<>())
                    .stream()
                    .filter(edge -> edge.source.equals(affectedClass))
                    .collect(Collectors.toList());
            
            for (DependencyEdge edge : relatedEdges) {
                Map<String, Object> impact = new HashMap<>();
                impact.put("affected_class", affectedClass);
                impact.put("dependency_type", edge.type);
                impact.put("line_number", edge.lineNumber);
                impact.put("description", edge.context);
                impact.put("source_file", nodes.get(affectedClass).filePath);
                impacts.add(impact);
            }
        }
        
        return impacts;
    }

    // Getters
    public Map<String, ClassNode> getNodes() {
        return nodes;
    }

    public List<DependencyEdge> getEdges() {
        return edges;
    }

    public Map<String, List<DependencyEdge>> getReverseEdges() {
        return reverseEdges;
    }

    public Map<String, List<DependencyEdge>> getForwardEdges() {
        return forwardEdges;
    }

    /**
     * Get out-degree (number of classes this class depends on)
     */
    public int getOutDegree(String className) {
        return forwardEdges.getOrDefault(className, new ArrayList<>()).size();
    }

    /**
     * Get in-degree (number of classes that depend on this class)
     */
    public int getInDegree(String className) {
        return reverseEdges.getOrDefault(className, new ArrayList<>()).size();
    }

}
