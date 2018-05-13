package TestSplitter;

import java.lang.instrument.Instrumentation;

public class TestInstrumenter {

    /**
     * Name of the Agent
     */
    private static Instrumentation sInstrumentation;
    public static void premain(String options, Instrumentation instrumentation) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                TestSplitter.TestMonitor.finalizeWriting();
            }
        });
        TestMonitor.initialize();
        init(instrumentation);

    }

    public static void agentmain(String options, Instrumentation instrumentation) {
        init(instrumentation);
    }

    private static void init(Instrumentation instrumentation) {

        if (sInstrumentation == null) {
            sInstrumentation = instrumentation;
            sInstrumentation.addTransformer(new TestClassFileTransformer());
        }
    }
}