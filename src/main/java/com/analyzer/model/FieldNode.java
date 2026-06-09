package com.analyzer.model;

import java.util.*;

/**
 * 字段节点 - 代表一个类的字段
 */
public class FieldNode {
    public String fieldId;            // 唯一标识 com.example.User#name
    public String className;          // 所属类
    public String fieldName;          // 字段名
    public String fieldType;          // 字段类型
    public String accessLevel;        // public/protected/private
    public boolean isStatic;          // 是否static
    public boolean isFinal;           // 是否final
    public int lineNumber;            // 代码行号
    
    // 访问关系
    public Set<String> readByMethods;         // 被哪些方法读取
    public Set<String> writtenByMethods;      // 被哪些方法写入
    
    // 统计
    public int readCount;                     // 被读取次数
    public int writeCount;                    // 被写入次数
    
    public FieldNode(String className, String fieldName, String fieldType) {
        this.className = className;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.fieldId = className + "#" + fieldName;
        
        this.readByMethods = new HashSet<>();
        this.writtenByMethods = new HashSet<>();
        this.readCount = 0;
        this.writeCount = 0;
    }
    
    @Override
    public String toString() {
        return fieldId + " (" + fieldType + ")";
    }
}