package com.od.TestSplitter;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This file extracts the statements for variable declarations and assertions in the test source via
 * JavaParser. <p> Write Step: Before each assertion, all of the previously declared variables are
 * sent a function, where they are serialized and written to disk. <p> Read Step: The generated
 * split tests are created together with the proper read functions. These read calls read the
 * serialized object, deserialize and downcast to proper object. <p> Created by od on 25.02.2018.
 */
public class TestParser {

    private static void findAllFiles(List<String> fileList, File file, String pattern) {
        File[] list = file.listFiles();
        if (list != null) {
            for (File fil : list) {
                if (fil.isDirectory()) {
                    findAllFiles(fileList, fil, pattern);
                }
                else if (fil.getName().contains(pattern)) {
                    fileList.add(fil.toString());
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Config.runningMainClass = true;

        long start = System.currentTimeMillis();
        Config.applyConfigs(args);


        List<String> allTestFiles = new ArrayList<>();
        findAllFiles(allTestFiles, new File(Config.projectPath), "Test.java");
        String className = Config.targetClassName;

        if (className == null) {
            System.out.println("Classpath:" + Config.projectPath);
        } else {
            final String claz = className;
            allTestFiles = allTestFiles.stream().filter(s -> s.contains(claz)).collect(Collectors.toList());
        }

        for (String pathToClassFile : allTestFiles) {
            CompilationUnit cu = JavaParser.parse(new FileInputStream(new File(pathToClassFile)));
            if (className == null) {
                className = pathToClassFile.substring(pathToClassFile.lastIndexOf("/") + 1, pathToClassFile.lastIndexOf("."));
            }


            if (!cu.getClassByName(className).isPresent()) {
                continue;
            }
            ClassOrInterfaceDeclaration cls = cu.getClassByName(className).get();
            RawClass rawClass = new RawClass(cu, pathToClassFile, cls, className);
            rawClass.processBasic();

            if (Config.combinatorialType == Config.CombinatorialType.NONE) {
                rawClass.writeAllToFileBasic();
            } else {
                Scanner scanner = new Scanner(System.in);
                System.out.print("Press enter to continue execution after generating object files:");
                scanner.next();
                Config.fetchCanonicalTypeMap();
                Config.fetchValueCountMap();
                rawClass.processCombinatorial();
                rawClass.writeAllToFileInterCombinatorial();
                System.out.print("Press enter to continue execution after generating assertion files:");
                scanner.next();
                rawClass.writeAllToFileCombinatorial();
            }
            className = null;
        }

        System.out.println("Instrumentation took: " + (System.currentTimeMillis() - start));
    }

}
