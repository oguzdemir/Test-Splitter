package com.od.TestSplitter.Basic;

import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.*;

public class SplitInformation {
    public MethodDeclaration belongingMethod;
    public List<Integer> splitPoints;
    public HashMap<Integer, HashMap<String,String>> activeVariableIndexedMap;
    public HashMap<String,String> fieldMap;

    public SplitInformation(MethodDeclaration methodDeclaration, HashMap<String, String> varTypeMap, HashMap<Integer, Set<String>> varIndexMap, List<Integer> splitPoints) {
        belongingMethod = methodDeclaration;
        this.splitPoints = splitPoints;
        activeVariableIndexedMap = new HashMap<>();

        /*
         * Creating a structure like
         * indexA -> {var1:type1, var2:type2}
         * indexB -> {var1:type1, var2:type2, var3:type3}
         *
         * Basically, each split index takes the variables defined before itself.
         */
        for (Integer splitIndex : splitPoints) {
            // temp = varName -> varType
            HashMap<String, String> temp = new HashMap<>();
            varIndexMap.keySet().stream().filter( i -> i < splitIndex).forEach( key -> {
                varIndexMap.get(key).forEach(s -> temp.put(s, varTypeMap.get(s)));
            });
            activeVariableIndexedMap.put(splitIndex, temp);
        }
    }
}
