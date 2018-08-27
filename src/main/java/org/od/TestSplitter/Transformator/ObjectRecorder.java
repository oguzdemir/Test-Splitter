package org.od.TestSplitter.Transformator;

import com.thoughtworks.xstream.XStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by od on 25.02.2018.
 */
public class ObjectRecorder {

    private static XStream xstream = new XStream();
    private static ObjectOutputStream out;

    private static ObjectInputStream in;
    private static int readIndex = 0;

    public static void writeObject(String methodName, Object object, int writeIndex) {
        try {

            if (out == null) {
                out = xstream
                    .createObjectOutputStream(new FileWriter(new File("out_" + methodName + "_" + writeIndex + ".xml")));
            }

            out.writeObject(object);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void finalizeWriting() {
        try {
            out.flush();
            out.close();
            out = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Object readObject(String methodName, int index) {
        int prevIndex = -1;
        try {
            if (index != readIndex) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e) {

                    }
                }
                prevIndex = readIndex;
                readIndex = index;
                in = xstream.createObjectInputStream(new FileReader(new File("out_" + methodName + "_" + index + ".xml")));
            }
        } catch (Exception e) {
            System.err.println("File cannot be opened.");
            return null;
        }
        try {
            return in.readObject();
        } catch (EOFException e) {
            return readObjectTryAgain(methodName, index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object readObjectTryAgain(String methodName, int index) {
        try {
            Thread.sleep(10);
            in = xstream.createObjectInputStream(new FileReader(new File("out_" + methodName + "_" + index + ".xml")));
            return in.readObject();
        } catch (Exception e) {
            return null;
        }
    }
}
