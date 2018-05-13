package TestSplitter.Transformator;

import com.thoughtworks.xstream.XStream;
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
    private static int writeIndex = 1;

    private static ObjectInputStream in;
    private static int readIndex = 0;

    public static void writeObject(Object object) {
        try {

            if (out == null) {
                out = xstream
                    .createObjectOutputStream(new FileWriter(new File("out" + writeIndex)));
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
            writeIndex++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Object readObject(int index, String objectName) {
        try {
            if (index != readIndex) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e) {

                    }
                }
                readIndex = index;
                in = xstream.createObjectInputStream(new FileReader(new File("out" + index)));
            }

            return in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
