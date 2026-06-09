package com.analyzer.graph;

import com.analyzer.model.*;
import java.util.*;

/**
 * 鏂规硶绾т緷璧栧浘 - 缁嗙矑搴︿緷璧栧垎鏋?
 */
public class MethodGraph {
    private Map<String, MethodNode> methods;              // 鎵€鏈夋柟娉?
    private Map<String, FieldNode> fields;                // 鎵€鏈夊瓧娈?
    private List<MethodCallEdge> methodCallEdges;         // 鏂规硶璋冪敤鍏崇郴
    private List<FieldAccessEdge> fieldAccessEdges;       // 瀛楁璁块棶鍏崇郴
    
    private Map<String, List<String>> methodToCallers;    // 鏂规硶 -> 璋冪敤鑰呭垪琛?
    private Map<String, List<String>> methodToCallees;    // 鏂规硶 -> 琚皟鐢ㄨ€呭垪琛?
    
    public MethodGraph() {
        this.methods = new HashMap<>();
        this.fields = new HashMap<>();
        this.methodCallEdges = new ArrayList<>();
        this.fieldAccessEdges = new ArrayList<>();
        this.methodToCallers = new HashMap<>();
        this.methodToCallees = new HashMap<>();
    }
    
    /**
     * 娣诲姞鏂规硶鑺傜偣
     */
    public void addMethod(MethodNode method) {
        methods.put(method.methodId, method);
    }
    
    /**
     * 娣诲姞瀛楁鑺傜偣
     */
    public void addField(FieldNode field) {
        fields.put(field.fieldId, field);
    }
    
    /**
     * 娣诲姞鏂规硶璋冪敤鍏崇郴
     */
    public void addMethodCall(String fromMethod, String toMethod, String callType) {
        MethodNode caller = methods.get(fromMethod);
        MethodNode callee = methods.get(toMethod);
        
        MethodCallEdge edge = new MethodCallEdge(fromMethod, toMethod, callType);
        methodCallEdges.add(edge);
        
        methodToCallers.computeIfAbsent(toMethod, k -> new ArrayList<>()).add(fromMethod);
        methodToCallees.computeIfAbsent(fromMethod, k -> new ArrayList<>()).add(toMethod);
        
        if (caller != null) caller.callMethods.add(toMethod);
        if (callee != null) { callee.calledByMethods.add(fromMethod); callee.invokeCount++; }
    }
    
    /**
     * 娣诲姞瀛楁璁块棶鍏崇郴
     */
    public void addFieldAccess(String method, String field, String accessType) {
        MethodNode methodNode = methods.get(method);
        FieldNode fieldNode = fields.get(field);
        
        if (methodNode != null && fieldNode != null) {
            if ("READ".equals(accessType)) {
                methodNode.accessFields.add(field);
                fieldNode.readByMethods.add(method);
                fieldNode.readCount++;
            } else if ("WRITE".equals(accessType)) {
                methodNode.accessFields.add(field);
                fieldNode.writtenByMethods.add(method);
                fieldNode.writeCount++;
            }
            
            FieldAccessEdge edge = new FieldAccessEdge(method, field, accessType);
            fieldAccessEdges.add(edge);
        }
    }
    
    /**
     * 鑾峰彇鏂规硶鐨勬墍鏈夎皟鐢ㄨ€呴摼璺?
     */
    public List<List<String>> getCallChain(String methodId, int maxDepth) {
        List<List<String>> chains = new ArrayList<>();
        List<String> currentChain = new ArrayList<>();
        dfsCallChain(methodId, currentChain, chains, maxDepth, new HashSet<>());
        return chains;
    }
    
    private void dfsCallChain(String methodId, List<String> currentChain, 
                             List<List<String>> result, int depth, Set<String> visited) {
        if (depth <= 0 || visited.contains(methodId)) {
            return;
        }
        
        currentChain.add(methodId);
        visited.add(methodId);
        
        List<String> callers = methodToCallers.getOrDefault(methodId, new ArrayList<>());
        if (callers.isEmpty()) {
            result.add(new ArrayList<>(currentChain));
        } else {
            for (String caller : callers) {
                dfsCallChain(caller, currentChain, result, depth - 1, visited);
            }
        }
        
        currentChain.remove(currentChain.size() - 1);
        visited.remove(methodId);
    }
    
    /**
     * 鑾峰彇鏂规硶璋冪敤鏍戯紙鎵€鏈夎璋冪敤鐨勬柟娉曪級
     */
    public Map<String, Object> getCallTree(String methodId, int maxDepth) {
        Map<String, Object> tree = new HashMap<>();
        tree.put("id", methodId);
        tree.put("label", extractMethodName(methodId));
        tree.put("depth", 0);
        
        List<Map<String, Object>> children = new ArrayList<>();
        buildCallTree(methodId, children, maxDepth, 0, new HashSet<>());
        tree.put("children", children);
        
        return tree;
    }
    
    private void buildCallTree(String methodId, List<Map<String, Object>> children, 
                              int maxDepth, int currentDepth, Set<String> visited) {
        if (currentDepth >= maxDepth || visited.contains(methodId)) {
            return;
        }
        
        visited.add(methodId);
        
        MethodNode method = methods.get(methodId);
        if (method != null) {
            for (String callee : method.callMethods) {
                if (!visited.contains(callee)) {
                    Map<String, Object> child = new HashMap<>();
                    child.put("id", callee);
                    child.put("label", extractMethodName(callee));
                    child.put("depth", currentDepth + 1);
                    
                    List<Map<String, Object>> grandChildren = new ArrayList<>();
                    buildCallTree(callee, grandChildren, maxDepth, currentDepth + 1, visited);
                    child.put("children", grandChildren);
                    
                    children.add(child);
                }
            }
        }
    }
    
    /**
     * 鑾峰彇褰卞搷鍒嗘瀽 - 淇敼姝ゆ柟娉曚細褰卞搷鍝簺鏂规硶
     */
    public Set<String> getImpactAnalysis(String methodId) {
        Set<String> impacted = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.offer(methodId);
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            List<String> callers = methodToCallers.getOrDefault(current, new ArrayList<>());
            
            for (String caller : callers) {
                if (impacted.add(caller)) {
                    queue.offer(caller);
                }
            }
        }
        
        return impacted;
    }
    
    /**
     * 鑾峰彇鏂规硶鐨勫畬鏁磋皟鐢ㄩ摼璺紙鐩存帴+闂存帴锛?
     */
    public Set<String> getTransitiveCalls(String methodId) {
        Set<String> result = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.offer(methodId);
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            MethodNode method = methods.get(current);
            
            if (method != null) {
                for (String callee : method.callMethods) {
                    if (result.add(callee)) {
                        queue.offer(callee);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * 鎵惧嚭寰幆璋冪敤
     */
    public List<List<String>> findCyclicCalls() {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String method : methods.keySet()) {
            if (!visited.contains(method)) {
                findCyclesDFS(method, visited, recursionStack, new ArrayList<>(), cycles);
            }
        }
        
        return cycles;
    }
    
    private void findCyclesDFS(String methodId, Set<String> visited, Set<String> recursionStack,
                              List<String> path, List<List<String>> cycles) {
        visited.add(methodId);
        recursionStack.add(methodId);
        path.add(methodId);
        
        MethodNode method = methods.get(methodId);
        if (method != null) {
            for (String callee : method.callMethods) {
                if (!visited.contains(callee)) {
                    findCyclesDFS(callee, visited, recursionStack, path, cycles);
                } else if (recursionStack.contains(callee)) {
                    // 鎵惧埌涓€涓惊鐜?
                    int cycleStart = path.indexOf(callee);
                    List<String> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
                    cycle.add(callee);
                    cycles.add(cycle);
                }
            }
        }
        
        recursionStack.remove(methodId);
        path.remove(path.size() - 1);
    }
    
    /**
     * 鑾峰彇鏂规硶澶嶆潅搴︽寚鏍?
     */
    public Map<String, Object> getMethodMetrics(String methodId) {
        MethodNode method = methods.get(methodId);
        if (method == null) return new HashMap<>();
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("methodId", methodId);
        metrics.put("name", method.methodName);
        metrics.put("class", method.className);
        metrics.put("codeLines", method.codeLines);
        metrics.put("complexity", method.complexity);
        metrics.put("callCount", method.invokeCount);
        metrics.put("localVariables", method.localVariables);
        metrics.put("callMethods", method.callMethods.size());
        metrics.put("calledBy", method.calledByMethods.size());
        metrics.put("accessedFields", method.accessFields.size());
        
        return metrics;
    }
    
    private String extractMethodName(String methodId) {
        String[] parts = methodId.split("#");
        if (parts.length > 1) {
            String classAndMethod = parts[1];
            int parenIdx = classAndMethod.indexOf('(');
            return parenIdx > 0 ? classAndMethod.substring(0, parenIdx) : classAndMethod;
        }
        return methodId;
    }
    
    // Getters
    public Map<String, MethodNode> getMethods() { return methods; }
    public Map<String, FieldNode> getFields() { return fields; }
    public List<MethodCallEdge> getMethodCallEdges() { return methodCallEdges; }
    public List<FieldAccessEdge> getFieldAccessEdges() { return fieldAccessEdges; }
}
