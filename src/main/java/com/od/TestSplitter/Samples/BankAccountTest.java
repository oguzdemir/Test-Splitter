package com.od.TestSplitter.Samples;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by od on 25.02.2018.
 */
public class BankAccountTest {

    @Test
    public void testS0() {
        String a = "someString";
        String b = a.hashCode() + "";
        assertTrue(true);
        String c = "Asd";
        a = "another";
        b = a.hashCode() + "";
        assertNotEquals(a, b);
        com.od.TestSplitter.Transformator.ObjectRecorder.writeObject("BankAccountTest_testS0", a, 2);
        com.od.TestSplitter.Transformator.ObjectRecorder.writeObject("BankAccountTest_testS0", b, 2);
        com.od.TestSplitter.Transformator.ObjectRecorder.finalizeWriting("BankAccountTest_testS0", 2);
        String d = "xd";
        a = "asdasd";
        b = a;
        assertEquals(a, b);
        com.od.TestSplitter.Transformator.ObjectRecorder.writeObject("BankAccountTest_testS0", a, 3);
        com.od.TestSplitter.Transformator.ObjectRecorder.writeObject("BankAccountTest_testS0", b, 3);
        com.od.TestSplitter.Transformator.ObjectRecorder.finalizeWriting("BankAccountTest_testS0", 3);
    }
}
