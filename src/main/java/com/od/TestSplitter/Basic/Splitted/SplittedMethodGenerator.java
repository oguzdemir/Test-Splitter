package com.od.TestSplitter.Basic.Splitted;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.od.TestSplitter.RawClass;
import com.od.TestSplitter.Basic.SplitInformation;

import java.util.*;

public class SplittedMethodGenerator extends GenericVisitorAdapter<List<SplittedMethod>, String> {

    @Override
    public List<SplittedMethod> visit(MethodDeclaration methodDeclaration, String className) {
        String classAndMethodName = className + "_" + methodDeclaration.getNameAsString();
        SplitInformation splitInformation = RawClass.splitInformationMap.get(methodDeclaration);

        if (splitInformation == null) {
            return null;
        }

        if (!methodDeclaration.getBody().isPresent()) {
            return null;
        }

        List<Integer> splitPoints = splitInformation.splitPoints;
        List<Statement> statements = methodDeclaration.getBody().get().getStatements();
        List<SplittedMethod> generatedMethods = new ArrayList<>();

        BlockStmt block = new BlockStmt();
        int fileIndexCounter = 0;
        for (int i = 0; i < statements.size(); i++) {
            block.addStatement(statements.get(i));

            if (splitPoints.contains(i)) {
                // create a methodDeclaration
                EnumSet<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
                String methodName = "generatedU" + ++RawClass.generatedMethodCounter;
                MethodDeclaration generated = new MethodDeclaration(modifiers, new VoidType(),methodName);
                for (AnnotationExpr a : methodDeclaration.getAnnotations()) {
                    generated.addAnnotation(a);
                }
                for (ReferenceType referenceType : methodDeclaration.getThrownExceptions()) {
                    generated.addThrownException(referenceType);
                }
                generated.setBody(block);

                generatedMethods.add(new SplittedMethod(methodDeclaration, generated, classAndMethodName, fileIndexCounter++));
                block = new BlockStmt();
            }
        }

        return generatedMethods;
    }
}
