package com.od.TestSplitter.Combinatorial;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.od.TestSplitter.Basic.SplitInformation;
import com.od.TestSplitter.Config;
import com.od.TestSplitter.RawClass;

import java.util.*;

public class CombinatorialMethod {
    private MethodDeclaration parentMethod;
    private MethodDeclaration methodDeclaration;
    private String classAndMethodName;
    private int fileReadIndex;
    private TreeMap<String, Expression> variableReadStatements;
    private LinkedList<Expression> thisFieldReadStatements;
    private ArrayList<Expression> alteredReadStatements;
    private Map<String, String> varTypeMap;
    private HashMap<String, Integer> alteredValues; // the map of altered variables, variable -> customReadIndex
    private ArrayList<Expression> assertionRecords; // the write statements for assertions
    private ArrayList<Expression> modifiedAssertions; // assertions with record statements on expected clause


    public CombinatorialMethod(MethodDeclaration parentMethod,
                               MethodDeclaration methodDeclaration,
                               String classAndMethodName,
                               int fileReadIndex,
                               TreeMap<String, Expression> variableReadStatements,
                               LinkedList<Expression> thisFieldReadStatements,
                               Map<String, String> varTypeMap,
                               HashMap<String, Integer> alteredValues) {
        this.parentMethod = parentMethod;
        this.methodDeclaration = methodDeclaration;
        this.classAndMethodName = classAndMethodName;
        this.fileReadIndex = fileReadIndex;
        this.variableReadStatements = variableReadStatements;
        this.thisFieldReadStatements = thisFieldReadStatements;
        this.varTypeMap = varTypeMap;
        this.alteredValues = alteredValues;
    }

    public void process() {
        generateCustomReads();
        generateModifiedAssertionsAndWrites();
        addAllReadStatements();
        addRecordAssertions();
    }

    public void postProcess() {
        removeAssertionRecords();
        addModifiedAssertions();
    }

    private void generateCustomReads() {
        alteredReadStatements = new ArrayList<>();
        for(Map.Entry<String, Integer> entry: alteredValues.entrySet()) {
            String variableName = entry.getKey();
            String typeName = varTypeMap.get(variableName);
            String fullClassName = Config.getCannonicalType(classAndMethodName, typeName);
            int index = entry.getValue();
            alteredReadStatements.add(toSpecificReadExpr(variableName, typeName, fullClassName, index));
        }
    }

    private Expression toSpecificReadExpr(String variableName, String typeName, String fullClassName, int index) {
        Type type = JavaParser.parseType(typeName);
        Type castType = type;
        if (type.isPrimitiveType()) {
            castType = ((PrimitiveType) type).toBoxedType();
        }
        NameExpr clazz = new NameExpr("com.od.TestSplitter.Transformator.ObjectRecorder");
        MethodCallExpr call = new MethodCallExpr(clazz, "readSpecificObject");
        call.addArgument(new StringLiteralExpr(fullClassName));
        call.addArgument(new IntegerLiteralExpr(index));
        CastExpr castExpr = new CastExpr(castType, call);
        VariableDeclarator declarator = new VariableDeclarator(type, variableName,
            castExpr);
        return new VariableDeclarationExpr(declarator);
    }

    private void addAllReadStatements() {
        if (fileReadIndex == 0)
            return;

        int count = 0;
        BlockStmt block = methodDeclaration.getBody().get();

        for (Expression ex: thisFieldReadStatements) {
            block.addStatement(count++, ex);
        }

        for (Map.Entry<String, Expression> entry: variableReadStatements.entrySet()) {
            if (!alteredValues.containsKey(entry.getKey())) {
                block.addStatement(count++, entry.getValue());
            }
        }

        for (Expression ex: alteredReadStatements) {
            block.addStatement(count++, ex);
        }
    }

    private void addRecordAssertions() {
        BlockStmt blockStmt = methodDeclaration.getBody().get();
        assertionRecords.forEach( r-> blockStmt.addStatement(r));
    }

    private void addModifiedAssertions() {
        BlockStmt blockStmt = methodDeclaration.getBody().get();
        modifiedAssertions.forEach( r-> blockStmt.addStatement(r));
    }

    private void generateModifiedAssertionsAndWrites() {

        assertionRecords = new ArrayList<>();
        modifiedAssertions = new ArrayList<>();

        //Extracting assertions
        ArrayList<Statement> assertions = new ArrayList<>();
        for (Statement s: methodDeclaration.getBody().get().getStatements()) {
            if (s.toString().startsWith("assert")) {
                assertions.add(s);
            }
        }
        for (Statement s: assertions) {
            methodDeclaration.getBody().get().remove(s);
        }


        int count = 0;
        String fileName = classAndMethodName.split("_")[0] + "_" + methodDeclaration.getNameAsString();
        for (Statement s: assertions) {
            if (s.toString().startsWith("assertEquals")) {
                // Record statement for an assertion
                Expression actual = ((MethodCallExpr) ((ExpressionStmt) s).getExpression()).getArgument(1);

                NameExpr objectRecorderClass = new NameExpr(
                        "com.od.TestSplitter.Transformator.ObjectRecorder");
                MethodCallExpr writeCallExpr = new MethodCallExpr(objectRecorderClass, "writeObject");
                writeCallExpr.addArgument(new StringLiteralExpr(fileName));
                writeCallExpr.addArgument(actual);
                writeCallExpr.addArgument(new IntegerLiteralExpr(0));
                assertionRecords.add(writeCallExpr);

                // Modified assertion statement
                MethodCallExpr exp = (MethodCallExpr) ((ExpressionStmt) s).getExpression();

                NameExpr clazz = new NameExpr("com.od.TestSplitter.Transformator.ObjectRecorder");
                MethodCallExpr call = new MethodCallExpr(clazz, "readObject");
                call.addArgument(new StringLiteralExpr(fileName));
                call.addArgument(new IntegerLiteralExpr(0));
                call.addArgument(new IntegerLiteralExpr(count++));

                exp.setArgument(0, call);
                modifiedAssertions.add(exp);

            }
        }

        if (count > 0) {
            NameExpr clazz = new NameExpr("com.od.TestSplitter.Transformator.ObjectRecorder");
            MethodCallExpr call = new MethodCallExpr(clazz, "finalizeWriting");
            call.addArgument(new StringLiteralExpr(fileName));
            call.addArgument(new IntegerLiteralExpr(0));
            assertionRecords.add(call);
        }
    }

    public void removeAssertionRecords() {
        methodDeclaration.getBody().get().getStatements().removeIf(s -> {
            if (s.toString().contains("com.od.TestSplitter.Transformator.ObjectRecorder.writeObject") ||
                    s.toString().contains("com.od.TestSplitter.Transformator.ObjectRecorder.finalizeWriting")) {
                return true;
            }
            return false;
        });
    }

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }
}
