package com.od.TestSplitter.Basic.Intermediate;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class IntermediateSystemClass {
    private CompilationUnit cu;
    private String pathToClassFile;
    private String className;

    public IntermediateSystemClass(CompilationUnit cu, String pathToClassFile, String className) {
        this.cu = cu;
        this.pathToClassFile = pathToClassFile;
        this.className = className;
    }

    public void process() {
        IntermediateSysMethodGenerator intermediateSysMethodGenerator = new IntermediateSysMethodGenerator();

        if (!cu.getClassByName(className).isPresent())
            return;

        for (MethodDeclaration m: cu.getClassByName(className).get().getMethods()) {
            m.accept(intermediateSysMethodGenerator, className);
        }
    }

    public void writeIntermediateClassToFile() {
        try {
            FileWriter fw = new FileWriter(new File(pathToClassFile));
            fw.append(cu.toString().replaceAll("ı", "i"));
            fw.flush();
            fw.close();
        } catch (IOException e) {
            System.err.println(className + ": class cannot be written to disk.");
        }
    }
}
