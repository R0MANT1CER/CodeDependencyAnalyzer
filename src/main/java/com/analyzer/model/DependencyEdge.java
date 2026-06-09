package com.analyzer.model;

public class DependencyEdge {
    public String source;          // 源类
    public String target;          // 目标类
    public String type;            // extends/implements/field_type/param_type/return_type/method_body_ref
    public int lineNumber;         // 行号
    public String context;         // 描述

    public DependencyEdge(String source, String target, String type, int lineNumber, String context) {
        this.source = source;
        this.target = target;
        this.type = type;
        this.lineNumber = lineNumber;
        this.context = context;
    }

    @Override
    public String toString() {
        return String.format("%s -> %s (%s) [line %d]", source, target, type, lineNumber);
    }
}
