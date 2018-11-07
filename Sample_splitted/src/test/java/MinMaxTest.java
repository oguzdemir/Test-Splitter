import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class MinMaxTest {

    @Test
    public void generatedU1() {
        int x = 1;
        int y = 2;
        assertEquals(true, true);
    }

    @Test
    public void generatedU2() {
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readObject("MinMaxTest_minMaxTest1", 1, 0);
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readObject("MinMaxTest_minMaxTest1", 1, 1);
        System.out.println("Testing min");
        assertEquals(1, MinMax.min(x, y));
    }

    @Test
    public void generatedU3() {
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readObject("MinMaxTest_minMaxTest1", 2, 0);
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readObject("MinMaxTest_minMaxTest1", 2, 1);
        System.out.println("Testing max");
        assertEquals(2, MinMax.max(x, y));
    }

    @Test
    public void generatedU4() {
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readObject("MinMaxTest_minMaxTest1", 1, 1);
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 0);
        System.out.println("Testing min");
        assertEquals(1, MinMax.min(x, y));
    }

    @Test
    public void generatedU5() {
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 0);
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 0);
        System.out.println("Testing min");
        assertEquals(1, MinMax.min(x, y));
    }

    @Test
    public void generatedU6() {
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 1);
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 0);
        System.out.println("Testing min");
        assertEquals(1, MinMax.min(x, y));
    }

    @Test
    public void generatedU7() {
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readObject("MinMaxTest_minMaxTest1", 1, 1);
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 1);
        System.out.println("Testing min");
        assertEquals(1, MinMax.min(x, y));
    }

    @Test
    public void generatedU8() {
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 0);
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 1);
        System.out.println("Testing min");
        assertEquals(1, MinMax.min(x, y));
    }

    @Test
    public void generatedU9() {
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 1);
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 1);
        System.out.println("Testing min");
        assertEquals(1, MinMax.min(x, y));
    }

    @Test
    public void generatedU10() {
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readObject("MinMaxTest_minMaxTest1", 1, 0);
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 0);
        System.out.println("Testing min");
        assertEquals(1, MinMax.min(x, y));
    }

    @Test
    public void generatedU11() {
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readObject("MinMaxTest_minMaxTest1", 1, 0);
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 1);
        System.out.println("Testing min");
        assertEquals(1, MinMax.min(x, y));
    }

    @Test
    public void generatedU12() {
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readObject("MinMaxTest_minMaxTest1", 2, 1);
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 0);
        System.out.println("Testing max");
        assertEquals(2, MinMax.max(x, y));
    }

    @Test
    public void generatedU13() {
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 0);
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 0);
        System.out.println("Testing max");
        assertEquals(2, MinMax.max(x, y));
    }

    @Test
    public void generatedU14() {
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 1);
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 0);
        System.out.println("Testing max");
        assertEquals(2, MinMax.max(x, y));
    }

    @Test
    public void generatedU15() {
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readObject("MinMaxTest_minMaxTest1", 2, 1);
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 1);
        System.out.println("Testing max");
        assertEquals(2, MinMax.max(x, y));
    }

    @Test
    public void generatedU16() {
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 0);
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 1);
        System.out.println("Testing max");
        assertEquals(2, MinMax.max(x, y));
    }

    @Test
    public void generatedU17() {
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 1);
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 1);
        System.out.println("Testing max");
        assertEquals(2, MinMax.max(x, y));
    }

    @Test
    public void generatedU18() {
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readObject("MinMaxTest_minMaxTest1", 2, 0);
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 0);
        System.out.println("Testing max");
        assertEquals(2, MinMax.max(x, y));
    }

    @Test
    public void generatedU19() {
        int x = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readObject("MinMaxTest_minMaxTest1", 2, 0);
        int y = (Integer) com.od.TestSplitter.Transformator.ObjectRecorder.readSpecificObject("java.lang.Integer", 1);
        System.out.println("Testing max");
        assertEquals(2, MinMax.max(x, y));
    }
}
