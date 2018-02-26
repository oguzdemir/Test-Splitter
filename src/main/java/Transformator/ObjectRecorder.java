package Transformator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by od on 25.02.2018.
 */
public class ObjectRecorder {

    private static FileOutputStream fileOutputStream;
    private static ObjectOutputStream out;
    private static int writeIndex = 1;

    private static FileInputStream fileInputStream;
    private static ObjectInputStream in;
    private static int readIndex = 0;

    public static void writeObject(Object object)  {
        try {
            if(out == null) {
                fileOutputStream = new FileOutputStream(new File("out" + writeIndex));
                out = new ObjectOutputStream(fileOutputStream);
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
            fileOutputStream.flush();
            fileOutputStream.close();
            out = null;
            fileOutputStream = null;
            writeIndex++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Object readObject(int index, String objectName) {
        try {
            if (index != readIndex) {
                readIndex = index;
                fileInputStream = new FileInputStream(new File("out" + index));
                in = new ObjectInputStream(fileInputStream);
            }

            return in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
