package com.od.TestSplitter.Basic.Splitted;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SplittedClass {
    private CompilationUnit cu;
    private String pathToClassFile;
    private String className;
    private List<SplittedMethod> splittedMethods;

    public SplittedClass(CompilationUnit cu, String pathToClassFile, String className) {
        this.cu = cu;
        this.pathToClassFile = pathToClassFile;
        this.className = className;
        splittedMethods = new ArrayList<>();
    }


    public void process() {
        SplittedMethodGenerator splittedMethodGenerator = new SplittedMethodGenerator();

        if (!cu.getClassByName(className).isPresent())
            return;

        for (MethodDeclaration m: cu.getClassByName(className).get().getMethods()) {
            List<SplittedMethod> res = m.accept(splittedMethodGenerator, className);
            if (res != null)
                splittedMethods.addAll(res);
        }

        for (SplittedMethod sm: splittedMethods) {
            sm.process();
        }
    }


    public void writeSplittedClassToFile() {
        Set<String> splittedMethodNames = new HashSet<>();
        splittedMethods.forEach( m-> splittedMethodNames.add(m.getClassAndMethodName().split("_")[1]));
        ClassOrInterfaceDeclaration cls = cu.getClassByName(className).get();

        for (MethodDeclaration methodDeclaration: cls.getMethods()) {
            if (splittedMethodNames.contains(methodDeclaration.getNameAsString()))
                cls.remove(methodDeclaration);
        }

        splittedMethods.forEach( m -> m.postProcess());

        List<MethodDeclaration> generatedList = new ArrayList<>(splittedMethods.size());
        splittedMethods.forEach( m-> generatedList.add(m.getMethodDeclaration()));

        Collections.sort(generatedList, new Comparator<MethodDeclaration>() {
            @Override
            public int compare(MethodDeclaration o1, MethodDeclaration o2) {
                Integer number1 = Integer.parseInt(o1.getNameAsString().replace("generatedU", ""));
                Integer number2 = Integer.parseInt(o2.getNameAsString().replace("generatedU", ""));
                return number1.compareTo(number2);
            }
        });

        generatedList.forEach(m-> cls.addMember(m));

        try {
            FileWriter fw = new FileWriter(new File(pathToClassFile));
            fw.append(cu.toString().replaceAll("ı", "i"));
            fw.flush();
            fw.close();
        } catch (IOException e) {
            System.err.println(className + ": class cannot be written to disk.");
        }
    }

    public List<SplittedMethod> getSplittedMethods() {
        return splittedMethods;
    }
}
