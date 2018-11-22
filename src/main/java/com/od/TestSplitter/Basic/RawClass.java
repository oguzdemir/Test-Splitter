package com.od.TestSplitter.Basic;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.od.TestSplitter.Basic.Splitted.SplittedClass;
import com.od.TestSplitter.Basic.Intermediate.IntermediateSystemClass;
import com.od.TestSplitter.Config;
import com.od.TestSplitter.TestParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


public class RawClass {

    public static HashMap<MethodDeclaration, SplitInformation> splitInformationMap = new HashMap<>();
    public static int generatedMethodCounter;

    private CompilationUnit cu;
    private String pathToClassFile;
    private ClassOrInterfaceDeclaration classOrInterfaceDeclaration;
    private String className;
    private SplittedClass splittedClass;
    private IntermediateSystemClass intermediateSystemClass;


    public RawClass(CompilationUnit cu, String pathToClassFile, ClassOrInterfaceDeclaration classOrInterfaceDeclaration, String className) {
        this.cu = cu;
        this.pathToClassFile = pathToClassFile;
        this.classOrInterfaceDeclaration = classOrInterfaceDeclaration;
        this.className = className;
        generatedMethodCounter = 0;
    }


    public void process() {
        if (classOrInterfaceDeclaration.getModifiers().contains(Modifier.ABSTRACT) || !checkConcurrentImports() ||
            !cu.getClassByName(className).isPresent() || classOrInterfaceDeclaration.getImplementedTypes().size() > 0) {
            return;
        }

        HashMap<String, String> fieldMap = extractFieldMap();

        boolean found = false;
        for (MethodDeclaration methodDeclaration: cu.getClassByName(className).get().getMethods()) {
            SplitInformation splitInformation = methodDeclaration.accept(new InformationExtracter(), classOrInterfaceDeclaration);
            if (splitInformation == null) {
                continue;
            }

            found = true;
            splitInformation.fieldMap = fieldMap;
            splitInformationMap.put(splitInformation.belongingMethod, splitInformation);
        }

        // No split information found for any method.
        if (!found)
            return;

        String classPathForSplit = pathToClassFile.replace(Config.projectPath, Config.projectSplittedPath);
        String classPathForIntermediate = pathToClassFile.replace(Config.projectPath, Config.projectIntermediatePath);

        splittedClass = new SplittedClass(cu.clone(), classPathForSplit, className);
        intermediateSystemClass = new IntermediateSystemClass(cu.clone(), classPathForIntermediate, className);

        intermediateSystemClass.process();
        splittedClass.process();

    }

    private boolean checkConcurrentImports() {
        for(ImportDeclaration importDeclaration: cu.getImports()) {
            if (importDeclaration.toString().startsWith("import java.util.concurrent")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Constructs a fieldMap for the field variables of the test class. (fieldName -> fieldType)
     * @return map of class fields with values of types.
     */
    private HashMap<String, String> extractFieldMap() {
        HashMap<String, String> fieldMap = new HashMap<>();

        for (FieldDeclaration fieldDeclaration: classOrInterfaceDeclaration.getFields()) {
            if (!fieldDeclaration.isFinal()) {
                for (VariableDeclarator variableDeclarator: fieldDeclaration.getVariables()) {
                    fieldMap.put(variableDeclarator.getNameAsString(), variableDeclarator.getType().toString());
                }
            }
        }

        return fieldMap;
    }

    public void writeAllToFile() {
        if (splittedClass == null || intermediateSystemClass == null)
            return;

        splittedClass.writeSplittedClassToFile();
        intermediateSystemClass.writeIntermediateClassToFile();
    }
}


