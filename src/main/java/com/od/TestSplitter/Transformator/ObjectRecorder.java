package com.od.TestSplitter.Transformator;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.ImmutableFieldKeySorter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.SunUnsafeReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * Created by od on 25.02.2018.
 */
public class ObjectRecorder {

    private static ObjectRecorder instance = new ObjectRecorder();

    private XStream xstream;

    // Objects are firstly stored in a list, then serialized as a list.
    private ArrayList<Object> writtenObjects;

    // Object are firstly deserialized as list, then returned one at a time.
    private ArrayList<Object> readObjects;
    // The index of the next object is stored to return the objects in order.
    private int readObjectIndex;

    // The data stored to detect when a new method is called for read operations.
    private String readMethod;
    private int readIndex;

    // The data needed for writing the serialized objects.
    private int writeIndex;
    private String classAndMethodName;

    private ObjectRecorder() {
        SplitterJavaReflectionProvider splitterJavaReflectionProvider = new SplitterJavaReflectionProvider();
        xstream = new XStream(splitterJavaReflectionProvider);
        xstream.registerConverter(
            new SplitterReflectionConverter(xstream.getMapper(), splitterJavaReflectionProvider, Serializable.class),
        XStream.PRIORITY_LOW);
    }

    public static Converter getConverter(Class cls) {
        return instance.xstream.getConverterLookup().lookupConverterForType(cls);
    }

    private void writeObjectHelper(String classAndMethodName, Object object, int writeIndex) {
        if (writtenObjects == null) {
            this.classAndMethodName = classAndMethodName;
            this.writeIndex = writeIndex;
            writtenObjects = new ArrayList<>();
        }
        writtenObjects.add(object);
    }

    private void finalizeWritingHelper() {
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

    private Object readObjectHelper(String classAndMethodName, int index) {
        try {
            if (!classAndMethodName.equals(readMethod) || index != readIndex) {
                readObjectIndex = 0;
                readIndex = index;
                readMethod = classAndMethodName;
                readObjects = (ArrayList) xstream.fromXML(new File("./snapshots/out_" + classAndMethodName + "_" + index + ".xml"));

            }
        } catch (Exception e) {
            //System.err.println(e.getMessage());
            return null;
        }
        return readObjects.get(readObjectIndex++);
    }

    public static void writeObject(String classAndMethodName, Object object, int writeIndex) {
        instance.writeObjectHelper(classAndMethodName, object, writeIndex);
    }

    public static void finalizeWriting() {
        instance.finalizeWritingHelper();
    }

    public static Object readObject(String classAndMethodName, int index) {
        return instance.readObjectHelper(classAndMethodName, index);
    }
}

class SplitterJavaReflectionProvider extends SunUnsafeReflectionProvider {

    public SplitterJavaReflectionProvider() {
        this(new FieldDictionary(new ImmutableFieldKeySorter()));
    }

    public SplitterJavaReflectionProvider(FieldDictionary fieldDictionary) {
        super(fieldDictionary);
    }

    protected boolean fieldModifiersSupported(Field field) {
        int modifiers = field.getModifiers();
        return !Modifier.isStatic(modifiers);
    }

    public boolean fieldDefinedInClass(String fieldName, Class type) {
        Field field = fieldDictionary.fieldOrNull(type, fieldName, null);
        return field != null && fieldModifiersSupported(field);
    }


}

class SplitterReflectionConverter extends ReflectionConverter {

    public SplitterReflectionConverter(Mapper mapper,
        ReflectionProvider reflectionProvider, Class type) {
        super(mapper, reflectionProvider, type);
    }

    @Override
    protected boolean shouldUnmarshalTransientFields() {
        return true;
    }

    @Override
    protected void doMarshal(Object source, HierarchicalStreamWriter writer,
        MarshallingContext context) {
        System.out.println("X");
        super.doMarshal(source, writer, context);
    }
}
