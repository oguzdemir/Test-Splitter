package org.od.TestSplitter;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.UnaryExpr.Operator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This file extracts the statements for variable declarations and assertions in the test source via
 * JavaParser. <p> Write Step: Before each assertion, all of the previously declared variables are
 * sent a function, where they are serialized and written to disk. <p> Read Step: The generated
 * split tests are created together with the proper read functions. These read calls read the
 * serialized object, deserialize and downcast to proper object. <p> Created by od on 25.02.2018.
 */
public class TestParser {

    static ArrayList<MethodDeclaration> methods;
    static ArrayList<String> boolFlags;
    static int testCount = 0;

    enum TargetType {
        ALL_METHODS, METHOD_NAME
    }

    enum SplitType {
        ASSERTION, ALL_METHODS, METHOD_NAME
    }

    static class MyVisitor extends VoidVisitorAdapter<Object> {

        Set<String> targetNames;
        Set<String> splitNames;
        TargetType targetType;
        SplitType splitType;
        ClassOrInterfaceDeclaration cls;

        public MyVisitor(ClassOrInterfaceDeclaration cls, Set<String> targetNames, Set<String> splitNames, TargetType targetType,
            SplitType splitType) {
            this.cls = cls;
            this.targetNames = targetNames;
            this.splitNames = splitNames;
            this.targetType = targetType;
            this.splitType = splitType;
        }


        VariableDeclarationExpr toReadExpr(String methodName, String typeName, int index, String var) {
            Type type = JavaParser.parseType(typeName);
            Type castType = type;
            if (type.isPrimitiveType()) {
                castType = ((PrimitiveType) type).toBoxedType();
            }
            NameExpr clazz = new NameExpr("org.od.TestSplitter.Transformator.ObjectRecorder");
            MethodCallExpr call = new MethodCallExpr(clazz, "readObject");
            call.addArgument(new StringLiteralExpr(methodName));
            call.addArgument(new IntegerLiteralExpr(index));
            CastExpr castExpr = new CastExpr(castType, call);
            VariableDeclarator declarator = new VariableDeclarator(type, var,
                castExpr);
            return new VariableDeclarationExpr(declarator);
        }

        void addWriteStatements(String methodName, BlockStmt methodBlock, ArrayList<Integer> splitIndexes,
            ArrayList<String> variables) {

            int addedStatements = 0;
            int index = 1;
            for (Integer i : splitIndexes) {
                int ind = i + addedStatements + 1;

                NameExpr clazz = new NameExpr("org.od.TestSplitter.Transformator.ObjectRecorder");
                MethodCallExpr call = new MethodCallExpr(clazz, "finalizeWriting");
                methodBlock.addStatement(ind, call);
                addedStatements++;
                for (String variable : variables) {
                    if (variable.equals("%")) {
                        break;
                    }
                    NameExpr objectClazz = new NameExpr(
                        "org.od.TestSplitter.Transformator.ObjectRecorder");
                    MethodCallExpr writeCall = new MethodCallExpr(objectClazz, "writeObject");
                    writeCall.addArgument(new StringLiteralExpr(methodName));
                    writeCall.addArgument(variable);
                    writeCall.addArgument(new IntegerLiteralExpr(index));
                    methodBlock.addStatement(ind, writeCall);
                    addedStatements++;
                }
                variables.remove("%");
                index++;
            }
        }

        Map<String, Object> parseBody(NodeList<Statement> statements) {
            Map<String, Object> resultMap = new HashMap<>();
            ArrayList<String> recordedVariables = new ArrayList<>();
            ArrayList<String> recordedTypes = new ArrayList<>();
            ArrayList<Integer> splitIndexes = new ArrayList<>();

            resultMap.put("recordedVariables", recordedVariables);
            resultMap.put("recordedTypes", recordedTypes);
            resultMap.put("splitIndexes", splitIndexes);

            //Collect variable declarations and assert statements
            Object[] statementArray = statements.toArray();
            for (int i = 0; i < statementArray.length; i++) {
                Statement statement = (Statement) statementArray[i];
                if (statement instanceof ExpressionStmt &&
                    ((ExpressionStmt) statement)
                        .getExpression() instanceof VariableDeclarationExpr) {
                    VariableDeclarationExpr stmt = (VariableDeclarationExpr) (((ExpressionStmt) statement)
                        .getExpression());
                    for (VariableDeclarator declarator : stmt.getVariables()) {
                        recordedVariables.add(declarator.getName().toString());
                        recordedTypes.add(declarator.getType().toString());
                    }
                }

                boolean split = false;
                // Split point
                if (statement instanceof ExpressionStmt &&
                    ((ExpressionStmt) statement).getExpression() instanceof MethodCallExpr) {
                    String methodName = statement.toString();
                    if (methodName.startsWith("assert") && splitType == SplitType.ASSERTION) {
                        if (i + 1 >= statementArray.length) {
                            split = true;
                        }
                        else if (!statementArray[i + 1].toString().startsWith("assert")) {
                            split = true;
                        }
                    }
                    else {
                        if (splitType == SplitType.ALL_METHODS) {
                            split = true;
                        }
                        else {
                            if (i == statementArray.length - 1) {
                                split = true;
                            } else {
                                String decidedSplit = null;
                                for (String spl : splitNames) {
                                    if (methodName.contains(spl)) {
                                        decidedSplit = spl;
                                        split = true;
                                        break;
                                    }
                                }
                                if (split) {
                                    splitNames.remove(decidedSplit);
                                }
                            }
                        }
                    }
                }
                if (split && i > 0) {
                    recordedVariables.add("%");
                    recordedTypes.add("%");
                    splitIndexes.add(i);
                    statement.setComment(new LineComment("Split Point: " + splitIndexes.size()));
                }
            }

            return resultMap;
        }

        @Override
        public void visit(MethodDeclaration n, Object arg) {

            if ((!n.getName().toString().startsWith("generated")) && n.getAnnotations().size() > 0 &&
                n.getAnnotation(0).toString().equals("@Test") &&
                (
                    targetType == TargetType.ALL_METHODS ||
                    targetNames.contains(n.getName().toString()))) {
                Map<String, Object> parsedBody = parseBody(n.getBody().get().getStatements());

                ArrayList<String> recordedVariables = (ArrayList<String>) parsedBody
                    .get("recordedVariables");
                ArrayList<String> recordedTypes = (ArrayList<String>) parsedBody
                    .get("recordedTypes");
                ArrayList<Integer> splitIndexes = (ArrayList<Integer>) parsedBody
                    .get("splitIndexes");

                if (splitIndexes.size() <= 1) {
                    super.visit(n, arg);
                    return;
                }
                // Generating splitted methods.
                recordedVariables.add(0, "%");
                recordedTypes.add(0, "%");

                BlockStmt block = new BlockStmt();
                int index = 0;
                Object[] statementArray = n.getBody().get().getStatements().toArray();
                boolean beforeClass = false;
                for (int statementIndex = 0; statementIndex < statementArray.length;
                    statementIndex++) {
                    Statement s = (Statement) statementArray[statementIndex];
                    if (statementIndex == splitIndexes.get(index)) {
                        int count = 0;
                        for (int i = 0; i < recordedVariables.size(); i++) {
                            String var = recordedVariables.get(i);
                            if (var.equals("%")) {
                                if (count == index) {
                                    break;
                                }
                                count++;
                                continue;
                            }
                            String typeName = recordedTypes.get(i);
                            VariableDeclarationExpr variableDeclarationExpr = toReadExpr(n.getNameAsString(),typeName,
                                index, var);
                            block.addStatement(0, variableDeclarationExpr);
                        }

                        // create a method
                        testCount++;
                        EnumSet<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
                        MethodDeclaration method = new MethodDeclaration(modifiers, new VoidType(),
                            "generatedU" + testCount);
                        method.addAnnotation(n.getAnnotation(0));
                        for (ReferenceType referenceType: n.getThrownExceptions()) {
                            method.addThrownException(referenceType);
                        }
                        // add a body to the method
                        block.addStatement(s.clone());
                        method.setBody(block);
                        methods.add(method);
                        beforeClass = true;
                        block = new BlockStmt();
                        index++;

                        if (index == splitIndexes.size()) {
                            break;
                        }
                    }
                    else {
                        block.addStatement(s.clone());
                    }
                }

                // Inserting Writes to the actual source code.
                recordedVariables.remove(0);
                recordedTypes.remove(0);
                if (beforeClass) {
                    //n.addModifier(Modifier.STATIC);
                    n.setAnnotation(0, new MarkerAnnotationExpr(new Name("Before")));
                }
                addWriteStatements(n.getNameAsString(), n.getBody().get(), splitIndexes, recordedVariables);

                String boolFlag = "hasInit" + n.getNameAsString();
                boolFlags.add(boolFlag);
                BlockStmt blockStmt = n.getBody().get().clone();
                blockStmt.addStatement(new AssignExpr(new NameExpr(boolFlag), new BooleanLiteralExpr(true), AssignExpr.Operator.ASSIGN ));
                BlockStmt newBlockStmt = new BlockStmt();
                IfStmt ifStmt = new IfStmt(new UnaryExpr(new NameExpr(boolFlag), Operator.LOGICAL_COMPLEMENT), blockStmt, null);
                newBlockStmt.addStatement(ifStmt);

                n.setBody(newBlockStmt);
            }
            super.visit(n, arg);
        }
    }

    public static void validateOption(String errorMsg, String option) {
        if (option == null || option.startsWith("-")) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    public static void findAllFiles(ArrayList<String> fileList, File file) {
        File[] list = file.listFiles();
        if (list != null) {
            for (File fil : list) {
                if (fil.isDirectory()) {
                    findAllFiles(fileList, fil);
                }
                else if (fil.getName().contains("Test.java")) {
                    fileList.add(fil.toString());
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {

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
        TargetType targetType = TargetType.ALL_METHODS;
        SplitType splitType = SplitType.ALL_METHODS;
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

        ArrayList<String> allTestFiles = new ArrayList<>();
        if (className == null) {
            findAllFiles(allTestFiles, new File(classPath));
        } else {
            String fullPath = classPath + className + ".java";
            File testFile = new File(fullPath);
            if (testFile.exists() && !testFile.isDirectory()) {
                allTestFiles.add(fullPath);
            } else {
                throw new IllegalArgumentException("Path: " + fullPath + " does not lead to a test class file.");
            }
        }

        ArrayList<String> existingClasses = new ArrayList<>();
        ArrayList<String> generatedClasses = new ArrayList<>();
        for (String path : allTestFiles) {
            methods = new ArrayList<>();
            boolFlags = new ArrayList<>();
            testCount = 0;

            CompilationUnit cu = JavaParser.parse(new FileInputStream(new File(path)));
            className = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
            if (System.getProperty("os.name").startsWith("Windows")) {
                className = path.substring(path.lastIndexOf("\\") + 1, path.lastIndexOf("."));
            }

            cu.addImport("org.junit.Before");
            ClassOrInterfaceDeclaration cls = cu.getClassByName(className).get();
            cu.accept(new MyVisitor(cls,targetNames, splitNames, targetType, splitType), null);

            for (String flag: boolFlags) {
                cls.addFieldWithInitializer(PrimitiveType.booleanType(), flag, new BooleanLiteralExpr(false), Modifier.PRIVATE);
            }
            for (MethodDeclaration m : methods) {
                cls.addMember(m);
            }
            String newName = className + "Generated" + testCount + "Test";
            cls.setName(newName);
            FileWriter fw = new FileWriter(new File(path.replace(className, newName)));
            fw.append(cu.toString().replaceAll("ı", "i"));
            fw.flush();
            fw.close();

            if (record) {
                existingClasses.add(className);
                generatedClasses.add(newName);
            }
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

    }
}
