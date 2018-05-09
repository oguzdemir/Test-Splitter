import com.thoughtworks.xstream.mapper.Mapper;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class TestMonitor {
    // Output type
    // 0 for loading to memory
    // 1 for writing to file
    // 2 for std.out
    private static final byte OUTPUT_TYPE = 1;
    private static int counter = 0;

    // loading to memory
    private static ArrayList<String> executionTrace;

    // writing to file
    private static FileWriter fileWriter;
    private static int stackDepth = 0;
    private static StringBuilder indentation;

    public static void initialize() {
        switch(OUTPUT_TYPE){
            case 0:
                executionTrace = new ArrayList<>();
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
                executionTrace.add(visited);
                break;
            case 1:
                try {

                    fileWriter.append(indentation);
                    fileWriter.append(visited.replaceAll("#|/","."));
                    fileWriter.append("\n");
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
        if(OUTPUT_TYPE == 1) {
            stackDepth--;
            indentation.delete(indentation.length() - 2, indentation.length());
        }
    }

    public static void finalizeWriting() {
        switch(OUTPUT_TYPE){
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
}
