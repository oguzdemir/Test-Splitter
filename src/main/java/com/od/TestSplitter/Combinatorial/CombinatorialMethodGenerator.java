package com.od.TestSplitter.Combinatorial;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.od.TestSplitter.Basic.SplitInformation;
import com.od.TestSplitter.Basic.Splitted.SplittedMethod;
import com.od.TestSplitter.Config;
import com.od.TestSplitter.RawClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CombinatorialMethodGenerator {

    public static List<CombinatorialMethod> processSplittedMethods(List<SplittedMethod> splittedMethods) {
        List<CombinatorialMethod> result = new ArrayList<>();
        for (SplittedMethod splittedMethod: splittedMethods) {
            Map<String,String> varTypeMap = splittedMethod.getVarTypeMap();
            if (varTypeMap != null) {
                List<HashMap<String,Integer>> list = findAllCombinations(varTypeMap, splittedMethod.getClassAndMethodName());
                list.forEach(m -> result.add(splittedMethod.cloneForCombinatorial(m)));
            }
        }

        return result;
    }


    public static List<HashMap<String, Integer>> findAllCombinations(Map<String,String>  varMap, String classAndMethodName) {
        List<HashMap<String, Integer>> allCombinations = new ArrayList<>();
        List<String> variableNames = new ArrayList<>(varMap.keySet());

        for (int i = 0 ; i < variableNames.size(); i++) {
            // Single combination with all possible values
            String shortClassName = varMap.get(variableNames.get(i));
            String canonicalClassName = Config.getCannonicalType(classAndMethodName, shortClassName);
            if (canonicalClassName == null) {
                continue;
            }

            int options1 = (Integer) Config.valueCountMap.get(canonicalClassName);

            for (int iOptions = 0; iOptions < options1; iOptions++) {
                HashMap<String, Integer> combinationMap = new HashMap<>();
                combinationMap.put(variableNames.get(i), iOptions);


                if (Config.combinatorialType == Config.CombinatorialType.SINGLE) {
                    allCombinations.add(new HashMap<>(combinationMap));
                } else if (Config.combinatorialType == Config.CombinatorialType.PAIR_WISE){
                    for (int j = i+1; j < variableNames.size(); j++) {
                        String shortClassName2 = varMap.get(variableNames.get(j));
                        String canonicalClassName2 = Config.getCannonicalType(classAndMethodName, shortClassName2);
                        if (canonicalClassName2 == null) {
                            continue;
                        }
                        int options2 = (Integer) Config.valueCountMap.get(canonicalClassName2);
                        for (int jOptions = 0; jOptions < options2; jOptions++) {
                            combinationMap.put(variableNames.get(j), jOptions);
                            allCombinations.add(new HashMap<>(combinationMap));
                            combinationMap.remove(variableNames.get(j));
                        }
                    }
                } else {
                    System.err.println("Invalid combinatorial type.");
                }
            }
        }
        return allCombinations;
    }
}
