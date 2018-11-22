package com.od.TestSplitter.Basic;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.od.TestSplitter.Config;
import com.od.TestSplitter.TestParser;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class InformationExtracter  extends GenericVisitorAdapter<SplitInformation, ClassOrInterfaceDeclaration> {

    @Override
    public SplitInformation visit(MethodDeclaration methodDeclaration, ClassOrInterfaceDeclaration cls) {

        /*
         * The declared method is either a generated method, or is not a test method, or contains an innter class,
         * or is not included in the target methods. Therefore, no process will happen.
         */
        if (!isTargetMethod(methodDeclaration)) {
            return null;
        }

        if (!methodDeclaration.getParentNode().get().equals(cls)) {
            return null;
        }

        if (!methodDeclaration.getBody().isPresent()) {
            return null;
        }

        // Creating split information
        HashMap<String,String> varTypeMap = new HashMap<>();
        HashMap<Integer, Set<String>> varIndexMap = new HashMap<>();
        List<Integer> splitPoints = new ArrayList<>();

        List<Statement> statements = methodDeclaration.getBody().get().getStatements();


        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);
            Statement nextStatement = i + 1 < statements.size() ? statements.get(i+1) : null;

            HashMap<String, String> tempVarTypeMap = extractVariables(statement);
            if (tempVarTypeMap != null) {
                varTypeMap.putAll(tempVarTypeMap);
                varIndexMap.put(i, new HashSet<>(tempVarTypeMap.keySet()));
            }

            if (isSplitStatement(statement, nextStatement)) {
                splitPoints.add(i);
            }
        }

        return new SplitInformation(methodDeclaration, varTypeMap, varIndexMap, splitPoints);
    }


    private static boolean isTargetMethod(MethodDeclaration method) {
        boolean isGenerated = method.getName().toString().startsWith("generated");
        boolean isTest =  method.getAnnotations().size() > 0
                && method.getAnnotations().contains(new MarkerAnnotationExpr("Test"));

        boolean containsInnerClass = method.getBody().get().findAll(LocalClassDeclarationStmt.class).size() > 0;
        return !isGenerated && !containsInnerClass &&
                isTest &&
                (Config.targetType == Config.TargetType.ALL_METHODS
                        || Config.targetMethodNames.contains(method.getName().toString()));
    }

    /**
     * Determine whether the statement in given index is a point of split or not.
     * If assertions are used, next statement is also checked for group of assertions.
     * @return true if the statement is a split statement.
     */
    private static boolean isSplitStatement(Statement statement, Statement nextStatement) {
        if (nextStatement == null) {
            return true;
        }

        if (statement instanceof ExpressionStmt &&
                ((ExpressionStmt) statement).getExpression() instanceof MethodCallExpr) {
            String methodName = statement.toString();
            String nextStatementName = "";
            if (nextStatement instanceof ExpressionStmt)
                nextStatementName = ((ExpressionStmt) nextStatement).getExpression().toString();
            if (methodName.startsWith("assert") && Config.splitType == Config.SplitType.ASSERTION &&
                    !nextStatementName.startsWith("assert")) {
                return true;
            }
            else {
                if (Config.splitType == Config.SplitType.ALL_METHODS)
                    return true;

                String decidedSplit = null;
                for (String spl : Config.splitTargetMethodNames) {
                    if (methodName.contains(spl)) {
                        decidedSplit = spl;
                        break;
                    }
                }
                if (decidedSplit != null) {
                    Config.splitTargetMethodNames.remove(decidedSplit);
                    return true;
                }
            }
        }
        return false;
    }

    private HashMap<String, String> extractVariables(Statement statement) {
        if (statement instanceof ExpressionStmt &&
                ((ExpressionStmt) statement).getExpression() instanceof VariableDeclarationExpr) {
            VariableDeclarationExpr stmt = (VariableDeclarationExpr) (((ExpressionStmt) statement)
                    .getExpression());
            HashMap<String, String> result = new HashMap<>();
            for (VariableDeclarator declarator : stmt.getVariables()) {
                result.put(declarator.getNameAsString(), declarator.getType().toString());
            }
            return result;
        }
        return null;
    }
}

