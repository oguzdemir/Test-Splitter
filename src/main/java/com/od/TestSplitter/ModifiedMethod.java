package com.od.TestSplitter;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.VoidType;
import com.od.TestSplitter.TestParser.SplitType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ModifiedMethod {

    private MethodDeclaration methodDeclaration;
    private List<Statement> statements;
    private Set<String> splitTargets;
    private List<Integer> splitIndexes;
    private String classAndMethodName;
    private Map<Integer, Map<String,String>> variableIndexedMap;
    private Map<Integer, List<Expression>> expressions;
    private Map<String, String> fieldMap;

    ModifiedMethod(MethodDeclaration methodDeclaration, Set<String> splitTargets,
        String classAndMethodName, Map<String, String> fieldMap) {
        this.methodDeclaration = methodDeclaration;
        this.statements = methodDeclaration.getBody().get().getStatements();
        this.splitTargets = splitTargets;
        this.classAndMethodName = classAndMethodName;
        this.fieldMap = fieldMap;
        findSplitIndexes();
        fixEmptyDeclarationsAndGenerateVariableMap();
        expressions = new HashMap<>();
    }

    public List<Integer> getSplitIndexes() {
        return splitIndexes;
    }

    public Map<String, String> getVariableMap(int index) {
        return variableIndexedMap.get(index);
    }

    public void processGenerated(int index, List<String> list) {
        int fileIndex = splitIndexes.indexOf(index);
        List<Expression> expList = new ArrayList<>(list.size() + fieldMap.size() + 1);
        List<String> list2 = new ArrayList<>(fieldMap.keySet());
        Collections.sort(list2);
        for (String field : list2) {
            expList.add(toWriteExpr(field, fileIndex, true));
        }
        Collections.sort(list);
        for (String var: list) {
            expList.add(toWriteExpr(var, fileIndex, false));
        }
        if (expList.size() > 0) {
            expList.add(toFinalizeExpr(fileIndex));
        }
        expressions.put(index, expList);
    }

    public void writeAllStatements() {
        if (splitIndexes.size() == 0)
            return;

        for(int i = 0; i < splitIndexes.size() - 1; i++) {
            int current = splitIndexes.get(i);
            int next = splitIndexes.get(i+1);
            expressions.put(current, expressions.get(next));
        }

        expressions.remove(splitIndexes.get(splitIndexes.size()-1));

        BlockStmt blockStmt = new BlockStmt();

        for (int i = 0; i < statements.size(); i++) {
            blockStmt.addStatement(statements.get(i));
            if (expressions.containsKey(i)) {
                List<Expression> exps = expressions.get(i);
                if (exps != null)
                    exps.forEach(exp-> blockStmt.addStatement(exp));
            }
        }

        methodDeclaration.setBody(blockStmt);
    }

    /**
     * Returns the map of generated methods. Keys are the index of the last statement of each
     * generated method in the original method.
     * @param startingNumber starting number to name the generated methods.
     * @return map of generated methods.
     */
    Map<Integer, MethodDeclaration> generateMethods(int startingNumber) {
        Map<Integer, MethodDeclaration> generatedMethods = new TreeMap<>();

        BlockStmt block = new BlockStmt();
        for (int i = 0; i < statements.size(); i++) {
            block.addStatement(statements.get(i));

            if (splitIndexes.contains(i)) {
                // create a methodDeclaration
                EnumSet<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
                MethodDeclaration generated = new MethodDeclaration(modifiers, new VoidType(),"generatedU" + startingNumber++);
                for (AnnotationExpr a : methodDeclaration.getAnnotations()) {
                    generated.addAnnotation(a);
                }
                for (ReferenceType referenceType : methodDeclaration.getThrownExceptions()) {
                    generated.addThrownException(referenceType);
                }
                generated.setBody(block);
                generatedMethods.put(i, generated);
                block = new BlockStmt();
            }
        }

        return generatedMethods;
    }

    private Expression toFinalizeExpr(int fileIndex) {
        NameExpr clazz = new NameExpr("com.od.TestSplitter.Transformator.ObjectRecorder");
        MethodCallExpr call = new MethodCallExpr(clazz, "finalizeWriting");
        call.addArgument(new StringLiteralExpr(classAndMethodName));
        call.addArgument(new IntegerLiteralExpr(fileIndex));
        return call;
    }

    private Expression toWriteExpr(String variable, int index, boolean isThis) {
        NameExpr objectRecorderClass = new NameExpr(
            "com.od.TestSplitter.Transformator.ObjectRecorder");
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

    /**
     * Sometimes, we are getting "variable may not be initialized" error whenever we try to write an
     * object which has an empty declaration (such as Object obj;). Therefore, we are fixing every
     * such initialization to default value. This has no effect on the execution.
     *
     * At the same time, the variable map ( variableName > type ) is generated.
     */
    private void fixEmptyDeclarationsAndGenerateVariableMap() {
        HashMap<String,String> variableMap = new HashMap<>();
        variableIndexedMap = new HashMap<>();
        variableIndexedMap.put(splitIndexes.get(0),new HashMap<>());
        // AtomicInteger is used to be able to increase it inside lambda func
        AtomicInteger index = new AtomicInteger(0);
        statements.forEach(statement -> {
            if (statement instanceof ExpressionStmt &&
                ((ExpressionStmt) statement).getExpression() instanceof VariableDeclarationExpr) {
                VariableDeclarationExpr stmt = (VariableDeclarationExpr) (((ExpressionStmt) statement)
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
                    variableMap.put(declarator.getNameAsString(), declarator.getType().toString());
                }
            }

            int splitIndex = splitIndexes.indexOf(index.getAndIncrement());
            if (splitIndex != -1 && splitIndex != splitIndexes.size() - 1) {
                variableIndexedMap.put(splitIndexes.get(splitIndex + 1), new HashMap<>(variableMap));
            }
        });
    }
    /**
     * Find where to split the original method, in terms of statement index.
     */
    private void findSplitIndexes() {
        splitIndexes = new ArrayList<>();
        for (int i = 0; i < statements.size(); i++) {
            if(isSplitStatement(i)) splitIndexes.add(i);
        }
    }

    /**
     * Determine whether the statement in given index is a point of split or not.
     * If assertions are used, next statement is also checked for group of assertions.
     * @param index index of the statement
     * @return true if the statement is a split statement.
     */
    private boolean isSplitStatement(int index) {
        Statement statement = getStatement(index);
        if (index == statements.size() - 1)
            return true;

        if (statement instanceof ExpressionStmt &&
            ((ExpressionStmt) statement).getExpression() instanceof MethodCallExpr) {
            String methodName = statement.toString();
            Statement nextStatement = getStatement(index + 1);
            String nextStatementName = "";
            if (nextStatement instanceof ExpressionStmt)
                nextStatementName = ((ExpressionStmt) nextStatement).getExpression().toString();
            if (methodName.startsWith("assert") && TestParser.splitType == SplitType.ASSERTION &&
                !nextStatementName.startsWith("assert")) {
                return true;
            }
            else {
                if (TestParser.splitType == SplitType.ALL_METHODS)
                    return true;

                String decidedSplit = null;
                for (String spl : splitTargets) {
                    if (methodName.contains(spl)) {
                        decidedSplit = spl;
                        break;
                    }
                }
                if (decidedSplit != null) {
                    splitTargets.remove(decidedSplit);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A getter for statements to get rid of the out of bound exp, and checking size everytime.
     * @param index index of statement.
     * @return statement in corresponding index, null if out of bounds.
     */
    private Statement getStatement(int index) {
        if (index < 0 || index >= statements.size()) {
            return null;
        }
        return statements.get(index);
    }
}
