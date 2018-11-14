package com.od.TestSplitter;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.od.TestSplitter.TestParser.TargetType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class MethodVisitorForSplit extends VoidVisitorAdapter<Object> {
    private Set<String> targetNames;
    private Set<String> splitTargets;
    private Map<String, String> fieldMap;
    private int generationInd;
    private int count;
    ClassOrInterfaceDeclaration cls;
    ArrayList<MethodDeclaration> originalMethodList;
    ArrayList<MethodDeclaration> generatedMethodList;
    ArrayList<GeneratedMethod> allGeneratedMethods;
    CompilationUnit cu;
    String path;
    public int methodCounter;

    public MethodVisitorForSplit(CompilationUnit cu,  String path, ClassOrInterfaceDeclaration cls,Set<String> targetNames,
        Set<String> splitTargets) {
        this.cu = cu;
        this.path = path;
        this.cls = cls;
        this.targetNames = targetNames;
        this.splitTargets = splitTargets;
        this.fieldMap = extractFieldMap(cls);
        this.generationInd = 1;
        this.originalMethodList = new ArrayList<>();
        this.generatedMethodList = new ArrayList<>();
        this.allGeneratedMethods = new ArrayList<>();
        this.count = 0;
    }

    @Override
    public void visit(MethodDeclaration method, Object arg) {
        String classAndMethodName = cls.getNameAsString() + "_" + method.getNameAsString();

        if (!method.getParentNode().get().equals(cls)) {
            return;
        }

        if (!checkTargetMethod(method)) {
            originalMethodList.add(method);
            return;
        }

        ModifiedMethod modifiedMethod = new ModifiedMethod(method, splitTargets, classAndMethodName, fieldMap);
        if (modifiedMethod.getSplitIndexes().size() == 1) {
            originalMethodList.add(method);
            return;
        }

        Map<Integer, MethodDeclaration> generatedMethods = modifiedMethod.generateMethods(generationInd);
        generationInd += generatedMethods.size();
        Map<Integer, GeneratedMethod>  generatedMethodsMap = new HashMap<>();

        // Creating method objects.
        count = 0;
        generatedMethods.forEach((i,m) -> {
            GeneratedMethod gm = new GeneratedMethod(m, classAndMethodName, modifiedMethod.getVariableMap(i), count++, fieldMap,false);
            generatedMethodsMap.put(i, gm);
            generatedMethodList.add(m);
            allGeneratedMethods.add(gm);
        });

        // Processing variables requirements for modified method (write statements)
        generatedMethodsMap.forEach((ind, m) -> {
            modifiedMethod.processGenerated(ind, m.getNeededVariables());
        });

        modifiedMethod.writeAllStatements();
        methodCounter += generatedMethodList.size();
        // TODO: Not sure about super
        super.visit(method, arg);
    }

    public void visitAll(HashMap<String, String> map) {
        allGeneratedMethods.forEach(m -> {
            m.addReadStatements(map, this);
        });
    }

    public void visitAllLast(String splittedClassPath) {
        allGeneratedMethods.forEach(m -> {
            m.finalizeAssertions(splittedClassPath);
        });
    }

    public void addGeneratedMethod(GeneratedMethod gm) {
        generatedMethodList.add(gm.getMethodDeclaration());
    }

    boolean checkTargetMethod(MethodDeclaration method) {
        boolean isGenerated = method.getName().toString().startsWith("generated");
        boolean isTest =  method.getAnnotations().size() > 0
            && method.getAnnotations().contains(new MarkerAnnotationExpr("Test"));

        boolean containsInnerClass = method.getBody().get().findAll(LocalClassDeclarationStmt.class).size() > 0;
        return !isGenerated && !containsInnerClass &&
            isTest &&
            (TestParser.targetType == TargetType.ALL_METHODS
                || targetNames.contains(method.getName().toString()));
    }
    /**
     * Constructs a fieldMap for the field variables of the test class. (fieldName -> fieldType)
     * @param cls classDeclaration to be processed.
     * @return map of class fields with values of types.
     */
    Map<String, String> extractFieldMap(ClassOrInterfaceDeclaration cls) {
        HashMap<String, String> fieldMap = new HashMap<>();

        for (FieldDeclaration fieldDeclaration: cls.getFields()) {
            if (!fieldDeclaration.isFinal()) {
                for (VariableDeclarator variableDeclarator: fieldDeclaration.getVariables()) {
                    fieldMap.put(variableDeclarator.getNameAsString(), variableDeclarator.getType().toString());
                }
            }
        }

        return fieldMap;
    }
}
