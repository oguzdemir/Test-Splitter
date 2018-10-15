package com.od.TestSplitter;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GeneratedMethod {
    private MethodDeclaration methodDeclaration;
    private List<Statement> statements;
    private List<String> neededVariables;
    private String classAndMethodName;
    private Map<String, String> neededVariablesTypes;
    private Map<String, String> fieldMap;
    private int fileIndex;

    public GeneratedMethod(MethodDeclaration methodDeclaration, String classAndMethodName,
        Map<String, String> variableMap, int fileIndex, Map<String, String> fieldMap) {
        this.methodDeclaration = methodDeclaration;
        statements = methodDeclaration.getBody().get().getStatements();
        this.classAndMethodName = classAndMethodName;
        this.fieldMap = fieldMap;
        this.fileIndex = fileIndex;
        HashMap<String, Integer> defs = getVariableInformation();
        this.neededVariablesTypes = new HashMap<>();
        neededVariables.retainAll(variableMap.keySet());
        neededVariables.forEach( v -> neededVariablesTypes.put(v, variableMap.get(v)));
        fixDefinitions(defs, variableMap);
    }

    public List<String> getNeededVariables() {
        return neededVariables;
    }

    private void fixDefinitions(HashMap<String, Integer> defs, Map<String, String> variableMap) {
        defs.forEach((k,v) -> {
            if (v == -1)
                return;
            if (fieldMap.containsKey(k))
                return;
            if (!variableMap.containsKey(k))
                return;

            if(!neededVariablesTypes.containsKey(k)) {
                Statement s = statements.remove((int) v);
                if (s.asExpressionStmt().getExpression() instanceof AssignExpr) {
                    AssignExpr expr = (AssignExpr) s.asExpressionStmt().getExpression();
                    if (k.equals(expr.getTarget().toString())){
                        Type type = JavaParser.parseType(variableMap.get(k));
                        VariableDeclarator varDec = new VariableDeclarator(type, k, expr.getValue());
                        s = new ExpressionStmt(new VariableDeclarationExpr(varDec));
                        if (s.getComment().isPresent())
                            s.setComment(s.getComment().get());
                        statements.add(v, s);
                    } else {
                        System.err.println("Var names doesnt match!");
                    }
                } else {
                    System.err.println("Assign expression expected!");
                }
            }
        });
    }
    public void addReadStatements() {
        if (fileIndex == 0)
            return;

        BlockStmt block = methodDeclaration.getBody().get();
        List<String> list = new ArrayList<>(neededVariablesTypes.keySet());
        Collections.sort(list, Collections.reverseOrder());
        for (String variableName: list) {
            Expression readExpr = toReadExpr(variableName, neededVariablesTypes.get(variableName), fileIndex, false);
            block.addStatement(0, readExpr);
        }

        list = new ArrayList<>(fieldMap.keySet());
        Collections.sort(list, Collections.reverseOrder());
        for (String variableName: list) {
            Expression readExpr = toReadExpr(variableName, fieldMap.get(variableName), fileIndex, true);
            block.addStatement(0, readExpr);
        }
    }
    private HashMap<String,Integer> getVariableInformation() {
        // Gathering Definitions and Uses
        HashMap<String, Integer> uses = new HashMap<>();
        HashMap<String, Integer> defs = new HashMap<>();
        for (int i = statements.size() - 1; i >= 0; i--) {
            processExpr(i, statements.get(i), uses, defs);
        }
        neededVariables = new ArrayList<>();
        uses.forEach((var, index) -> {
            if (defs.containsKey(var)) {
                if (defs.get(var) >= index)
                    neededVariables.add(var);
            } else {
                neededVariables.add(var);
            }

        });
        Collections.sort(neededVariables);
        return defs;
    }


    void processExpr(int index, Node expression,
        HashMap<String, Integer> uses, HashMap<String, Integer> defs) {
        if (expression == null) {
            return;
        }
        if (expression instanceof ExpressionStmt && ((ExpressionStmt) expression).isExpressionStmt()) {
            Expression exp = ((ExpressionStmt) expression).getExpression();
            if (exp instanceof VariableDeclarationExpr) {
                for (VariableDeclarator variableDeclarator: ((VariableDeclarationExpr) exp).getVariables()) {
                    // If there is a new definition, this variable should not be restored.
                    defs.put(variableDeclarator.getNameAsString(), -1);
                    processExpr(index, variableDeclarator.getInitializer().get(), uses, defs);
                }
            } else if (exp instanceof AssignExpr) {
                processExpr(index, ((AssignExpr) exp).getValue(), uses, defs);
                Expression target = ((AssignExpr) exp).getTarget();
                if (target instanceof  NameExpr) {
                    defs.put(((NameExpr) target).getNameAsString(), index);
                } else {
                    processExpr(index, target, uses, defs);
                }
            } else {
                exp.findAll(NameExpr.class).forEach(u -> {
                    uses.put(u.toString(), index);
                });
            }
        }
        else {
            expression.findAll(NameExpr.class).forEach(u -> {
                uses.put(u.toString(), index);
            });
        }
    }

    /**
     * Creates a read expression to recover the state of a variable or state
     *
     * @param variableName Name of the variable.
     * @param typeName Type of the recovered variable field.
     * @param index Index of the splitted method, this index is used for finding the correct recorded file.
     * @param isThis Flag for differentiate fields and variables. If isThis = true; the resulting read statement
     *               will be `this.fieldName = readExpr`;
     * @return Corresponding read expression
     */
    private Expression toReadExpr(String variableName, String typeName, int index, boolean isThis) {
        Type type = JavaParser.parseType(typeName);
        Type castType = type;
        if (type.isPrimitiveType()) {
            castType = ((PrimitiveType) type).toBoxedType();
        }
        NameExpr clazz = new NameExpr("com.od.TestSplitter.Transformator.ObjectRecorder");
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

}
