package com.analyzer.model;

/**
 * 字段访问关系边
 */
public class FieldAccessEdge {
    public String method;             // 访问方法
    public String field;              // 被访问字段
    public String accessType;         // WRITE 或 READ
    public int lineNumber;            // 访问所在行号
    public int accessCount;           // 访问次数
    
    public FieldAccessEdge(String method, String field, String accessType) {
        this.method = method;
        this.field = field;
        this.accessType = accessType;
        this.accessCount = 1;
    }
    
    @Override
    public String toString() {
        return method + " [" + accessType + "] " + field;
    }
}