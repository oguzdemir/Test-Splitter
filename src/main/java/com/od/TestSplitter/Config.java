package com.od.TestSplitter;

import com.od.TestSplitter.Transformator.ObjectRecorder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Config {
    public static String targetClassName = null;
    public static TargetType targetType = TargetType.ALL_METHODS;
    public static SplitType splitType = SplitType.ASSERTION;
    public static CombinatorialType combinatorialType = CombinatorialType.SINGLE;
    public static Set<String> targetMethodNames = new HashSet<>();
    public static Set<String> splitTargetMethodNames = new HashSet<>();

    public static String projectPath = null;
    public static String projectSplittedPath = null;
    public static String projectIntermediatePath = null;
    public static boolean runningMainClass = false;

    public static ConcurrentHashMap canonicalTypeMap;
    public static ConcurrentHashMap valueCountMap;

    public enum TargetType {
        ALL_METHODS, METHOD_NAME
    }

    public enum SplitType {
        ASSERTION, ALL_METHODS, METHOD_NAME
    }

    public enum CombinatorialType {
        NONE, SINGLE, PAIR_WISE
    }

    /**
     * Usage:
     * -p <Path to test source file>
     * (optional) -c <Target class name> default: all classes are processed.
     * (optional, repeated) -t <Target method name>, default: all methods in the class are
     *      processed. <Target class name> should be set.
     * (optional, repeated) -s <Split method name>, default: all method calls in the function
     *      level of the target method are considered as split point
     * -a : Enables splitting in assertions. Splitting by method name is disabled. If there is a
     *      group of assertions, the last one is considered as the split point.
     */
    public static void applyConfigs(String[] args) throws IllegalArgumentException {
        for (int i = 0; i < args.length; i++) {
            String option = args[i];
            String nextArgument = null;
            if (i + 1 < args.length) {
                nextArgument = args[i + 1];
            }
            // Skip the next argument if the option is not -a.
            i++;
            switch (option) {
                case "-p":
                    validateOption("Path should be specified after option -p",
                            nextArgument);
                    projectPath = nextArgument;
                    break;
                case "-c":
                    validateOption("Class name should be specified after option -c",
                            nextArgument);
                    targetClassName = nextArgument;
                    break;
                case "-t":
                    validateOption("Target method should be specified after option -t",
                            nextArgument);
                    targetMethodNames.add(nextArgument);
                    break;
                case "-s":
                    validateOption("Point of split method should be specified after option -s",
                            nextArgument);
                    splitTargetMethodNames.add(nextArgument);
                    break;
                case "-a":
                    i--;
                    splitType = SplitType.ASSERTION;
                    break;
                default:
                    throw new IllegalArgumentException("Option is not recognized: " + option);
            }
        }

        if (projectPath == null) {
            throw new IllegalArgumentException("Path is not specified.");
        }

        if (projectPath.charAt(projectPath.length() -1) != '/') {
            projectPath = projectPath + "/";
        }

        if (splitType != SplitType.ASSERTION) {
            if (splitTargetMethodNames.size() > 0) {
                splitType = SplitType.METHOD_NAME;
            }
        }

        if (targetMethodNames.size() > 0) {
            targetType = TargetType.METHOD_NAME;
        }


        projectSplittedPath = projectPath.substring(0, projectPath.length() - 1) + "_splitted/";
        projectIntermediatePath = projectPath.substring(0, Config.projectPath.length() - 1) + "_intermediate/";

    }

    public static void fetchCanonicalTypeMap() {
        canonicalTypeMap = ObjectRecorder.readTypeMap(projectIntermediatePath);
    }

    public static void fetchValueCountMap() {
        valueCountMap = ObjectRecorder.readCountMap(projectIntermediatePath);
    }

    public static String getCannonicalType(String classAndMethodName, String shortClassName) {
        HashMap map = (HashMap) canonicalTypeMap.get(classAndMethodName);

        if (map == null)
            return null;

        return (String) map.get(shortClassName);
    }
    private static void validateOption(String errorMsg, String option) {
        if (option == null || option.startsWith("-")) {
            throw new IllegalArgumentException(errorMsg);
        }
    }
}
