package com.od.TestSplitter;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.od.TestSplitter.Transformator.ObjectRecorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This file extracts the statements for variable declarations and assertions in the test source via
 * JavaParser. <p> Write Step: Before each assertion, all of the previously declared variables are
 * sent a function, where they are serialized and written to disk. <p> Read Step: The generated
 * split tests are created together with the proper read functions. These read calls read the
 * serialized object, deserialize and downcast to proper object. <p> Created by od on 25.02.2018.
 */
public class TestParser {
    static TargetType targetType;
    static SplitType splitType;
    static ConcurrentHashMap typeMap;
    public static String repoPath;

    enum TargetType {
        ALL_METHODS, METHOD_NAME
    }

    enum SplitType {
        ASSERTION, ALL_METHODS, METHOD_NAME
    }


    private static void validateOption(String errorMsg, String option) {
        if (option == null || option.startsWith("-")) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

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

    private static void writeClassToFile(CompilationUnit cu, String filePath) throws IOException{
        FileWriter fw = new FileWriter(new File(filePath));
        fw.append(cu.toString().replaceAll("ı", "i"));
        fw.flush();
        fw.close();
    }

    private static boolean checkConcurrentImports(CompilationUnit cu) {
        for(ImportDeclaration importDeclaration: cu.getImports()) {
            if (importDeclaration.toString().startsWith("import java.util.concurrent")) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        /**
         * Usage:
         * -p <Path to test source file>
         * (optional) -c <Target class name> default: all classes are processed.
         * (optional, repeated) -t <Target method name>, default: all methods in the class are
         *      processed. <Target class name> should be set.
         * (optional, repeated) -s <Split method name>, default: all method calls in the function
         *      level of the target method are considered as split point
         * -a : Enables splitting in assertions. Splitting by method name is disabled. If there is a
         *      group of assertions, the last one is considered as the split point.
         */
        String classPath = null;
        String className = null;
        Set<String> targetNames = new HashSet<>();
        Set<String> splitNames = new HashSet<>();
        targetType = TargetType.ALL_METHODS;
        splitType = SplitType.ALL_METHODS;
        boolean record = false;

        for (int i = 0; i < args.length; i++) {
            String option = args[i];
            String nextArgument = null;
            if (i + 1 < args.length) {
                nextArgument = args[i + 1];
            }
            // Skip the next argument if the option is not -a.
            i++;
            switch (option) {
                case "-p":
                    validateOption("Path should be specified after option -p",
                        nextArgument);
                    classPath = nextArgument;
                    break;
                case "-c":
                    validateOption("Class name should be specified after option -c",
                        nextArgument);
                    className = nextArgument;
                    break;
                case "-t":
                    validateOption("Target method should be specified after option -t",
                        nextArgument);
                    targetNames.add(nextArgument);
                    break;
                case "-s":
                    validateOption("Point of split method should be specified after option -s",
                        nextArgument);
                    splitNames.add(nextArgument);
                    break;
                case "-a":
                    i--;
                    splitType = SplitType.ASSERTION;
                    break;
                default:
                    throw new IllegalArgumentException("Option is not recognized: " + option);
            }
        }
        if (classPath == null) {
            throw new IllegalArgumentException("Path is not specified.");
        }

        if (classPath.charAt(classPath.length() -1) != '/') {
            classPath = classPath + "/";
        }
        if (System.getProperty("os.name").startsWith("Windows")) {
            classPath.replaceAll("/", "\\\\");
        }

        if (splitType != SplitType.ASSERTION) {
            if (splitNames.size() > 0) {
                splitType = SplitType.METHOD_NAME;
            }
        }

        if (targetNames.size() > 0) {
            targetType = TargetType.METHOD_NAME;
        }

        repoPath = classPath;

        List<String> allTestFiles = new ArrayList<>();
        findAllFiles(allTestFiles, new File(classPath), "Test.java");
        if (className == null) {
            System.out.println("Classpath:" + classPath);
        } else {
            final String claz = className;
            allTestFiles = allTestFiles.stream().filter(s -> s.contains(claz)).collect(Collectors.toList());
        }
        System.out.println("Arguments: " + Arrays.toString(args));
        System.out.println("Found test files:" + allTestFiles.size());
        ArrayList<String> existingClasses = new ArrayList<>();
        ArrayList<String> generatedClasses = new ArrayList<>();


        int testCount = 1;

        ArrayList<MethodVisitorForSplit> visitors = new ArrayList<>();
        for (String path : allTestFiles) {
            CompilationUnit cu = JavaParser.parse(new FileInputStream(new File(path)));
            if (className == null) {
                if (System.getProperty("os.name").startsWith("Windows")) {
                    className = path.substring(path.lastIndexOf("\\") + 1, path.lastIndexOf("."));
                } else {
                    className = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
                }
            }
            testCount = 1;

            if (!cu.getClassByName(className).isPresent()) {
                continue;
            }

            ClassOrInterfaceDeclaration cls = cu.getClassByName(className).get();
            if (cls.getModifiers().contains(Modifier.ABSTRACT) || !checkConcurrentImports(cu)) {
                writeClassToFile(cu, path);
                String newClassPath = classPath.substring(0, classPath.length() - 1) + "_splitted/";
                writeClassToFile(cu, path.replaceFirst(classPath, newClassPath));
                className = null;
                continue;
            }
            MethodVisitorForSplit visitor = new MethodVisitorForSplit(cu, path, cls, targetNames, splitNames);
            visitors.add(visitor);
            cu.accept(visitor, null);
            writeClassToFile(cu, path);
            className = null;
        }
        Scanner scanner = new Scanner(System.in);
        System.out.print("Press enter to continue execution after generating object files:");
        scanner.next();
        List<String> allObjectFiles = new ArrayList<>();
        findAllFiles(allObjectFiles, new File(classPath + ObjectRecorder.SNAPSHOT_URL_COMBINATION), ".xml");
        HashMap<String,String> foundObjects = new HashMap<>();
        allObjectFiles.forEach(path-> {
            foundObjects.put(path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf(".")), path);
        });

        typeMap = ObjectRecorder.readTypeMap(classPath);
        for(MethodVisitorForSplit visitor: visitors) {
            ClassOrInterfaceDeclaration cls = visitor.cls;
            String path = visitor.path;
            CompilationUnit cu = visitor.cu;
            visitor.visitAll(foundObjects);
            List<MethodDeclaration> originalMethodList = visitor.originalMethodList;
            List<MethodDeclaration> generatedMethodList = visitor.generatedMethodList;

            for (MethodDeclaration methodDeclaration: cls.getMethods()) {
                cls.remove(methodDeclaration);
            }

            for (MethodDeclaration m : originalMethodList) {
                cls.addMember(m);
            }

            Collections.sort(generatedMethodList, new Comparator<MethodDeclaration>() {
                @Override
                public int compare(MethodDeclaration o1, MethodDeclaration o2) {
                    Integer number1 = Integer.parseInt(o1.getNameAsString().replace("generatedU", ""));
                    Integer number2 = Integer.parseInt(o2.getNameAsString().replace("generatedU", ""));
                    return number1.compareTo(number2);
                }
            });

            for (MethodDeclaration m : generatedMethodList) {
                cls.addMember(m);
            }
            //cls.setName(className);
            String newClassPath = classPath.substring(0, classPath.length() - 1) + "_splitted/";
            writeClassToFile(cu, path.replaceFirst(classPath, newClassPath));

        }
        System.out.print("Press enter to continue execution after running the spllitted tests:");
        scanner.next();
        for(MethodVisitorForSplit visitor: visitors) {
            ClassOrInterfaceDeclaration cls = visitor.cls;
            List<MethodDeclaration> generatedMethodList = visitor.generatedMethodList;
            String path = visitor.path;
            String newClassPath = classPath.substring(0, classPath.length() - 1) + "_splitted/";
            CompilationUnit cu = visitor.cu;
            visitor.visitAllLast();
            List<MethodDeclaration> originalMethodList = visitor.originalMethodList;


            //cls.setName(className);

            writeClassToFile(cu, path.replaceFirst(classPath, newClassPath));

        }

        if (record) {
            FileWriter fileWriter = new FileWriter("existingTests.txt");
            for (String s : existingClasses) {
                fileWriter.append(s + "\n");
            }
            fileWriter.close();
            fileWriter = new FileWriter("generatedTests.txt");
            for (String s : generatedClasses) {
                fileWriter.append(s + "\n");
            }
            fileWriter.close();
        }


        System.out.println("Instrumentation took: " + (System.currentTimeMillis() - start));
    }

}
