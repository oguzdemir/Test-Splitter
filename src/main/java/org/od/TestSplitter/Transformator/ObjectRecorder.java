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

    // Private xstream object for serialize-deserialize
    private static XStream xstream = new XStream();

    // Objects are firstly stored in a list, then serialized as a list.
    private static ArrayList<Object> writtenObjects;

    // Object are firstly deserialized as list, then returned one at a time.
    private static ArrayList<Object> readObjects;
    // The index of the next object is stored to return the objects in order.
    private static int readObjectIndex;

    // The data stored to detect when a new method is called for read operations.
    private static String readMethod;
    private static int readIndex;

    // The data needed for writing the serialized objects.
    private static int writeIndex;
    private static String classAndMethodName;



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
            if (!classAndMethodName.equals(readMethod) || index != readIndex) {
                readObjectIndex = 0;
                readObjects = (ArrayList) xstream.fromXML(new File("./snapshots/out_" + classAndMethodName + "_" + index + ".xml"));
                readIndex = index;
                readMethod = classAndMethodName;
            }
        } catch (Exception e) {
            System.err.println("File cannot be opened.");
            return null;
        }
        return readObjects.get(readObjectIndex++);
    }

}
