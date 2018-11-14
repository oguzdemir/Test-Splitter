package com.od.TestSplitter;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
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
import com.od.TestSplitter.Transformator.ObjectRecorder;

import java.util.*;

public class GeneratedMethod {
    private MethodDeclaration methodDeclaration;
    private List<Statement> assertions;
    private List<Statement> statements;
    private List<String> neededVariables;
    private String classAndMethodName;
    private Map<String, String> neededVariablesTypes;
    private Map<String, String> fieldMap;
    private int fileIndex;
    private boolean combinedMethod;
    private int recordedAssertions;


    private enum CombinationType {
        NONE, SINGLE, PAIR_WISE
    }
    private static CombinationType combinationType = CombinationType.SINGLE;

    public GeneratedMethod(MethodDeclaration methodDeclaration, String classAndMethodName,
        Map<String, String> variableMap, int fileIndex, Map<String, String> fieldMap, boolean optimized) {
        // This a constructor for pure (not combined) generated methods
        combinedMethod = false;

        this.methodDeclaration = methodDeclaration;
        statements = methodDeclaration.getBody().get().getStatements();
        this.classAndMethodName = classAndMethodName;
        this.fieldMap = fieldMap;
        this.fileIndex = fileIndex;

        if (optimized) {
            HashMap<String, Integer> defs = getVariableInformation();
            this.neededVariablesTypes = new HashMap<>();
            neededVariables.retainAll(variableMap.keySet());
            neededVariables.forEach( v -> neededVariablesTypes.put(v, variableMap.get(v)));
            fixDefinitions(defs, variableMap);
        } else {
            neededVariablesTypes = variableMap;
            neededVariables = new ArrayList<>(variableMap.keySet());
        }
    }

    public GeneratedMethod(MethodDeclaration methodDeclaration, List<String> neededVariables,
                           String classAndMethodName, Map<String, String> neededVariablesTypes,
                           Map<String, String> fieldMap, int fileIndex, List<Expression> newStatements) {
        // This is a constructor for combined methods
        combinedMethod = true;

        this.methodDeclaration = methodDeclaration;
        statements = methodDeclaration.getBody().get().getStatements();
        this.neededVariables = neededVariables;
        this.classAndMethodName = classAndMethodName;
        this.neededVariablesTypes = new HashMap<>(neededVariablesTypes);
        this.fieldMap = fieldMap;
        this.fileIndex = fileIndex;


        BlockStmt block = methodDeclaration.getBody().get();
        for(Expression s: newStatements) {
            this.neededVariablesTypes.put(((VariableDeclarationExpr) s).getVariable(0).getNameAsString(), "");
            block.addStatement(0, s);
        }

        if (combinationType != CombinationType.NONE) {
            //Extracting assertions
            assertions = new ArrayList<>();
            for (Statement s: methodDeclaration.getBody().get().getStatements()) {
                if (s.toString().startsWith("assert")) {
                    assertions.add(s);
                }
            }
            for (Statement s: assertions) {
                methodDeclaration.getBody().get().remove(s);
            }
        }

    }

    public void finalizeAssertions() {
        if (combinedMethod) {
            String fileName = classAndMethodName.split("_")[0] + "_" + methodDeclaration.getNameAsString();

            methodDeclaration.getBody().get().getStatements().removeIf(s -> {
                if (s.toString().contains("com.od.TestSplitter.Transformator.ObjectRecorder.writeObject") ||
                        s.toString().contains("com.od.TestSplitter.Transformator.ObjectRecorder.finalizeWriting")) {
                    return true;
                }
                return false;
            });

            for (Statement s : assertions) {
                int count = 0;
                if (s.toString().startsWith("assertEquals")) {
                    MethodCallExpr exp = (MethodCallExpr) ((ExpressionStmt) s).getExpression();

                    NameExpr clazz = new NameExpr("com.od.TestSplitter.Transformator.ObjectRecorder");
                    MethodCallExpr call = new MethodCallExpr(clazz, "readObject");
                    call.addArgument(new StringLiteralExpr(fileName));
                    call.addArgument(new IntegerLiteralExpr(0));
                    call.addArgument(new IntegerLiteralExpr(count++));

                    exp.setArgument(0, call);

                    methodDeclaration.getBody().get().addStatement(exp);
                }
            }
        }
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

    private void addAllCombinations(MethodVisitorForSplit visitor, List<GeneratedMethod> methods) {
        for (GeneratedMethod gm: methods) {
            int newMethodIndex = ++visitor.methodCounter;
            gm.methodDeclaration.setName("generatedU" + newMethodIndex);
            gm.addWritesForAssertions();
            visitor.addGeneratedMethod(gm);
        }
    }

    private GeneratedMethod createCombination(List<Expression> expressions) {
        GeneratedMethod gm = new GeneratedMethod(methodDeclaration.clone(), neededVariables, classAndMethodName,
            neededVariablesTypes, fieldMap, fileIndex, expressions);
        gm.addReadStatements();
        return gm;
    }

    void addWritesForAssertions() {
        int count = 0;
        String fileName = classAndMethodName.split("_")[0] + "_" + methodDeclaration.getNameAsString();
        for (Statement s: assertions) {
            if (s.toString().startsWith("assertEquals")) {
                Expression actual = ((MethodCallExpr) ((ExpressionStmt) s).getExpression()).getArgument(1);

                NameExpr objectRecorderClass = new NameExpr(
                        "com.od.TestSplitter.Transformator.ObjectRecorder");
                MethodCallExpr writeCallExpr = new MethodCallExpr(objectRecorderClass, "writeObject");
                writeCallExpr.addArgument(new StringLiteralExpr(fileName));
                writeCallExpr.addArgument(actual);
                writeCallExpr.addArgument(new IntegerLiteralExpr(0));
                methodDeclaration.getBody().get().addStatement(writeCallExpr);
                count++;
            }
        }

        if (count > 0) {
            NameExpr clazz = new NameExpr("com.od.TestSplitter.Transformator.ObjectRecorder");
            MethodCallExpr call = new MethodCallExpr(clazz, "finalizeWriting");
            call.addArgument(new StringLiteralExpr(fileName));
            call.addArgument(new IntegerLiteralExpr(0));
            methodDeclaration.getBody().get().addStatement(call);
        }

        recordedAssertions = count;
    }

    private List<GeneratedMethod> processCombinations(List<String> variableNames, List<String> shortTypeNames,List<String> fullTypeNames) {

        List<GeneratedMethod> allCombinations = new ArrayList<>();
        for (int i = 0 ; i < variableNames.size(); i++) {
            // Single combination with all possible values
            int options1 = ObjectRecorder.readSpecificObjectCount(fullTypeNames.get(i));

            for (int iOptions = 0; iOptions < options1; iOptions++) {
                List<Expression> statements = new ArrayList<>(2);
                statements.add(toSpecificReadExpr(variableNames.get(i), shortTypeNames.get(i), fullTypeNames.get(i), iOptions));

                if (combinationType == CombinationType.SINGLE)
                    allCombinations.add(createCombination(statements));

                for (int j = i+1; j < variableNames.size(); j++) {
                    // Pairwise combination
                    int options2 = ObjectRecorder.readSpecificObjectCount(fullTypeNames.get(j));
                    for (int jOptions = 0; jOptions < options2; jOptions++) {
                        statements.add(toSpecificReadExpr(variableNames.get(j), shortTypeNames.get(j), fullTypeNames.get(j), jOptions));

                        if (combinationType == CombinationType.PAIR_WISE) {
                            allCombinations.add(createCombination(statements));
                        }

                        statements.remove(1);
                    }
                }
            }
        }

        return allCombinations;
    }

    public void addReadStatements(HashMap<String, String> map, MethodVisitorForSplit visitor) {
        if (fileIndex == 0)
            return;

        List<String> variableNames = new ArrayList<>();
        List<String> shortTypeNames = new ArrayList<>();
        List<String> fullTypeNames = new ArrayList<>();
        for (Map.Entry<String, String> entry : neededVariablesTypes.entrySet()) {
            HashMap typeMap = (HashMap) TestParser.typeMap.get(classAndMethodName);
            String variableName = entry.getKey();
            String shortTypeName = entry.getValue();
            Type type = JavaParser.parseType(shortTypeName);
            String boxedTypeName = shortTypeName;
            if (type.isPrimitiveType()) {
                boxedTypeName = ((PrimitiveType) type).toBoxedType().toString();
            }
            String fullTypeName = (String) typeMap.get(boxedTypeName);
            if (map.containsKey(fullTypeName)) {
                if (ObjectRecorder.readSpecificObjectCount(fullTypeName) < 2) {
                    continue;
                }
                variableNames.add(variableName);
                shortTypeNames.add(shortTypeName);
                fullTypeNames.add(fullTypeName);
            }
        }
        List<GeneratedMethod> combinations = processCombinations(variableNames, shortTypeNames, fullTypeNames);
        addAllCombinations(visitor, combinations);


        BlockStmt block = methodDeclaration.getBody().get();
        List<String> list = new ArrayList<>(neededVariablesTypes.keySet());
        Collections.sort(list, Collections.reverseOrder());
        int readIndex = list.size() + fieldMap.size() - 1;
        for (String variableName: list) {
            String typeName = neededVariablesTypes.get(variableName);
            if (!typeName.equals("")) {
                Expression readExpr = toReadExpr(variableName, neededVariablesTypes.get(variableName), fileIndex, false, readIndex);
                block.addStatement(0, readExpr);
            }
            readIndex--;
        }

        list = new ArrayList<>(fieldMap.keySet());
        Collections.sort(list, Collections.reverseOrder());
        for (String variableName: list) {
            Expression readExpr = toReadExpr(variableName, fieldMap.get(variableName), fileIndex, true, readIndex--);
            block.addStatement(0, readExpr);
        }
    }

    public void addReadStatements() {
        if (fileIndex == 0)
            return;

        BlockStmt block = methodDeclaration.getBody().get();
        List<String> list = new ArrayList<>(neededVariablesTypes.keySet());
        Collections.sort(list, Collections.reverseOrder());
        int readIndex = list.size() + fieldMap.size() - 1;
        for (String variableName: list) {
            String typeName = neededVariablesTypes.get(variableName);
            if (!typeName.equals("")) {
                Expression readExpr = toReadExpr(variableName, neededVariablesTypes.get(variableName), fileIndex, false, readIndex);
                block.addStatement(0, readExpr);
            }
            readIndex = readIndex - 1;
        }

        list = new ArrayList<>(fieldMap.keySet());
        Collections.sort(list, Collections.reverseOrder());
        for (String variableName: list) {
            Expression readExpr = toReadExpr(variableName, fieldMap.get(variableName), fileIndex, true, readIndex--);
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

    private Expression toWriteExpr(String variable) {
        NameExpr objectRecorderClass = new NameExpr(
                "com.od.TestSplitter.Transformator.ObjectRecorder");
        MethodCallExpr writeCallExpr = new MethodCallExpr(objectRecorderClass, "writeObject");
        writeCallExpr.addArgument(new StringLiteralExpr(classAndMethodName));
        writeCallExpr.addArgument(variable);
        return writeCallExpr;
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
    private Expression toReadExpr(String variableName, String typeName, int index, boolean isThis, int readIndex) {
        Type type = JavaParser.parseType(typeName);
        Type castType = type;
        if (type.isPrimitiveType()) {
            castType = ((PrimitiveType) type).toBoxedType();
        }
        NameExpr clazz = new NameExpr("com.od.TestSplitter.Transformator.ObjectRecorder");
        MethodCallExpr call = new MethodCallExpr(clazz, "readObject");
        call.addArgument(new StringLiteralExpr(classAndMethodName));
        call.addArgument(new IntegerLiteralExpr(index));
        call.addArgument(new IntegerLiteralExpr(readIndex));
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

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }
}
