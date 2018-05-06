import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashSet;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Includes 3 classes for instrumenting the class file of the Test Class.
 */
class TestMethodVisitor extends MethodVisitor {

    private String methodName;
    private String className;

    TestMethodVisitor(String methodName, MethodVisitor mv, String className,
                      String description) {
        super(Opcodes.ASM5, mv);
        this.methodName = methodName;
        this.className = className;
    }

    @Override
    public void visitCode(){
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn(className + " " +methodName);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        //System.out.println("Method: " + className + ":" + methodName );
    }
}

class TestClassVisitor extends ClassVisitor {

    private String className;

    TestClassVisitor(String className, ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
        this.className = className;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
                      String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                     String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        mv = new TestMethodVisitor(name, mv, className, desc);
        return mv;
    }
}


public class TestClassFileTransformer implements ClassFileTransformer {

    private HashSet<String> packages;

    public TestClassFileTransformer() {

    }

    public TestClassFileTransformer(String... pckgs) {
        packages = new HashSet<>(pckgs.length);
        packages.addAll(Arrays.asList(pckgs));
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {

        if(packages != null) {
            if(!packages.contains(className))
                return null;
        }
        else {
            if(className.startsWith("java/") || className.startsWith("javax/") || className.startsWith("org/junit/")
                    || className.contains("sun/") || className.startsWith("org/apache/maven/surefire/")
                    || className.startsWith("junit/") || className.startsWith("com/thoughtworks/xstream/")
                    || className.startsWith("jdk/") || className.startsWith("com/intellij/")) {
                return null;
            }
        }

        //System.out.println("Classname: " + className);
        ClassReader classReader = new ClassReader(classfileBuffer);
        ClassWriter classWriter = new ClassWriter(classReader,
                /* ClassWriter.COMPUTE_FRAMES | */ClassWriter.COMPUTE_MAXS);
        TestClassVisitor visitor = new TestClassVisitor(className, classWriter);
        classReader.accept(visitor, 0);
        return classWriter.toByteArray();
    }
}