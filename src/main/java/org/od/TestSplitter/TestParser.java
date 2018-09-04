package org.od.TestSplitter;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.*;

/**
 * This file extracts the statements for variable declarations and assertions in the test source via
 * JavaParser. <p> Write Step: Before each assertion, all of the previously declared variables are
 * sent a function, where they are serialized and written to disk. <p> Read Step: The generated
 * split tests are created together with the proper read functions. These read calls read the
 * serialized object, deserialize and downcast to proper object. <p> Created by od on 25.02.2018.
 */
public class TestParser {

    static ArrayList<MethodDeclaration> methods;
    static int testCount = 1;

    enum TargetType {
        ALL_METHODS, METHOD_NAME
    }

    enum SplitType {
        ASSERTION, ALL_METHODS, METHOD_NAME
    }

    static class MethodVisitorForSplit extends VoidVisitorAdapter<Object> {

        Set<String> targetNames;
        Set<String> splitNames;
        TargetType targetType;
        SplitType splitType;
        ClassOrInterfaceDeclaration cls;
        Map<String, String> fieldMap;

        public MethodVisitorForSplit(ClassOrInterfaceDeclaration cls, Set<String> targetNames, Set<String> splitNames, TargetType targetType,
                                     SplitType splitType) {
            this.cls = cls;
            this.targetNames = targetNames;
            this.splitNames = splitNames;
            this.targetType = targetType;
            this.splitType = splitType;
            this.fieldMap = extractFieldMap(cls);
        }

        @Override
        public void visit(MethodDeclaration method, Object arg) {
            if (!method.getParentNode().get().equals(cls)) {
                return;
            }
            if (checkTargetMethod(method)) {
                Map<Integer, Map<String, String>> parsedBody = parseMethodBody(method.getBody().get().getStatements());

                if (parsedBody.size() <= 1) {
                    super.visit(method, arg);
                    return;
                }

                BlockStmt block = new BlockStmt();
                int splitPointNumber = 0;
                int previousSplitStatementIndex = 0;
                int addedStatements = 0;
                Object[] statementArray = method.getBody().get().getStatements().toArray();

                for (int stmtInd = 0; stmtInd < statementArray.length; stmtInd++) {
                    Statement s = (Statement) statementArray[stmtInd];
                    block.addStatement(s.clone());

                    if (parsedBody.containsKey(stmtInd)) {
                        MethodDeclaration methodDeclaration = generateSplittedMethod(method, block, parsedBody.get(previousSplitStatementIndex), splitPointNumber);

                        methods.add(methodDeclaration);
                        block = new BlockStmt();
                        splitPointNumber++;

                        if (splitPointNumber == parsedBody.size()) {
                            break;
                        }
                        addWriteStatements(method, parsedBody.get(stmtInd), splitPointNumber, stmtInd + addedStatements + 1);
                        addedStatements += parsedBody.get(stmtInd).size() + fieldMap.size() + 1;

                        previousSplitStatementIndex = stmtInd;
                    }

                }
            } else {
                // Preserve the method if it is not splitted
                methods.add(method);
            }

            super.visit(method, arg);
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
        /**
         * Creates a read expression to recover the state of a variable or state
         *
         * @param classAndMethodName The method which is being processed. The read statement will go the this method.
         * @param variableName Name of the variable.
         * @param typeName Type of the recovered variable field.
         * @param index Index of the splitted method, this index is used for finding the correct recorded file.
         * @param isThis Flag for differentiate fields and variables. If isThis = true; the resulting read statement
         *               will be `this.fieldName = readExpr`;
         * @return Corresponding read expression
         */
        Expression toReadExpr(String classAndMethodName, String variableName, String typeName, int index, boolean isThis) {
            Type type = JavaParser.parseType(typeName);
            Type castType = type;
            if (type.isPrimitiveType()) {
                castType = ((PrimitiveType) type).toBoxedType();
            }
            NameExpr clazz = new NameExpr("org.od.TestSplitter.Transformator.ObjectRecorder");
            MethodCallExpr call = new MethodCallExpr(clazz, "readObject");
            call.addArgument(new StringLiteralExpr(classAndMethodName));
            call.addArgument(new IntegerLiteralExpr(index));
            CastExpr castExpr = new CastExpr(castType, call);
            if (isThis) {
                FieldAccessExpr fieldAccessExpr = new FieldAccessExpr(new ThisExpr(), variableName);
                return new AssignExpr(fieldAccessExpr, castExpr, AssignExpr.Operator.ASSIGN);
            } else {
                VariableDeclarator declarator = new VariableDeclarator(type, variableName,
                        castExpr);
                return new VariableDeclarationExpr(declarator);
            }
        }

        Expression toWriteExpr(String classAndMethodName, String variable, int index, boolean isThis) {
            NameExpr objectRecorderClass = new NameExpr(
                    "org.od.TestSplitter.Transformator.ObjectRecorder");
            MethodCallExpr writeCallExpr = new MethodCallExpr(objectRecorderClass, "writeObject");
            writeCallExpr.addArgument(new StringLiteralExpr(classAndMethodName));
            if (isThis) {
                writeCallExpr.addArgument(new FieldAccessExpr(new ThisExpr(), variable));
            } else {
                writeCallExpr.addArgument(variable);
            }
            writeCallExpr.addArgument(new IntegerLiteralExpr(index));

            return writeCallExpr;
        }

        void addWriteStatements(MethodDeclaration method, Map<String, String> variableMap, int splitIndex, int statementIndex) {
            BlockStmt methodBlock = method.getBody().get();

            methodBlock.getStatement(statementIndex).setComment(new LineComment("Split Point: " + splitIndex));

            NameExpr clazz = new NameExpr("org.od.TestSplitter.Transformator.ObjectRecorder");
            MethodCallExpr call = new MethodCallExpr(clazz, "finalizeWriting");
            methodBlock.addStatement(statementIndex, call);

            String classAndMethodName = cls.getNameAsString() + "_" + method.getNameAsString();
            for (Map.Entry<String, String> splitInfo : variableMap.entrySet()) {
                String variableName = splitInfo.getKey();
                Expression writeExpr = toWriteExpr(classAndMethodName,variableName, splitIndex, false);
                methodBlock.addStatement(statementIndex, writeExpr);
            }
            for (Map.Entry<String, String> fieldInfo : fieldMap.entrySet()) {
                String variableName = fieldInfo.getKey();
                Expression writeExpr = toWriteExpr(classAndMethodName,variableName, splitIndex, false);
                methodBlock.addStatement(statementIndex, writeExpr);
            }
        }

        boolean isSplitStatement(Object[] statementArray, int index) {
            Statement statement = (Statement) statementArray[index];
            boolean split = false;
            // Split point
            if (statement instanceof ExpressionStmt &&
                    ((ExpressionStmt) statement).getExpression() instanceof MethodCallExpr) {
                String methodName = statement.toString();
                if (methodName.startsWith("assert") && splitType == SplitType.ASSERTION) {
                    if (index + 1 >= statementArray.length) {
                        split = true;
                    }
                    else if (!statementArray[index + 1].toString().startsWith("assert")) {
                        split = true;
                    }
                }
                else {
                    if (splitType == SplitType.ALL_METHODS) {
                        split = true;
                    }
                    else {
                        if (index == statementArray.length - 1) {
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
            return split;
        }

        Map<Integer, Map<String, String>> parseMethodBody(NodeList<Statement> statements) {

            Map<Integer, Map<String, String>> resultMap = new HashMap<>();
            Map<String, String> splitInformation = new HashMap<>();

            //Collect variable declarations and assert statements
            Object[] statementArray = statements.toArray();
            for (int statementIndex = 0; statementIndex < statementArray.length; statementIndex++) {
                Statement statement = (Statement) statementArray[statementIndex];
                if (statement instanceof ExpressionStmt &&
                        ((ExpressionStmt) statement).getExpression() instanceof VariableDeclarationExpr) {
                    VariableDeclarationExpr stmt = (VariableDeclarationExpr) (((ExpressionStmt) statement)
                            .getExpression());
                    for (VariableDeclarator declarator : stmt.getVariables()) {
                        splitInformation.put(declarator.getNameAsString(), declarator.getType().toString());
                    }
                }

                boolean split = isSplitStatement(statementArray, statementIndex);

                if (split && statementIndex > 0) {
                    Map<String, String> clonedMap = new HashMap<>(splitInformation);
                    resultMap.put(statementIndex, clonedMap);
                    //statement.setComment(new LineComment("Split Point: " + resultMap.size()));
                }
            }

            return resultMap;
        }

        boolean checkTargetMethod(MethodDeclaration method) {
            boolean isGenerated = method.getName().toString().startsWith("generated");
            boolean isTest =  method.getAnnotations().size() > 0 && method.getAnnotation(0).toString().equals("@Test");

            return !isGenerated &&
                    isTest &&
                    (targetType == TargetType.ALL_METHODS || targetNames.contains(method.getName().toString()));
        }

        MethodDeclaration generateSplittedMethod(MethodDeclaration parentMethod, BlockStmt block, Map<String, String> variableMap, int index) {
            String classAndMethodName = cls.getNameAsString() + "_" + parentMethod.getNameAsString();
            // If map is null, it means the first splitted method is being generated.
            if (variableMap != null) {
                for (Map.Entry<String, String> splitInfo : variableMap.entrySet()) {
                    String variableName = splitInfo.getKey();
                    String variableType = splitInfo.getValue();
                    Expression readExpr = toReadExpr(classAndMethodName, variableName, variableType, index, false);
                    block.addStatement(0, readExpr);
                }

                for (Map.Entry<String, String> fieldInfo : fieldMap.entrySet()) {
                    String variableName = fieldInfo.getKey();
                    String variableType = fieldInfo.getValue();
                    Expression readExpr = toReadExpr(classAndMethodName, variableName, variableType, index, true);
                    block.addStatement(0, readExpr);
                }
            }

            // create a methodDeclaration
            EnumSet<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
            MethodDeclaration methodDeclaration = new MethodDeclaration(modifiers, new VoidType(),
                    "generatedU" + testCount++);
            methodDeclaration.addAnnotation(parentMethod.getAnnotation(0));
            for (ReferenceType referenceType: parentMethod.getThrownExceptions()) {
                methodDeclaration.addThrownException(referenceType);
            }
            methodDeclaration.setBody(block);

            return methodDeclaration;
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
        String repoName = "jfreechart";
        ArrayList<String> existingClasses = new ArrayList<>();
        ArrayList<String> generatedClasses = new ArrayList<>();
        for (String path : allTestFiles) {
            methods = new ArrayList<>();
            testCount = 1;
            CompilationUnit cu = JavaParser.parse(new FileInputStream(new File(path)));
            if (className == null) {
                if (System.getProperty("os.name").startsWith("Windows")) {
                    className = path.substring(path.lastIndexOf("\\") + 1, path.lastIndexOf("."));
                } else {
                    className = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
                }
            }

            ClassOrInterfaceDeclaration cls = cu.getClassByName(className).get();
            cu.accept(new MethodVisitorForSplit(cls,targetNames, splitNames, targetType, splitType), null);
            FileWriter fw = new FileWriter(new File(path));
            fw.append(cu.toString().replaceAll("ı", "i"));
            fw.flush();
            fw.close();

            for (MethodDeclaration methodDeclaration: cls.getMethods()) {
                cls.remove(methodDeclaration);
            }

            for (MethodDeclaration m : methods) {
                cls.addMember(m);
            }
            cls.setName(className);
            fw = new FileWriter(new File(path.replace(repoName, repoName + "_splitted")));
            fw.append(cu.toString().replaceAll("ı", "i"));
            fw.flush();
            fw.close();

            if (record) {
                existingClasses.add(className);
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
