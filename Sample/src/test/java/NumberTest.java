import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class NumberTest {

    @Test
    public void testMin() {
        int x = 1;
        int y = 2;
        int z = 3;
        assertEquals(1, Number.min(x, y, z));
        com.od.TestSplitter.Transformator.ObjectRecorder.writeObject("NumberTest_testMin", x, 1);
        com.od.TestSplitter.Transformator.ObjectRecorder.writeObject("NumberTest_testMin", y, 1);
        com.od.TestSplitter.Transformator.ObjectRecorder.writeObject("NumberTest_testMin", z, 1);
        com.od.TestSplitter.Transformator.ObjectRecorder.finalizeWriting("NumberTest_testMin", 1);
        x = 10;
        assertEquals(2, Number.min(x, y, z));
        com.od.TestSplitter.Transformator.ObjectRecorder.writeObject("NumberTest_testMin", x, 2);
        com.od.TestSplitter.Transformator.ObjectRecorder.writeObject("NumberTest_testMin", y, 2);
        com.od.TestSplitter.Transformator.ObjectRecorder.writeObject("NumberTest_testMin", z, 2);
        com.od.TestSplitter.Transformator.ObjectRecorder.finalizeWriting("NumberTest_testMin", 2);
        y = 5;
        assertEquals(3, Number.min(x, y, z));
    }
}
