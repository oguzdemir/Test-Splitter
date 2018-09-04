package org.od.TestSplitter.Transformator;

import com.thoughtworks.xstream.XStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by od on 25.02.2018.
 */
public class ObjectRecorder {

    private static XStream xstream = new XStream();

    private static int readIndex = 0;
    private static ArrayList<Object> writtenObjects;
    private static int writeIndex;
    private static String methodName;
    private static ArrayList<Object> readObjects;
    private static int readObjectIndex;

    public static void writeObject(String methodName, Object object, int writeIndex) {
        if (writtenObjects == null) {
            ObjectRecorder.methodName = methodName;
            ObjectRecorder.writeIndex = writeIndex;
            writtenObjects = new ArrayList<>();
        }

        writtenObjects.add(object);
    }

    public static void finalizeWriting() {
        try {
            xstream.toXML(writtenObjects,  new FileWriter(new File("out_" + methodName + "_" + writeIndex + ".xml")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Object readObject(String methodName, int index) {
        try {
            if (index != readIndex) {
                readObjectIndex = 0;
                readObjects = (ArrayList) xstream.fromXML(new File("out_" + methodName + "_" + index + ".xml"));
                readIndex = index;
            }
        } catch (Exception e) {
            System.err.println("File cannot be opened.");
            return null;
        }
        return readObjects.get(readObjectIndex++);
    }

}
