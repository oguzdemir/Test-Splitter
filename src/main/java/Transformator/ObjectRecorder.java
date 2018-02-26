package Transformator;

/**
 * Created by od on 25.02.2018.
 */
public class ObjectRecorder {

    public ObjectRecorder instance;

    private ObjectRecorder(){

    }

    public ObjectRecorder getInstance() {
        if(instance == null) {
            instance = new ObjectRecorder();
        }
        return instance;
    }

    public static Object readObject(int index, String objectName) {
        return null;
    }


}
