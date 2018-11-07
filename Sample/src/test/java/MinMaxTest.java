import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class MinMaxTest {

    @Test
    public void minMaxTest1() {
        int x = 1;
        int y = 2;
        assertEquals(true, true);
        com.od.TestSplitter.Transformator.ObjectRecorder.writeObject("MinMaxTest_minMaxTest1", x, 1);
        com.od.TestSplitter.Transformator.ObjectRecorder.writeObject("MinMaxTest_minMaxTest1", y, 1);
        com.od.TestSplitter.Transformator.ObjectRecorder.finalizeWriting("MinMaxTest_minMaxTest1", 1);
        System.out.println("Testing min");
        assertEquals(1, MinMax.min(x, y));
        com.od.TestSplitter.Transformator.ObjectRecorder.writeObject("MinMaxTest_minMaxTest1", x, 2);
        com.od.TestSplitter.Transformator.ObjectRecorder.writeObject("MinMaxTest_minMaxTest1", y, 2);
        com.od.TestSplitter.Transformator.ObjectRecorder.finalizeWriting("MinMaxTest_minMaxTest1", 2);
        System.out.println("Testing max");
        assertEquals(2, MinMax.max(x, y));
    }
}
