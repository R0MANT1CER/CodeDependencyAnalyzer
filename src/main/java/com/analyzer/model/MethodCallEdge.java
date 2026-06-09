package com.analyzer.model;

/**
 * 方法调用关系边
 */
public class MethodCallEdge {
    public String fromMethod;         // 调用者
    public String toMethod;           // 被调用者
    public String callType;           // INVOKE, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE
    public int lineNumber;            // 调用所在行号
    public int callCount;             // 调用次数（同一位置多次）
    public boolean isRecursive;       // 是否递归调用
    public String description;        // 调用描述
    
    public MethodCallEdge(String fromMethod, String toMethod, String callType) {
        this.fromMethod = fromMethod;
        this.toMethod = toMethod;
        this.callType = callType;
        this.callCount = 1;
        this.isRecursive = false;
    }
    
    @Override
    public String toString() {
        return fromMethod + " -> " + toMethod + " (" + callType + ")";
    }
}