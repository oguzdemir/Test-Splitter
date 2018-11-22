package com.od.TestSplitter.Basic.Intermediate;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.od.TestSplitter.RawClass;
import com.od.TestSplitter.Basic.SplitInformation;

import java.util.*;

public class IntermediateSysMethodGenerator extends VoidVisitorAdapter<String> {


    @Override
    public void visit(MethodDeclaration methodDeclaration, String className) {
        String classAndMethodName = className + "_" + methodDeclaration.getNameAsString();
        SplitInformation splitInformation = RawClass.splitInformationMap.get(methodDeclaration);

        if (splitInformation == null)
            return;

        Map<String, String> fieldMap = splitInformation.fieldMap;
        List<Integer> splitIndexes = splitInformation.splitPoints;

        if (splitIndexes.size() == 0)
            return;


        if (!methodDeclaration.getBody().isPresent()) {
            return;
        }


        Object[] constantStatementArray = methodDeclaration.getBody().get().getStatements().toArray();
        BlockStmt blockStmt = methodDeclaration.getBody().get();
        List<Statement> statements = blockStmt.getStatements();

        int writeIndexCount = 0;
        Statement currentSplitStatement = (Statement) constantStatementArray[splitIndexes.get(writeIndexCount)];
        Map<String, String> varTypeMap = splitInformation.activeVariableIndexedMap.get(splitIndexes.get(writeIndexCount++));

        BlockStmt newBlockStmt = new BlockStmt();

        for (Statement s : statements) {
            if (s.equals(currentSplitStatement) && writeIndexCount < splitIndexes.size()) {
                List<Expression> expressions = generateWriteAllStatements(classAndMethodName, varTypeMap, fieldMap, writeIndexCount);
                expressions.forEach(e-> newBlockStmt.addStatement(e));
                currentSplitStatement = (Statement) constantStatementArray[splitIndexes.get(writeIndexCount)];
                varTypeMap = splitInformation.activeVariableIndexedMap.get(splitIndexes.get(writeIndexCount++));
            }
            if (s instanceof ExpressionStmt &&
                ((ExpressionStmt) s).getExpression() instanceof VariableDeclarationExpr) {
                VariableDeclarationExpr stmt = (VariableDeclarationExpr) (((ExpressionStmt) s)
                    .getExpression());
                for (VariableDeclarator declarator : stmt.getVariables()) {
                    if (!declarator.getInitializer().isPresent()) {
                        if (!declarator.getType().isPrimitiveType()) {
                            declarator.setInitializer(new NullLiteralExpr());
                        } else if (declarator.getType().equals(PrimitiveType.booleanType())) {
                            declarator.setInitializer(new BooleanLiteralExpr(false));
                        } else {
                            declarator.setInitializer(new IntegerLiteralExpr(0));
                        }
                    }
                }
            }
            newBlockStmt.addStatement(s);
        }

        methodDeclaration.setBody(newBlockStmt);
    }

    private List<Expression> generateWriteAllStatements(String classAndMethodName, Map<String,String> varTypeMap, Map<String,String> fieldMap, int fileIndex) {
        ArrayList<Expression> result = new ArrayList<>();
        List<String> list = new ArrayList<>(fieldMap.keySet());
        Collections.sort(list);
        for (String variableName: list) {
            result.add(toWriteExpr(classAndMethodName, variableName, fileIndex, true ));
        }
        list = new ArrayList<>(varTypeMap.keySet());
        Collections.sort(list);
        for (String variableName: list) {
            result.add(toWriteExpr(classAndMethodName, variableName, fileIndex, false ));
        }

        result.add(toFinalizeExpr(classAndMethodName, fileIndex));
        return result;
    }


    private Expression toFinalizeExpr(String classAndMethodName, int fileIndex) {
        NameExpr clazz = new NameExpr("com.od.TestSplitter.Transformator.ObjectRecorder");
        MethodCallExpr call = new MethodCallExpr(clazz, "finalizeWriting");
        call.addArgument(new StringLiteralExpr(classAndMethodName));
        call.addArgument(new IntegerLiteralExpr(fileIndex));
        return call;
    }

    private Expression toWriteExpr(String classAndMethodName, String variable, int fileIndex, boolean isThis) {
        NameExpr objectRecorderClass = new NameExpr(
                "com.od.TestSplitter.Transformator.ObjectRecorder");
        MethodCallExpr writeCallExpr = new MethodCallExpr(objectRecorderClass, "writeObject");
        writeCallExpr.addArgument(new StringLiteralExpr(classAndMethodName));
        if (isThis) {
            writeCallExpr.addArgument(new FieldAccessExpr(new ThisExpr(), variable));
        } else {
            writeCallExpr.addArgument(variable);
        }
        writeCallExpr.addArgument(new IntegerLiteralExpr(fileIndex));

        return writeCallExpr;
    }
}
