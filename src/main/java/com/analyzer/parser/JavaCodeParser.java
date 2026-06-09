package com.analyzer.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.analyzer.model.ClassNode;
import com.analyzer.model.DependencyEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class JavaCodeParser {
    private static final Logger logger = LoggerFactory.getLogger(JavaCodeParser.class);
    private JavaParser javaParser = new JavaParser();
    private Map<String, String> simpleNameToFullName = new HashMap<>();  // User -> com.example.User

    /**
     * 扫描整个项目文件夹
     */
    public Map<String, ClassNode> scanProject(String projectPath) throws Exception {
        Map<String, ClassNode> classes = new HashMap<>();
        
        File projectDir = new File(projectPath);
        List<File> javaFiles = findAllJavaFiles(projectDir);
        
        logger.info("找到 {} 个Java文件", javaFiles.size());
        
        // 第一遍：解析所有类定义，构建名字映射
        for (File javaFile : javaFiles) {
            try {
                parseClassDefinitions(javaFile, classes);
            } catch (Exception e) {
                logger.warn("解析文件失败: {}", javaFile.getAbsolutePath(), e);
            }
        }
        
        // 构建简单名到完全限定名的映射
        for (ClassNode classNode : classes.values()) {
            simpleNameToFullName.put(classNode.simpleName, classNode.fullName);
        }
        
        return classes;
    }

    /**
     * 递归找所有Java文件
     */
    private List<File> findAllJavaFiles(File dir) {
        List<File> javaFiles = new ArrayList<>();
        File[] files = dir.listFiles();
        
        if (files == null) return javaFiles;
        
        for (File file : files) {
            if (file.isDirectory() && !file.getName().startsWith(".")) {
                javaFiles.addAll(findAllJavaFiles(file));
            } else if (file.isFile() && file.getName().endsWith(".java")) {
                javaFiles.add(file);
            }
        }
        
        return javaFiles;
    }

    /**
     * 解析单个Java文件中的类定义
     */
    private void parseClassDefinitions(File javaFile, Map<String, ClassNode> classes) throws FileNotFoundException {
        CompilationUnit cu = javaParser.parse(javaFile).getResult().orElseThrow(() -> new RuntimeException("Failed to parse: " + javaFile));
        
        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("default");
        
        // 找出所有类/接口定义
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            String simpleName = classDecl.getNameAsString();
            String fullName = packageName + "." + simpleName;
            
            ClassNode classNode = new ClassNode(fullName, simpleName, packageName, javaFile.getAbsolutePath());
            
            // 提取字段
            classDecl.getFields().forEach(field -> 
                classNode.fields.add(field.getVariable(0).getNameAsString())
            );
            
            // 提取方法
            classDecl.getMethods().forEach(method -> 
                classNode.methods.add(method.getNameAsString())
            );
            
            classes.put(fullName, classNode);
            logger.debug("解析到类: {}", fullName);
        });
    }

    /**
     * 提取文件中的所有依赖关系
     */
    public List<DependencyEdge> extractDependencies(File javaFile, Map<String, ClassNode> classes) throws FileNotFoundException {
        List<DependencyEdge> edges = new ArrayList<>();
        CompilationUnit cu = javaParser.parse(javaFile).getResult().orElseThrow(() -> new RuntimeException("Failed to parse: " + javaFile));
        
        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("default");
        
        // 构建import映射
        Map<String, String> imports = new HashMap<>();
        for (ImportDeclaration importDecl : cu.getImports()) {
            String importName = importDecl.getNameAsString();
            String simpleName = importName.substring(importName.lastIndexOf('.') + 1);
            imports.put(simpleName, importName);
        }
        
        // 找所有类/接口定义
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            String sourceClass = packageName + "." + classDecl.getNameAsString();
            
            // 提取继承关系
            classDecl.getExtendedTypes().forEach(extendedType -> {
                String targetClass = resolveClassName(extendedType.getNameAsString(), imports, packageName, classes);
                if (targetClass != null) {
                    edges.add(new DependencyEdge(
                            sourceClass,
                            targetClass,
                            "extends",
                            extendedType.getBegin().map(pos -> pos.line).orElse(-1),
                            sourceClass + " extends " + targetClass
                    ));
                }
            });
            
            // 提取实现关系
            classDecl.getImplementedTypes().forEach(implementedType -> {
                String targetClass = resolveClassName(implementedType.getNameAsString(), imports, packageName, classes);
                if (targetClass != null) {
                    edges.add(new DependencyEdge(
                            sourceClass,
                            targetClass,
                            "implements",
                            implementedType.getBegin().map(pos -> pos.line).orElse(-1),
                            sourceClass + " implements " + targetClass
                    ));
                }
            });
            
            // 提取字段类型依赖
            classDecl.getFields().forEach(field -> {
                String fieldType = field.getElementType().asString();
                String targetClass = resolveClassName(fieldType, imports, packageName, classes);
                if (targetClass != null && !targetClass.equals(sourceClass)) {
                    edges.add(new DependencyEdge(
                            sourceClass,
                            targetClass,
                            "field_type",
                            field.getBegin().map(pos -> pos.line).orElse(-1),
                            "字段 " + field.getVariable(0).getNameAsString() + " 的类型为 " + fieldType
                    ));
                }
            });
            
            // 提取方法参数和返回类型
            classDecl.getMethods().forEach(method -> {
                // 返回类型
                String returnType = method.getType().asString();
                String returnClass = resolveClassName(returnType, imports, packageName, classes);
                if (returnClass != null && !returnClass.equals(sourceClass)) {
                    edges.add(new DependencyEdge(
                            sourceClass,
                            returnClass,
                            "return_type",
                            method.getBegin().map(pos -> pos.line).orElse(-1),
                            "方法 " + method.getNameAsString() + " 的返回类型"
                    ));
                }
                
                // 参数类型
                method.getParameters().forEach(param -> {
                    String paramType = param.getType().asString();
                    String paramClass = resolveClassName(paramType, imports, packageName, classes);
                    if (paramClass != null && !paramClass.equals(sourceClass)) {
                        edges.add(new DependencyEdge(
                                sourceClass,
                                paramClass,
                                "param_type",
                                param.getBegin().map(pos -> pos.line).orElse(-1),
                                "方法 " + method.getNameAsString() + " 的参数类型"
                        ));
                    }
                });
                
                // 方法体内的类实例化和方法调用
                method.getBody().ifPresent(body -> {
                    // 找new表达式
                    body.findAll(ObjectCreationExpr.class).forEach(newExpr -> {
                        String createdType = newExpr.getTypeAsString();
                        String createdClass = resolveClassName(createdType, imports, packageName, classes);
                        if (createdClass != null && !createdClass.equals(sourceClass)) {
                            edges.add(new DependencyEdge(
                                    sourceClass,
                                    createdClass,
                                    "method_body_ref",
                                    newExpr.getBegin().map(pos -> pos.line).orElse(-1),
                                    "方法 " + method.getNameAsString() + " 中创建了 " + createdType + " 对象"
                            ));
                        }
                    });
                });
            });
        });
        
        return edges;
    }

    /**
     * 解析类名（处理简单名、完全限定名、generic等）
     */
    private String resolveClassName(String className, Map<String, String> imports, 
                                   String packageName, Map<String, ClassNode> classes) {
        // 移除泛型参数
        if (className.contains("<")) {
            className = className.substring(0, className.indexOf('<'));
        }
        
        // 移除数组标记
        className = className.replace("[]", "");
        
        // 基本类型忽略
        if (isPrimitiveType(className)) {
            return null;
        }
        
        // 已经是完全限定名
        if (className.contains(".")) {
            return classes.containsKey(className) ? className : null;
        }
        
        // 从import中查找
        if (imports.containsKey(className)) {
            String fullName = imports.get(className);
            return classes.containsKey(fullName) ? fullName : null;
        }
        
        // 尝试同包类
        String samePkgName = packageName + "." + className;
        if (classes.containsKey(samePkgName)) {
            return samePkgName;
        }
        
        // 从之前的扫描结果中查找
        if (simpleNameToFullName.containsKey(className)) {
            return simpleNameToFullName.get(className);
        }
        
        return null;
    }

    private boolean isPrimitiveType(String type) {
        return type.matches("^(int|long|short|byte|float|double|boolean|char|void|String)$");
    }
}
