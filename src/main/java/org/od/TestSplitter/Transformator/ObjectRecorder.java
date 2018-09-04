package org.od.TestSplitter.Transformator;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by od on 25.02.2018.
 */
public class ObjectRecorder {

    private static XStream xstream = new XStream();

    private static int readIndex = 0;
    private static ArrayList<Object> writtenObjects;
    private static int writeIndex;
    private static String classAndMethodName;
    private static ArrayList<Object> readObjects;
    private static int readObjectIndex;

    public static void writeObject(String classAndMethodName, Object object, int writeIndex) {
        if (writtenObjects == null) {
            ObjectRecorder.classAndMethodName = classAndMethodName;
            ObjectRecorder.writeIndex = writeIndex;
            writtenObjects = new ArrayList<>();
        }

        writtenObjects.add(object);
    }

    public static void finalizeWriting() {
        try {
            FileWriter fw = new FileWriter(new File("./snapshots/out_" + classAndMethodName + "_" + writeIndex + ".xml"));
            xstream.toXML(writtenObjects, fw);
            writtenObjects = null;
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Object readObject(String classAndMethodName, int index) {
        try {
            if (index != readIndex) {
                readObjectIndex = 0;
                readObjects = (ArrayList) xstream.fromXML(new File("./snapshots/out_" + classAndMethodName + "_" + index + ".xml"));
                readIndex = index;
            }
        } catch (Exception e) {
            System.err.println("File cannot be opened.");
            return null;
        }
        return readObjects.get(readObjectIndex++);
    }

}
