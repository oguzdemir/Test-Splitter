package TestSplitter;

import com.thoughtworks.xstream.mapper.Mapper;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;
import org.junit.Test;


public class TestMonitor {
    private static class StackItem {
        String method;
        boolean flag;

        public StackItem(String method){
            this.method=method;
            flag = false;
        }
    }

    // Output type
    // 0 for loading to memory
    // 1 for writing to file
    // 2 for std.out
    private static final byte OUTPUT_TYPE = 0;
    private static int counter = 0;

    // loading to memory
    private static HashSet<String> splitableMethods;
    private static Stack<StackItem> executionTrace;

    // writing to file
    private static FileWriter fileWriter;
    private static int stackDepth = 0;
    private static StringBuilder indentation;

    public static void initialize() {
        switch(OUTPUT_TYPE){
            case 0:
                executionTrace = new Stack<>();
                splitableMethods = new HashSet<>();
                break;
            case 1:
                try {
                    indentation = new StringBuilder();
                    fileWriter = new FileWriter("execution_trace.txt", false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }
    public static void visitMethod(String visited) {
        counter ++;
        switch(OUTPUT_TYPE){
            case 0:
                executionTrace.add(new StackItem(visited));
                if (isForbiddenMethod(visited)) {
                    for(StackItem s: executionTrace) {
                        if(s.flag)
                            break;
                        s.flag = true;
                    }
                }
                break;
            case 1:
                try {
                    fileWriter.append(indentation);
                    fileWriter.append(visited.replaceAll("#|/","."));
                    fileWriter.append("\n");
                    fileWriter.flush();
                    stackDepth++;
                    indentation.append("  ");

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 2:
                System.out.println(visited);
                break;
            default:
                break;
        }
    }

    public static void finalizeMethod() {
        if(OUTPUT_TYPE == 0) {
            StackItem s = executionTrace.pop();
            if(!s.flag) {
                String[] mm = s.method.split("#");
                if(!(mm[1].equals("<init>") || mm[1].equals("<clinit>")))
                    splitableMethods.add(s.method);
            }
        }
        if(OUTPUT_TYPE == 1) {
            stackDepth--;
            indentation.delete(indentation.length() - 2, indentation.length());
        }
    }

    public static void finalizeWriting() {
        switch(OUTPUT_TYPE){
            case 0:
                for(String s : splitableMethods) {
                    System.out.println("Splitable: " + s);
                }
                break;
            case 1:
                try {
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 2:
                System.out.println("Number of visited methods: " + counter);
                break;
            default:
                break;
        }
    }

    public static boolean isForbiddenMethod(String method) {
        if(method.startsWith("java/io/ObjectOutputStream#writeObject") || method.startsWith("java/sql/"))
            return true;
        return false;
    }
}
