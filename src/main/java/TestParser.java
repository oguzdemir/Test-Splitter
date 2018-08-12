import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * This file extracts the statements for variable declarations and assertions in the test source via
 * JavaParser. <p> Write Step: Before each assertion, all of the previously declared variables are
 * sent a function, where they are serialized and written to disk. <p> Read Step: The generated
 * split tests are created together with the proper read functions. These read calls read the
 * serialized object, deserialize and downcast to proper object. <p> Created by od on 25.02.2018.
 */
public class TestParser {

    enum TargetType {
        ALL_METHODS, METHOD_NAME
    }

    enum SplitType {
        ASSERTION, ALL_METHODS, METHOD_NAME
    }

    static ArrayList<MethodDeclaration> methods;

    static class MyVisitor extends VoidVisitorAdapter<Object> {

        Set<String> targetNames;
        Set<String> splitNames;
        TargetType targetType;
        SplitType splitType;

        public MyVisitor(Set<String> targetNames, Set<String> splitNames, TargetType targetType,
            SplitType splitType) {
            this.targetNames = targetNames;
            this.splitNames = splitNames;
            this.targetType = targetType;
            this.splitType = splitType;
        }

        @Override
        public void visit(MethodDeclaration n, Object arg) {

            if (targetType == TargetType.ALL_METHODS ||
                targetNames.contains(n.getName().toString())) {
                boolean finished = false;

                ArrayList<String> variables = new ArrayList<>();
                ArrayList<String> types = new ArrayList<>();
                ArrayList<Statement> statements = new ArrayList<>();

                //Collect variable declarations and assert statements
                Object[] statementArray = n.getBody().get().getStatements().toArray();
                for (int i = 0; i < statementArray.length; i++) {
                    Statement statement = (Statement) statementArray[i];
                    if (statement instanceof ExpressionStmt &&
                        ((ExpressionStmt) statement)
                            .getExpression() instanceof VariableDeclarationExpr) {
                        VariableDeclarationExpr stmt = (VariableDeclarationExpr) (((ExpressionStmt) statement)
                            .getExpression());
                        for (VariableDeclarator declarator : stmt.getVariables()) {
                            variables.add(declarator.getName().toString());
                            types.add(declarator.getType().toString());
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
                            } else if(!statementArray[i + 1].toString().startsWith("assert")){
                                split = true;
                            }
                        }
                        else {
                            if (splitType == SplitType.ALL_METHODS) {
                                split = true;
                            }
                            else {
                                for (String spl : splitNames) {
                                    if (spl.contains(methodName)) {
                                        split = true;
                                    }
                                }
                            }
                        }
                    }
                    if (split) {
                        variables.add("%");
                        types.add("%");
                        statements.add(statement);
                    }
                }

                methods = new ArrayList<>();

                // Generating splitted methods.
                variables.add(0, "%");
                types.add(0, "%");

                BlockStmt block = new BlockStmt();
                int index = 0;
                for (Statement s : n.getBody().get().getStatements()) {
                    if (s.equals(statements.get(index))) {

                        int count = 0;
                        for (int i = 0; i < variables.size(); i++) {
                            String var = variables.get(i);
                            if (var.equals("%")) {
                                if (count == index) {
                                    break;
                                }
                                count++;
                                continue;
                            }

                            Type type;
                            String typeName = types.get(i);
                            switch(typeName) {
                                case "int":
                                    type = PrimitiveType.intType();
                                    break;
                                case "float":
                                    type = PrimitiveType.floatType();
                                    break;
                                case "long":
                                    type = PrimitiveType.longType();
                                    break;
                                case "double":
                                    type = PrimitiveType.doubleType();
                                    break;
                                case "byte":
                                    type = PrimitiveType.byteType();
                                    break;
                                case "boolean":
                                    type = PrimitiveType.booleanType();
                                    break;
                                case "short":
                                    type = PrimitiveType.shortType();
                                    break;
                                case "char":
                                    type = PrimitiveType.charType();
                                    break;
                                default:
                                    type = JavaParser.parseClassOrInterfaceType(typeName);
                            }
                            NameExpr clazz = new NameExpr("Transformator.ObjectRecorder");
                            MethodCallExpr call = new MethodCallExpr(clazz, "readObject");
                            call.addArgument(new IntegerLiteralExpr(index));
                            call.addArgument(new StringLiteralExpr(var));
                            CastExpr castExpr = new CastExpr(type, call);
                            VariableDeclarator declarator = new VariableDeclarator(type, var,
                                castExpr);
                            VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr(
                                declarator);
                            block.addStatement(0, variableDeclarationExpr);
                        }

                        // create a method
                        EnumSet<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
                        MethodDeclaration method = new MethodDeclaration(modifiers, new VoidType(),
                            "generatedU" + index);
                        method.addAnnotation(n.getAnnotation(0));
                        // add a body to the method
                        block.addStatement(s.clone());
                        method.setBody(block);
                        methods.add(method);
                        block = new BlockStmt();
                        index++;

                        if (index == statements.size()) {
                            break;
                        }
                    } else {
                        block.addStatement(s.clone());
                    }
                }

                // Inserting Writes to the actual source code.
                variables.remove(0);
                types.remove(0);
                index = 0;
                ArrayList<Expression> expressions = new ArrayList<>();
                for (String s : variables) {
                    if (s.equals("%")) {
                        Statement statement = statements.get(index);

                        int ind = ((BlockStmt) ((ExpressionStmt) statement).getParentNode().get())
                            .getStatements().indexOf(statement);
                        NameExpr clazz = new NameExpr("Transformator.ObjectRecorder");
                        MethodCallExpr call = new MethodCallExpr(clazz, "finalizeWriting");
                        ((BlockStmt) ((ExpressionStmt) statement).getParentNode().get())
                            .addStatement(ind, call);
                        for (Expression expression : expressions) {
                            ((BlockStmt) ((ExpressionStmt) statement).getParentNode().get())
                                .addStatement(ind, expression);
                        }
                        index++;
                        continue;
                    }

                    NameExpr clazz = new NameExpr("Transformator.ObjectRecorder");
                    MethodCallExpr call = new MethodCallExpr(clazz, "writeObject");
                    call.addArgument(s);
                    expressions.add(call);
                }
            }
            super.visit(n, arg);
        }
    }

    public static void validateOption (String errorMsg, String option) {
        if (option == null || option.startsWith("-")) {
            throw new IllegalArgumentException(errorMsg);
        }
    }
    public static void main(String[] args) throws Exception {

        /**
         * Usage:
         * -p <Path to test source file>
         * -c <Target class name>
         * (optional, repeated) -t <Target method name>, default: all methods in the class are
         *      processed.
         * (optional, repeated) -s <Split method name>, default: all method calls in the function
         *      level of the target method are considered as split point
         * -a : Enables splitting in assertions. Splitting by method name is disabled. If there is a
         *      group of assertions, the last one is considered as the split point.
         */

        String path = null;
        String className = null;
        Set<String> targetNames = new HashSet<>();
        Set<String> splitNames = new HashSet<>();
        TargetType targetType = TargetType.ALL_METHODS;
        SplitType splitType = null;

        for (int i = 0; i < args.length; i++) {
            String option = args[i];
            String nextArgument = null;
            if (i + 1 < args.length) {
                nextArgument = args[i+1];
            }
            // Skip the next argument if the option is not -a.
            i++;
            switch(option) {
                case "-p":
                    validateOption("Path should be specified after option -p",
                        nextArgument);
                    path = nextArgument;
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
                    targetNames.add(nextArgument);
                    break;
                case "-a":
                    i--;
                    splitType = SplitType.ASSERTION;
                    break;
                default:
                    throw new IllegalArgumentException("Option is not recognized: " + option);
            }
        }

        if (splitType == null) {
            if (splitNames.size() > 0) {
                splitType = SplitType.METHOD_NAME;
            }
        }

        if (targetNames.size() > 0) {
            targetType = TargetType.METHOD_NAME;
        }

        if (path == null) {
            throw new IllegalArgumentException("Path is not specified.");
        }

        if (className == null) {
            throw new IllegalArgumentException("Class name is not specified.");
        }

        if (System.getProperty("os.name").startsWith("Windows")) {
            path.replaceAll("/", "\\\\");
        }

        CompilationUnit cu = JavaParser.parse(new FileInputStream(new File(path)));
        cu.accept(new MyVisitor(targetNames, splitNames, targetType, splitType), null);
        ClassOrInterfaceDeclaration cls = cu.getClassByName(className).get();
        for (MethodDeclaration m : methods) {
            cls.addMember(m);
        }

        cls.setName("GeneratedTest");
        FileWriter fw = new FileWriter(new File(path.replace(className, "GeneratedTest")));
        fw.append(cu.toString().replaceAll("ı", "i"));
        fw.flush();
        fw.close();
    }
}
