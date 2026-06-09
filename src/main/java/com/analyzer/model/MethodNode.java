package com.analyzer.model;

import java.util.*;

/**
 * 方法节点 - 代表一个具体方法
 */
public class MethodNode {
    public String methodId;           // 唯一标识 com.example.User#getName()
    public String className;          // 所属类
    public String methodName;         // 方法名
    public String signature;          // 方法签名 getName()V
    public String returnType;         // 返回类型
    public List<String> parameters;   // 参数类型列表
    public int lineStart;             // 代码起始行
    public int lineEnd;               // 代码结束行
    public String accessLevel;        // public/protected/private
    public boolean isStatic;          // 是否static
    public boolean isAbstract;        // 是否abstract
    public int complexity;            // 圈复杂度
    
    // 依赖信息
    public Set<String> callMethods;           // 调用的方法
    public Set<String> calledByMethods;       // 被谁调用
    public Set<String> accessFields;          // 访问的字段
    public Set<String> accessedByMethods;     // 被谁访问字段
    public Set<String> exceptionThrows;       // 抛出的异常
    public Set<String> exceptionCatches;      // 捕获的异常
    
    // 统计信息
    public int invokeCount;                   // 被调用次数
    public int codeLines;                     // 代码行数
    public int localVariables;                // 局部变量数
    
    public MethodNode(String className, String methodName, String signature) {
        this.className = className;
        this.methodName = methodName;
        this.signature = signature;
        this.methodId = className + "#" + methodName + signature;
        
        this.callMethods = new HashSet<>();
        this.calledByMethods = new HashSet<>();
        this.accessFields = new HashSet<>();
        this.accessedByMethods = new HashSet<>();
        this.exceptionThrows = new HashSet<>();
        this.exceptionCatches = new HashSet<>();
        this.parameters = new ArrayList<>();
        this.invokeCount = 0;
    }
    
    @Override
    public String toString() {
        return methodId + " (Calls: " + callMethods.size() + 
               ", Called: " + calledByMethods.size() + ")";
    }
}