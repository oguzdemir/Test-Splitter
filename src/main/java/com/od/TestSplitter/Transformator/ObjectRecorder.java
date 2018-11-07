package com.od.TestSplitter.Transformator;

import com.od.TestSplitter.TestParser;
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
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by od on 25.02.2018.
 */
public class ObjectRecorder {

    public static ObjectRecorder getInstance() {
        return instance;
    }

    private static final String SNAPSHOT_URL = "./snapshots/";
    public static final String SNAPSHOT_URL_COMBINATION = "./all_objects/";
    private static ObjectRecorder instance = new ObjectRecorder();

    public XStream xstream;

    // Objects are firstly stored in a list, then serialized as a list, mapped with user method
    private ConcurrentHashMap<String, LinkedList<Object>> writtenObjects;

    // Object are firstly deserialized as list, then returned one at a time, mapped with user method
    private ConcurrentHashMap<String, LinkedList<Object>> readObjects;

    private ConcurrentHashMap<String, Set<Object>> allObjects;

    private ConcurrentHashMap<String, HashMap<String,String>> typeMap;

    private ObjectRecorder() {
        SplitterJavaReflectionProvider splitterJavaReflectionProvider = new SplitterJavaReflectionProvider();
        xstream = new XStream(splitterJavaReflectionProvider);
        xstream.registerConverter(
            new SplitterReflectionConverter(xstream.getMapper(), splitterJavaReflectionProvider,
                Serializable.class),
            XStream.PRIORITY_LOW);

        writtenObjects = new ConcurrentHashMap<>();
        readObjects = new ConcurrentHashMap<>();
        allObjects = new ConcurrentHashMap<>();
        typeMap = new ConcurrentHashMap<>();

        File file = new File(SNAPSHOT_URL);
        if (!file.exists()) {
            file.mkdirs();
        }

        file = new File(SNAPSHOT_URL_COMBINATION);
        if (!file.exists()) {
            file.mkdirs();
        }

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            public void run()
            {
                System.err.println("Shutdown Hook is running for recording!");
                ObjectRecorder.finalizeAll();
                System.err.println("Application Terminating ...");
            }
        });
    }

    public static Converter getConverter(Class cls) {
        return instance.xstream.getConverterLookup().lookupConverterForType(cls);
    }

    private void writeObjectHelper(String classAndMethodName, Object object, int index) {
        String writePath = SNAPSHOT_URL  + "out_" + classAndMethodName + "_" + index + ".xml";
        if (!writtenObjects.containsKey(writePath)) {
            LinkedList<Object> objects = new LinkedList<>();
            objects.add(object);
            writtenObjects.put(writePath, objects);
        } else {
            writtenObjects.get(writePath).addLast(object);
        }

        try {
            String className = object.getClass().getCanonicalName();
            if (!typeMap.containsKey(classAndMethodName))
                typeMap.put(classAndMethodName, new HashMap<>());

            typeMap.get(classAndMethodName).put(object.getClass().getSimpleName(), className);

            if (object.getClass().toGenericString().contains("<"))
                return;
            if (allObjects.containsKey(className)) {
                allObjects.get(className).add(object);
            } else {
                HashSet<Object> objects = new HashSet<>();
                objects.add(object);
                allObjects.put(className, objects);
            }
        } catch (Exception e) {

        }
    }

    private void finalizeAllHelper() {
        for (Map.Entry<String, Set<Object>> entry: allObjects.entrySet()) {
            if (entry.getValue().size() < 2)
                continue;
            try {
                FileWriter fw = new FileWriter(new File(SNAPSHOT_URL_COMBINATION + entry.getKey()) + ".xml");
                xstream.toXML( new LinkedList<>(entry.getValue()), fw);
                fw.flush();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            FileWriter fw = new FileWriter(new File(SNAPSHOT_URL_COMBINATION + "typeMap" + ".xml"));
            xstream.toXML( typeMap, fw);
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void finalizeWritingHelper(String classAndMethodName, int index) {
        String writePath = SNAPSHOT_URL  + "out_" + classAndMethodName + "_" + index + ".xml";
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

    private Object readObjectHelper(String classAndMethodName, int index, int readIndex) {
        String readPath = SNAPSHOT_URL  + "out_" + classAndMethodName + "_" + index + ".xml";
        if (!readObjects.containsKey(readPath) || readObjects.get(readPath).size() == 0) {
            try {
                LinkedList list = (LinkedList) xstream.fromXML(new File(readPath));
                readObjects.put(readPath, list);
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return readObjects.get(readPath).get(readIndex);
    }

    private Object readSpecificObjectHelper(String className, int index) {
        String readPath = SNAPSHOT_URL_COMBINATION  + className + ".xml";
        if (!readObjects.containsKey(readPath)) {
            try {
                LinkedList list = (LinkedList) xstream.fromXML(new File(readPath));
                readObjects.put(readPath, list);
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return readObjects.get(readPath).get(index);
    }

    public static void writeObject(String classAndMethodName, Object object, int index) {
        instance.writeObjectHelper(classAndMethodName, object, index);
    }

    public static void finalizeWriting(String classAndMethodName, int index) {
        instance.finalizeWritingHelper(classAndMethodName, index);
    }

    public static void finalizeAll() {
        instance.finalizeAllHelper();
    }

    public static Object readObject(String classAndMethodName, int index, int readIndex) {
        return instance.readObjectHelper(classAndMethodName, index, readIndex);
    }

    public static Object readSpecificObject(String className, int index) {
        return instance.readSpecificObjectHelper(className, index);
    }

    public static int readSpecificObjectCount(String className) {
        Document doc;
        int count = 0;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(TestParser.repoPath + SNAPSHOT_URL_COMBINATION + className + ".xml");
            for(int i = 0; i < doc.getChildNodes().item(0).getChildNodes().getLength(); i++) {
                if (!doc.getChildNodes().item(0).getChildNodes().item(i).getNodeName().equals("#text")) {
                    count++;
                }
            }
        } catch (Exception e) {
        }

        return count;
    }

    public static ConcurrentHashMap readTypeMap(String path) {
        return (ConcurrentHashMap) instance.xstream.fromXML(new File(path + SNAPSHOT_URL_COMBINATION + "typeMap" + ".xml"));
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
