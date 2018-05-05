package org.glavo.javah;

import org.objectweb.asm.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class HeaderGenerator {
    private static final Path[] EMPTY_PATH_ARRAY = new Path[0];
    private static final List<String> THROWABLE_NAME_LIST = Arrays.asList("Ljava/lang/Throwable;", "Ljava/lang/Error;", "Ljava/lang/Exception");
    private static final HeaderGenerator generator = new HeaderGenerator();

    private static String escape(String source) {
        StringBuilder builder = new StringBuilder();
        char ch;
        for (int i = 0; i < source.length(); i++) {
            switch (ch = source.charAt(i)) {
                case '_':
                    builder.append("_1");
                    break;
                case ';':
                    builder.append("_2");
                    break;
                case '[':
                    builder.append("_3");
                    break;
                case '/':
                    builder.append('.');
                    break;
                default:
                    if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                        builder.append(ch);
                    } else {
                        builder.append("_0").append(String.format("%04x", (int) ch));
                    }
            }
        }
        return builder.toString();
    }

    public static void generateFunctionDeclarations(ClassReader reader, PrintWriter output) {
        getGenerator().classGenerateFunctionDeclarations(reader, output);
    }

    public static void generateHeader(ClassReader reader, PrintWriter output) {
        getGenerator().classGenerateHeader(reader, output);
    }

    public static void generateHeader(byte[] classFile, PrintWriter output) {
        getGenerator().classGenerateHeader(classFile, output);
    }

    public static void generateHeader(byte[] classFileBuffer, int classFileOffset, int classFileLength, PrintWriter output) {
        getGenerator().classGenerateHeader(classFileBuffer, classFileOffset, classFileLength, output);
    }

    public static void generateHeader(String className, PrintWriter output) throws IOException {
        getGenerator().classGenerateHeader(className, output);
    }

    public static void generateHeader(InputStream input, PrintWriter output) throws IOException {
        getGenerator().classGenerateHeader(input, output);
    }

    public static HeaderGenerator getGenerator() {
        return generator;
    }

    private Path[] classPaths;
    private boolean useRuntimeClassPath;

    public HeaderGenerator() {
        this(EMPTY_PATH_ARRAY, true);
    }

    public HeaderGenerator(boolean useRuntimeClassPath) {
        this(EMPTY_PATH_ARRAY, useRuntimeClassPath);
    }

    public HeaderGenerator(Path[] classPaths) {
        this(classPaths, true);
    }

    public HeaderGenerator(Path[] classPaths, boolean useRuntimeClassPath) {
        Objects.requireNonNull(classPaths);
        this.classPaths = classPaths;
        this.useRuntimeClassPath = useRuntimeClassPath;
    }

    private boolean isThrowable(Type type) {
        String desc = type.getDescriptor();
        if (!desc.startsWith("L")) {
            return false;
        }
        if (classPaths.length == 0 && !useRuntimeClassPath) {
            return THROWABLE_NAME_LIST.contains(type.getDescriptor());
        }
        String className = type.getInternalName();
        while (true) {
            if (className == null) {
                return false;
            }
            if (className.equals("java/lang/Throwable")) {
                return true;
            }
            try {
                ClassReader reader = null;
                loop:
                for (Path path : classPaths) {
                    if (!Files.exists(path)) {
                        continue;
                    }
                    String[] ps = (className + ".class").split("/");
                    for (String p : ps) {
                        path = path.resolve(p);
                        if (!Files.exists(path)) {
                            continue loop;
                        }
                    }
                    try {
                        reader = new ClassReader(Files.newInputStream(path));
                        break;
                    } catch (IOException ignored) {
                    }
                }
                if (reader == null && useRuntimeClassPath) {
                    reader = new ClassReader(className.replace('/', '.'));
                }
                if (reader == null) {
                    return false;
                }
                className = reader.getSuperName();
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    private String typeToNative(Type tpe) {
        if (tpe == Type.BOOLEAN_TYPE) {
            return "jboolean";
        } else if (tpe == Type.BYTE_TYPE) {
            return "jbyte";
        } else if (tpe == Type.CHAR_TYPE) {
            return "jchar";
        } else if (tpe == Type.SHORT_TYPE) {
            return "jshort";
        } else if (tpe == Type.INT_TYPE) {
            return "jint";
        } else if (tpe == Type.LONG_TYPE) {
            return "jlong";
        } else if (tpe == Type.FLOAT_TYPE) {
            return "jfloat";
        } else if (tpe == Type.DOUBLE_TYPE) {
            return "jdouble";
        } else if (tpe == Type.VOID_TYPE) {
            return "void";
        } else {
            String desc = tpe.getDescriptor();
            if (desc.startsWith("[")) {
                Type elemTpe = tpe.getElementType();
                String descriptor = elemTpe.getDescriptor();
                if (descriptor.startsWith("[") || descriptor.startsWith("L")) {
                    return "jobjectArray";
                }
                return typeToNative(elemTpe) + "Array";
            }
            if (desc.equals("Ljava/lang/String;")) {
                return "jstring";
            }
            if (desc.equals("Ljava/lang/Class;")) {
                return "jclass";
            }
            if (isThrowable(tpe)) {
                return "jthrowable";
            }
            return "jobject";
        }
    }

    private void classGenerateFunctionDeclarations(Generator generator, PrintWriter output) {
        String className = escape(generator.getClassName());

        for (Map.Entry<String, Set<MethodDesc>> entry : generator.getMethods().entrySet()) {
            boolean overload = entry.getValue().size() > 1;
            for (MethodDesc desc : entry.getValue()) {
                String methodName = escape(entry.getKey());
                output.println("/*" + "\n" +
                        " * Class:     " + className + "\n" +
                        " * Method:    " + entry.getKey() + "\n" +
                        " * Signature: " + desc.descriptor + "\n" +
                        " */"
                );

                Type[] argTypes = Type.getArgumentTypes(desc.descriptor);
                Type retType = Type.getReturnType(desc.descriptor);

                output.print(
                        "JNIEXPORT " + typeToNative(retType) + " JNICALL Java_" + className + "_" + methodName
                );

                if (overload) {
                    output.print("__");
                    for (Type tpe : argTypes) {
                        output.print(escape(tpe.toString()));
                    }
                }
                output.println();

                output.print("  (JNIEnv *, ");
                if (desc.isStatic) {
                    output.print("jclass");
                } else {
                    output.print("jobject");
                }
                for (Type tpe : argTypes) {
                    output.print(", " + typeToNative(tpe));
                }
                output.println(");\n");
            }

        }
    }

    public void classGenerateFunctionDeclarations(ClassReader reader, PrintWriter output) {
        Generator generator = new Generator();
        reader.accept(generator, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        classGenerateFunctionDeclarations(generator, output);
    }

    public void classGenerateHeader(ClassReader reader, PrintWriter output) {
        output.println("/* DO NOT EDIT THIS FILE - it is machine generated */");
        output.println("#include <jni.h>");

        Generator generator = new Generator();
        reader.accept(generator, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        StringBuilder builder = new StringBuilder();
        String className = escape(generator.getClassName());

        output.println("/* Header for class " + className + " */");

        String includeHeader = "_Include_" + className;
        output.println("#ifndef " + includeHeader);
        output.println("#define " + includeHeader);

        output.println("#ifdef __cplusplus\n" +
                "extern \"C\" {\n" +
                "#endif");

        classGenerateFunctionDeclarations(generator, output);

        output.println("#ifdef __cplusplus\n" +
                "}\n" +
                "#endif\n" +
                "#endif\n");

    }

    public void classGenerateHeader(byte[] classFile, PrintWriter output) {
        classGenerateHeader(new ClassReader(classFile), output);
    }

    public void classGenerateHeader(byte[] classFileBuffer, int classFileOffset, int classFileLength, PrintWriter output) {
        classGenerateHeader(new ClassReader(classFileBuffer, classFileOffset, classFileLength), output);
    }

    public void classGenerateHeader(String className, PrintWriter output) throws IOException {
        classGenerateHeader(new ClassReader(className), output);
    }

    public void classGenerateHeader(InputStream input, PrintWriter output) throws IOException {
        classGenerateHeader(new ClassReader(input), output);
    }

    public Path[] getClassPaths() {
        return classPaths;
    }

    public void setClassPaths(Path[] classPaths) {
        Objects.requireNonNull(classPaths);
        this.classPaths = classPaths;
    }

    public boolean isUseRuntimeClassPath() {
        return useRuntimeClassPath;
    }

    public void setUseRuntimeClassPath(boolean useRuntimeClassPath) {
        this.useRuntimeClassPath = useRuntimeClassPath;
    }

    public static void main(String[] args) throws IOException {
    }
}

class MethodDesc {
    public final boolean isStatic;
    public final String descriptor;

    public MethodDesc(boolean isStatic, String descriptor) {
        this.isStatic = isStatic;
        this.descriptor = descriptor;
    }
}

class Generator extends ClassVisitor {
    private String className;

    private Map<String, Set<MethodDesc>> methods = new LinkedHashMap<String, Set<MethodDesc>>();

    public Generator() {
        super(Opcodes.ASM5);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if ((access & Opcodes.ACC_NATIVE) != 0) {
            if (methods.containsKey(name)) {
                methods.get(name).add(new MethodDesc((access & Opcodes.ACC_STATIC) != 0, descriptor));
            } else {
                LinkedHashSet<MethodDesc> set = new LinkedHashSet<MethodDesc>();
                set.add(new MethodDesc((access & Opcodes.ACC_STATIC) != 0, descriptor));
                methods.put(name, set);
            }
        }
        return null;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, Set<MethodDesc>> getMethods() {
        return methods;
    }
}