import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;

/**
 * This file extracts the statements for variable declarations and assertions in the test source via JavaParser.
 *
 * Write Step:
 * Before each assertion, all of the previously declared variables are sent a function, where they are serialized and written to disk.
 *
 * Read Step:
 * The generated split tests are created together with the proper read functions.
 * These read calls read the serialized object, deserialize and downcast to proper object.
 *
 * Created by od on 25.02.2018.
 */
public class TestParser {

    static ArrayList<MethodDeclaration> methods;

    static class MyVisitor extends VoidVisitorAdapter<Object> {
        @Override
        public void visit(MethodDeclaration n, Object arg) {

            if (n.getName().toString().equals("testS0")) {
                boolean finished = false;

                ArrayList<String> variables = new ArrayList<>();
                ArrayList<String> types = new ArrayList<>();
                ArrayList<Statement> statements = new ArrayList<>();

                //Collect variable declarations and assert statements
                for (Statement statement : n.getBody().get().getStatements()) {
                    if (statement instanceof ExpressionStmt &&
                            ((ExpressionStmt) statement).getExpression() instanceof VariableDeclarationExpr) {
                        VariableDeclarationExpr stmt = (VariableDeclarationExpr) (((ExpressionStmt) statement).getExpression());
                        for (VariableDeclarator declarator : stmt.getVariables()) {
                            variables.add(declarator.getName().toString());
                            types.add(declarator.getType().toString());
                        }
                    }

                    if (statement instanceof ExpressionStmt &&
                            ((ExpressionStmt) statement).getExpression() instanceof MethodCallExpr &&
                            statement.toString().startsWith("assert")) {
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
                                if (count == index)
                                    break;
                                count++;
                                continue;
                            }

                            ClassOrInterfaceType type = JavaParser.parseClassOrInterfaceType(types.get(i));

                            NameExpr clazz = new NameExpr("Transformator.ObjectRecorder");
                            MethodCallExpr call = new MethodCallExpr(clazz, "readObject");
                            call.addArgument(new IntegerLiteralExpr(index));
                            call.addArgument(new StringLiteralExpr(var));
                            CastExpr castExpr = new CastExpr(type, call);
                            VariableDeclarator declarator = new VariableDeclarator(type, var, castExpr);
                            VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr(declarator);
                            block.addStatement(0, variableDeclarationExpr);
                        }

                        // create a method
                        EnumSet<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
                        MethodDeclaration method = new MethodDeclaration(modifiers, new VoidType(), "generatedU" + index);
                        method.addAnnotation(n.getAnnotation(0));
                        // add a body to the method
                        block.addStatement(s.clone());
                        method.setBody(block);
                        methods.add(method);
                        block = new BlockStmt();
                        index++;

                        if (index == statements.size())
                            break;
                    } else
                        block.addStatement(s.clone());
                }

                // Inserting Writes to the actual source code.
                variables.remove(0);
                types.remove(0);
                index = 0;
                ArrayList<Expression> expressions = new ArrayList<>();
                for (String s : variables) {
                    if (s.equals("%")) {
                        Statement statement = statements.get(index);

                        int ind = ((BlockStmt) ((ExpressionStmt) statement).getParentNode().get()).getStatements().indexOf(statement);
                        NameExpr clazz = new NameExpr("Transformator.ObjectRecorder");
                        MethodCallExpr call = new MethodCallExpr(clazz, "finalizeWriting");
                        ((BlockStmt) ((ExpressionStmt) statement).getParentNode().get()).addStatement(ind, call);
                        for (Expression expression : expressions) {
                            ((BlockStmt) ((ExpressionStmt) statement).getParentNode().get()).addStatement(ind, expression);
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

    public static void main(String... args) throws Exception {
        System.out.println("Working Directory = " +
                System.getProperty("user.dir"));

        CompilationUnit cu = JavaParser.parse(new FileInputStream(
                new File(".\\src\\main\\java\\Sample1\\SingleLinkedListTest.java")));
        cu.accept(new MyVisitor(), null);
        ClassOrInterfaceDeclaration cls = cu.getClassByName("SingleLinkedListTest").get();
        for (MethodDeclaration m : methods)
            cls.addMember(m);

        cls.setName("GeneratedTest");
        FileWriter fw = new FileWriter(new File(".\\src\\main\\java\\Sample1\\GeneratedTest.java"));
        fw.append(cu.toString().replaceAll("ı", "i"));
        fw.flush();
        fw.close();
    }
}
