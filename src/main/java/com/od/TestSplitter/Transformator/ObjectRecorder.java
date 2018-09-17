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
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by od on 25.02.2018.
 */
public class ObjectRecorder {

    private static final String SNAPSHOT_URL = "./snapshots/";
    private static ObjectRecorder instance = new ObjectRecorder();

    private XStream xstream;

    // Objects are firstly stored in a list, then serialized as a list, mapped with user method
    private ConcurrentHashMap<String, ArrayList<Object>> writtenObjects;

    // Object are firstly deserialized as list, then returned one at a time, mapped with user method
    private ConcurrentHashMap<String, LinkedList<Object>> readObjects;


    private ObjectRecorder() {
        SplitterJavaReflectionProvider splitterJavaReflectionProvider = new SplitterJavaReflectionProvider();
        xstream = new XStream(splitterJavaReflectionProvider);
        xstream.registerConverter(
            new SplitterReflectionConverter(xstream.getMapper(), splitterJavaReflectionProvider,
                Serializable.class),
            XStream.PRIORITY_LOW);

        writtenObjects = new ConcurrentHashMap<>();
        readObjects = new ConcurrentHashMap<>();

        File file = new File(SNAPSHOT_URL);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static Converter getConverter(Class cls) {
        return instance.xstream.getConverterLookup().lookupConverterForType(cls);
    }

    private void writeObjectHelper(String writePath, Object object) {
        if (!writtenObjects.containsKey(writePath)) {
            ArrayList<Object> objects = new ArrayList<>();
            objects.add(object);
            writtenObjects.put(writePath, objects);
        } else {
            writtenObjects.get(writePath).add(object);
        }
    }

    private void finalizeWritingHelper(String writePath) {
        if (writtenObjects.containsKey(writePath)) {
            try {
                FileWriter fw = new FileWriter(
                    new File(writePath));
                xstream.toXML(writtenObjects.get(writePath), fw);
                fw.flush();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private Object readObjectHelper(String readPath) {
        if (!readObjects.containsKey(readPath)) {
            try {
                LinkedList list = (LinkedList) xstream.fromXML(readPath);
                readObjects.put(readPath, list);
            }
            catch (Exception e) {
                return null;
            }
        }
        return readObjects.get(readPath).removeFirst();
    }

    public static void writeObject(String classAndMethodName, Object object, int index) {
        instance.writeObjectHelper(SNAPSHOT_URL  + "out_" + classAndMethodName + "_" + index + ".xml", object);
    }

    public static void finalizeWriting(String classAndMethodName, int index) {
        instance.finalizeWritingHelper(SNAPSHOT_URL  + "out_" + classAndMethodName + "_" + index + ".xml");
    }

    public static Object readObject(String classAndMethodName, int index) {
        return instance.readObjectHelper(SNAPSHOT_URL  + "out_" + classAndMethodName + "_" + index + ".xml");
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
        super.doMarshal(source, writer, context);
    }

    @Override
    public boolean canConvert(Class type) {
        if (type instanceof Serializable) {
            return true;
        }
        return super.canConvert(type);
    }
}
