package com.od.TestSplitter.Basic.Splitted;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.od.TestSplitter.Combinatorial.CombinatorialMethod;
import com.od.TestSplitter.RawClass;
import com.od.TestSplitter.Basic.SplitInformation;

import java.util.*;

public class SplittedMethod {
    public MethodDeclaration parentMethod;
    private MethodDeclaration methodDeclaration;
    private String classAndMethodName;
    private int fileReadIndex;
    private TreeMap<String, Expression> variableReadStatements;
    private LinkedList<Expression> thisFieldReadStatements;
    private Map<String, String> varTypeMap;

    public SplittedMethod(MethodDeclaration parentMethod, MethodDeclaration methodDeclaration, String classAndMethodName, int fileReadIndex) {
        this.parentMethod = parentMethod; // splitted method
        this.methodDeclaration = methodDeclaration;
        this.classAndMethodName = classAndMethodName;
        this.fileReadIndex = fileReadIndex;
    }

    public void process() {
        SplitInformation splitInformation = RawClass.splitInformationMap.get(parentMethod);

        if (splitInformation == null)
            return;

        createReadStatements(splitInformation);
    }

    public void postProcess() {
        addReadStatements();
    }

    public void createReadStatements(SplitInformation splitInformation) {
        if (fileReadIndex == 0)
            return;

        variableReadStatements = new TreeMap<>();
        thisFieldReadStatements = new LinkedList<>();

        Map<String,String> fieldMap = splitInformation.fieldMap;
        varTypeMap = splitInformation.activeVariableIndexedMap.get(splitInformation.splitPoints.get(fileReadIndex - 1));

        List<String> list = new ArrayList<>(varTypeMap.keySet());
        Collections.sort(list, Collections.reverseOrder());

        int readIndex = list.size() + fieldMap.size() - 1;
        for (String variableName: list) {
            String typeName = varTypeMap.get(variableName);
            if (!typeName.equals("")) {
                Expression readExpr = toReadExpr(variableName, varTypeMap.get(variableName), fileReadIndex, false, readIndex);
                variableReadStatements.put(variableName, readExpr);
            }
            readIndex = readIndex - 1;
        }

        list = new ArrayList<>(fieldMap.keySet());
        Collections.sort(list, Collections.reverseOrder());
        for (String variableName: list) {
            Expression readExpr = toReadExpr(variableName, fieldMap.get(variableName), fileReadIndex, true, readIndex--);
            thisFieldReadStatements.addFirst(readExpr);
        }
    }

    public void addReadStatements() {
        if (fileReadIndex == 0)
            return;

        int count = 0;
        BlockStmt block = methodDeclaration.getBody().get();

        for (Expression ex: thisFieldReadStatements) {
            block.addStatement(count++, ex);
        }

        for (Map.Entry<String, Expression> entry: variableReadStatements.entrySet()) {
            block.addStatement(count++, entry.getValue());
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

    // Used to determine which methods are splitted, as they will be removed from the new test suite.
    public String getClassAndMethodName() {
        return classAndMethodName;
    }

    // Used to add to class
    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    public int getFileReadIndex() {
        return fileReadIndex;
    }

    public Map<String, String> getVarTypeMap() {
        return varTypeMap;
    }

    public CombinatorialMethod cloneForCombinatorial(HashMap<String, Integer> alteredValues) {
        EnumSet<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
        String methodName = "generatedU" + ++RawClass.generatedMethodCounter;
        MethodDeclaration combinatorial = new MethodDeclaration(modifiers, new VoidType(),methodName);
        for (AnnotationExpr a : methodDeclaration.getAnnotations()) {
            combinatorial.addAnnotation(a);
        }
        for (ReferenceType referenceType : methodDeclaration.getThrownExceptions()) {
            combinatorial.addThrownException(referenceType);
        }
        combinatorial.setBody(methodDeclaration.getBody().get().clone());

        return new CombinatorialMethod(parentMethod, combinatorial, classAndMethodName, fileReadIndex,
                variableReadStatements, thisFieldReadStatements, varTypeMap, alteredValues);
    }
}
