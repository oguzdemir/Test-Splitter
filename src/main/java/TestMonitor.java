import java.io.FileWriter;
import java.io.IOException;

public class TestMonitor {

    private static FileWriter fileWriter;

    public static void visitMethod(String visited) {
        try {
            if (fileWriter == null) {
                fileWriter = new FileWriter("agentOut.txt", true);
            }
            fileWriter.append(visited + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void finalizeWriting() {
        if (fileWriter != null) {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
