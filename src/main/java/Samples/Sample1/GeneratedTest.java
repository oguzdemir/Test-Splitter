package Samples.Sample1;

import Transformator.ObjectRecorder;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class GeneratedTest {

    @Test
    public void testS0() {
        // system test given as input
        SingleLinkedList l = new SingleLinkedList();
        l.add(1);
        Transformator.ObjectRecorder.writeObject(l);
        Transformator.ObjectRecorder.finalizeWriting();
        assertTrue(l.size == 1 && l.header.elem == 1 && l.header.next == null);
        // test boundary -- default: each test assertion defines test boundary
        // another option: each public method invocation defines a test boundary
        // pre-state for next test -- save to file/read from file
        l.add(0);
        Transformator.ObjectRecorder.writeObject(l);
        Transformator.ObjectRecorder.finalizeWriting();
        assertTrue(l.size == 2 && l.header.elem == 0 && l.header.next.elem == 1 && l.header.next.next == null);
    }

    @Test
    public void testU0() {
        // unit test to create as output
        SingleLinkedList l = new SingleLinkedList();
        l.add(1);
        assertTrue(l.header.elem == 1 && l.header.next == null);
    }

    @Test
    public void testU1() {
        // unit test to create as output
        SingleLinkedList l = (SingleLinkedList) ObjectRecorder.readObject(1, "l");
        l.add(0);
        assertTrue(l.size == 2 && l.header.elem == 0 && l.header.next.elem == 1 && l.header.next.next == null);
    }

    @Test
    public void generatedU0() {
        // system test given as input
        SingleLinkedList l = new SingleLinkedList();
        l.add(1);
        assertTrue(l.size == 1 && l.header.elem == 1 && l.header.next == null);
    }

    @Test
    public void generatedU1() {
        SingleLinkedList l = (SingleLinkedList) Transformator.ObjectRecorder.readObject(1, "l");
        // pre-state for next test -- save to file/read from file
        l.add(0);
        assertTrue(l.size == 2 && l.header.elem == 0 && l.header.next.elem == 1 && l.header.next.next == null);
    }
}
