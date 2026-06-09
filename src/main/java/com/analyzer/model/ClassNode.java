package com.analyzer.model;

import java.util.ArrayList;
import java.util.List;

public class ClassNode {
    public String fullName;           // 完全限定名 com.example.User
    public String simpleName;         // 简单名 User
    public String packageName;        // 包名 com.example
    public String filePath;           // 文件路径
    public List<String> fields;
    // Method and Field level nodes
    public List<MethodNode> methodNodes = new ArrayList<>();
    public List<FieldNode> fieldNodes = new ArrayList<>();
       // 字段列表
    public List<String> methods;      // 方法列表
    public long lastModified;         // 最后修改时间

    public ClassNode(String fullName, String simpleName, String packageName, String filePath) {
        this.fullName = fullName;
        this.simpleName = simpleName;
        this.packageName = packageName;
        this.filePath = filePath;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.lastModified = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return simpleName + " (" + fullName + ")";
    }
}