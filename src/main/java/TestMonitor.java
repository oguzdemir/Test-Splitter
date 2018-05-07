import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class TestMonitor {

    private static ArrayList<String> executionTrace = new ArrayList<>();

    public static void visitMethod(String visited) {
        executionTrace.add(visited);
    }

    public static void finalizeWriting() {
        System.out.println("Size of trace: " + executionTrace.size());
    }
}
