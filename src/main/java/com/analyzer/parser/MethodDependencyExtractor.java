package com.analyzer.parser;

import com.analyzer.graph.MethodGraph;
import com.analyzer.model.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class MethodDependencyExtractor {
    private static final Logger logger = LoggerFactory.getLogger(MethodDependencyExtractor.class);
    private MethodGraph methodGraph;
    private JavaParser javaParser;
    private Map<String, String> simpleToFullName;

    public MethodDependencyExtractor() {
        this.methodGraph = new MethodGraph();
        this.javaParser = new JavaParser();
        this.simpleToFullName = new HashMap<>();
    }

    public MethodGraph extractDependencies(String projectPath, Map<String, ClassNode> classNodes) {
        methodGraph = new MethodGraph();
        for (ClassNode cn : classNodes.values()) simpleToFullName.put(cn.simpleName, cn.fullName);

        List<File> javaFiles = findJavaFiles(new File(projectPath));
        logger.info("Scanning {} Java files", javaFiles.size());

        for (File f : javaFiles) {
            try { registerMethodsAndFields(f); } 
            catch (Exception e) { logger.warn("Register: {}", f, e); }
        }
        logger.info("Methods: {}, Fields: {}", methodGraph.getMethods().size(), methodGraph.getFields().size());

        for (File f : javaFiles) {
            try { extractCallRelations(f, classNodes); } 
            catch (Exception e) { logger.warn("Extract: {}", f, e); }
        }
        logger.info("Call edges: {}, Field edges: {}", 
            methodGraph.getMethodCallEdges().size(), methodGraph.getFieldAccessEdges().size());
        return methodGraph;
    }

    private List<File> findJavaFiles(File dir) {
        List<File> result = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return result;
        for (File f : files) {
            if (f.isDirectory() && !f.getName().startsWith(".")) result.addAll(findJavaFiles(f));
            else if (f.isFile() && f.getName().endsWith(".java")) result.add(f);
        }
        return result;
    }

    private String findMethod(String className, String methodName) {
        String prefix = className + "#" + methodName;
        for (String key : methodGraph.getMethods().keySet()) {
            if (key.equals(prefix) || key.startsWith(prefix + "(")) return key;
        }
        return null;
    }

    private void registerMethodsAndFields(File javaFile) throws FileNotFoundException {
        CompilationUnit cu = javaParser.parse(javaFile).getResult()
            .orElseThrow(() -> new RuntimeException("Parse: " + javaFile));
        String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("default");

        for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            String clsName = pkg + "." + cls.getNameAsString();

            for (FieldDeclaration f : cls.getFields()) {
                for (VariableDeclarator v : f.getVariables()) {
                    FieldNode fn = new FieldNode(clsName, v.getNameAsString(), f.getCommonType().asString());
                    fn.accessLevel = f.getAccessSpecifier().asString();
                    fn.isStatic = f.isStatic(); fn.isFinal = f.isFinal();
                    methodGraph.addField(fn);
                }
            }

            for (MethodDeclaration m : cls.getMethods()) {
                String d = buildDesc(m);
                MethodNode mn = new MethodNode(clsName, m.getNameAsString(), d);
                mn.accessLevel = m.getAccessSpecifier().asString();
                mn.isStatic = m.isStatic(); mn.isAbstract = m.isAbstract();
                mn.returnType = m.getType().asString();
                for (Parameter p : m.getParameters()) mn.parameters.add(p.getType().asString());
                for (ReferenceType e : m.getThrownExceptions()) mn.exceptionThrows.add(e.asString());
                mn.codeLines = countLines(m); mn.complexity = calcComplexity(m); mn.localVariables = countVars(m);
                methodGraph.addMethod(mn);
            }
        }
    }

    private void extractCallRelations(File javaFile, Map<String, ClassNode> classNodes) throws FileNotFoundException {
        CompilationUnit cu = javaParser.parse(javaFile).getResult()
            .orElseThrow(() -> new RuntimeException("Parse: " + javaFile));
        String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("default");

        Map<String, String> imports = new HashMap<>();
        for (ImportDeclaration imp : cu.getImports()) {
            String full = imp.getNameAsString();
            imports.put(full.substring(full.lastIndexOf('.') + 1), full);
        }

        for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            String clsName = pkg + "." + cls.getNameAsString();

            // Build variable->type map
            Map<String, String> varTypes = new HashMap<>();
            for (FieldDeclaration f : cls.getFields()) {
                for (VariableDeclarator v : f.getVariables())
                    varTypes.put(v.getNameAsString(), f.getCommonType().asString());
            }
            for (MethodDeclaration m : cls.getMethods()) {
                for (Parameter p : m.getParameters())
                    varTypes.put(p.getNameAsString(), p.getType().asString());
            }

            for (MethodDeclaration m : cls.getMethods()) {
                String fromId = findMethod(clsName, m.getNameAsString());
                if (fromId == null) fromId = clsName + "#" + m.getNameAsString() + buildDesc(m);

                if (!m.getBody().isPresent()) continue;
                Statement body = m.getBody().get();

                // Method calls
                for (MethodCallExpr call : body.findAll(MethodCallExpr.class)) {
                    String calleeName = call.getNameAsString();
                    String scopeText = call.getScope().map(s -> s.toString()).orElse(null);
                    
                    String calleeClass = clsName;
                    if (scopeText != null) {
                        String resolved = resolveType(scopeText, imports, pkg, classNodes);
                        if (resolved == null && varTypes.containsKey(scopeText)) {
                            resolved = resolveType(varTypes.get(scopeText), imports, pkg, classNodes);
                        }
                        if (resolved != null) calleeClass = resolved;
                    }

                    String calleeId = findMethod(calleeClass, calleeName);
                    if (calleeId == null) calleeId = calleeClass + "#" + calleeName + "(*)";
                    methodGraph.addMethodCall(fromId, calleeId, "invoke");
                }

                // Field accesses
                for (FieldAccessExpr fa : body.findAll(FieldAccessExpr.class)) {
                    String fieldName = fa.getNameAsString();
                    String scopeText = fa.getScope().toString();
                    String owner = resolveType(scopeText, imports, pkg, classNodes);
                    if (owner == null && varTypes.containsKey(scopeText))
                        owner = resolveType(varTypes.get(scopeText), imports, pkg, classNodes);
                    if (owner == null) owner = clsName;
                    methodGraph.addFieldAccess(fromId, owner + "#" + fieldName, "READ");
                }

                // Constructor calls
                for (ObjectCreationExpr oe : body.findAll(ObjectCreationExpr.class)) {
                    String full = resolveType(oe.getTypeAsString(), imports, pkg, classNodes);
                    if (full != null) {
                        String ctor = findMethod(full, "<init>");
                        if (ctor == null) ctor = full + "#<init>(*)";
                        methodGraph.addMethodCall(fromId, ctor, "construct");
                    }
                }
            }
        }
    }

    private String buildDesc(MethodDeclaration m) {
        StringBuilder sb = new StringBuilder("(");
        for (Parameter p : m.getParameters()) {
            if (sb.length() > 1) sb.append(",");
            sb.append(p.getType().asString());
        }
        return sb.append(")").toString();
    }

    private String resolveType(String name, Map<String, String> imports, String pkg, Map<String, ClassNode> classes) {
        if (name == null) return null;
        String c = name.replaceAll("<.*>", "").replace("[]", "").trim();
        if (c.isEmpty() || c.matches("^(int|long|short|byte|float|double|boolean|char|void|String|Integer|Long|Double|Float|Boolean|Character|Byte|Short)$"))
            return null;
        if (c.contains(".") && classes.containsKey(c)) return c;
        if (c.contains(".")) return c;
        if (imports.containsKey(c) && classes.containsKey(imports.get(c))) return imports.get(c);
        if (imports.containsKey(c)) return imports.get(c);
        String same = pkg + "." + c;
        if (classes.containsKey(same)) return same;
        if (simpleToFullName.containsKey(c)) return simpleToFullName.get(c);
        return same;
    }

    private int countLines(MethodDeclaration m) {
        return m.getBody().map(b -> b.getEnd().map(e -> e.line).orElse(0) - b.getBegin().map(be -> be.line).orElse(0) + 1).orElse(0);
    }

    private int calcComplexity(MethodDeclaration m) {
        if (!m.getBody().isPresent()) return 1;
        Statement body = m.getBody().get();
        int cx = 1;
        cx += body.findAll(IfStmt.class).size();
        cx += body.findAll(ForStmt.class).size();
        cx += body.findAll(ForEachStmt.class).size();
        cx += body.findAll(WhileStmt.class).size();
        cx += body.findAll(DoStmt.class).size();
        cx += body.findAll(SwitchEntry.class).size();
        cx += body.findAll(ConditionalExpr.class).size();
        for (BinaryExpr be : body.findAll(BinaryExpr.class)) {
            if (be.getOperator() == BinaryExpr.Operator.AND || be.getOperator() == BinaryExpr.Operator.OR) cx++;
        }
        return cx;
    }

    private int countVars(MethodDeclaration m) {
        if (!m.getBody().isPresent()) return m.getParameters().size();
        int count = 0;
        for (VariableDeclarationExpr v : m.getBody().get().findAll(VariableDeclarationExpr.class))
            count += v.getVariables().size();
        return count + m.getParameters().size();
    }

    public MethodGraph getMethodGraph() { return methodGraph; }
}
