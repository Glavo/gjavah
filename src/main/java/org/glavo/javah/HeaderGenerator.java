package org.glavo.javah;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

public final class HeaderGenerator {
    private static final Path[] EMPTY_PATH_ARRAY = new Path[0];
    private static final List<String> THROWABLE_NAME_LIST = Arrays.asList("Ljava/lang/Throwable;", "Ljava/lang/Error;", "Ljava/lang/Exception");

    private Path[] classPaths;
    private boolean useRuntimeClassPath;
    private Map<String, ClassReader> readerCache = new HashMap<>();

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

    public void generateSingleFile(Path outputFile, Collection<String> classNames)
            throws IOException {
        Files.createDirectories(outputFile.getParent());
        boolean first = true;
        try (StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(new BufferedWriter(stringWriter))) {
            for (String name : classNames) {
                NativeMethodVisitor g = getRequiredNativeMethodVisitor(name);
                if (g.isEmpty()) {
                    continue;
                }
                if (first) {
                    classGenerateHeader(g, writer);
                    first = false;
                } else {
                    classGenerateHeaderWithoutInclude(g, writer);
                }
            }
            writer.flush();
            writeIfChanged(stringWriter.toString(), outputFile);
        }
    }

    public void generateMultipleFiles(Path outputDir, Collection<String> classNames)
            throws IOException {
        Files.createDirectories(outputDir);
        for (String name : classNames) {
            NativeMethodVisitor g = getRequiredNativeMethodVisitor(name);
            if (g.isEmpty()) {
                continue;
            }
            Path headerFile = outputDir.resolve(g.getClassName()
                .replace('.', '_')
                .replace('/', '_')
                .replace('$', '_') + ".h");
            try (StringWriter stringWriter = new StringWriter();
                 PrintWriter writer = new PrintWriter(new BufferedWriter(stringWriter))) {
                classGenerateHeader(g, writer);
                writer.flush();
                writeIfChanged(stringWriter.toString(), headerFile);
            }
        }
    }

    private void writeIfChanged(String data, Path outputFile)
            throws IOException {
        boolean write = true;
        if (outputFile.toFile().exists()) {
            String fileData = new String(Files.readAllBytes(outputFile));
            write = !fileData.equals(data);
        }
        if (write) {
            Files.write(outputFile, data.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    public void generateHeader(String className, PrintWriter output) throws IOException {
        classGenerateHeader(className, output);
    }

    public void generateFunctionDeclarations(ClassReader reader, PrintWriter output) {
        classGenerateFunctionDeclarations(reader, output);
    }

    public void generateHeader(ClassReader reader, PrintWriter output) {
        classGenerateHeader(reader, output);
    }

    public void generateHeader(byte[] classFile, PrintWriter output) {
        classGenerateHeader(classFile, output);
    }

    public void generateHeader(byte[] classFileBuffer, int classFileOffset, int classFileLength, PrintWriter output) {
        classGenerateHeader(classFileBuffer, classFileOffset, classFileLength, output);
    }

    public void generateHeader(InputStream input, PrintWriter output) throws IOException {
        classGenerateHeader(input, output);
    }


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
                    builder.append('_');
                    break;
                default:
                    if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
                        builder.append(ch);
                    } else {
                        builder.append("_0").append(String.format("%04x", (int) ch));
                    }
            }
        }
        return builder.toString();
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
                className = findClass(className).getSuperName();
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

    private ClassReader findClass(String name)
        throws IOException {
        if (readerCache.containsKey(name)) {
            return readerCache.get(name);
        }
        ClassReader ret;
        Path directPath = Paths.get(name);
        if (Files.exists(directPath)) {
            ret = findClassByFile(directPath);
        } else {
            ret = findClassByName(name);
        }
        readerCache.put(name, ret);
        return ret;
    }

    private ClassReader findClassByFile(Path path)
        throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return new ClassReader(is);
        }
    }

    private ClassReader findClassByName(String name)
        throws IOException {

        loop:
        for (Path path : classPaths) {
            path = path.toAbsolutePath();
            if (!Files.exists(path)) {
                continue;
            }
            Path filePath = path.resolve(name);
            if (Files.exists(filePath)) {
                return findClassByFile(filePath);
            }

            String[] ps = name.split("/|\\.");
            if (ps.length == 0) {
                continue;
            }
            ps[ps.length - 1] += ".class";
            for (String p : ps) {
                path = path.resolve(p);
                if (!Files.exists(path)) {
                    continue loop;
                }
            }
            return findClassByFile(path);
        }

        return new ClassReader(name.replace('/', '.'));
    }

    private void classGenerateFunctionDeclarations(NativeMethodVisitor visitor, PrintWriter output) {
        String className = escape(visitor.getClassName());

        for (Map.Entry<String, Object> entry: visitor.getConstants().entrySet()) {
            String constant = className + '_' + entry.getKey();
            String value = entry.getValue().toString();
            if(entry.getValue() instanceof Integer)
                value += "L";
            else if(entry.getValue() instanceof Long)
                value += "LL";
            else if(entry.getValue() instanceof Float)
                value += "f";
            output.println("#undef " + constant);
            output.println("#define " + constant + ' ' + value);
        }

        for (Map.Entry<String, Set<MethodDesc>> entry : visitor.getMethods().entrySet()) {
            boolean overload = entry.getValue().size() > 1;
            for (MethodDesc desc : entry.getValue()) {
                String methodName = escape(entry.getKey());
                String readableClassName = visitor.getClassName()
                        .replace('.', '_')
                        .replace('/', '_')
                        .replace('$', '_');
                output.println("/*" + "\n" +
                        " * Class:     " + readableClassName + "\n" +
                        " * Method:    " + entry.getKey() + "\n" +
                        " * Signature: " + desc.descriptor.replace('$', '/') + "\n" +
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

    private void classGenerateFunctionDeclarations(ClassReader reader, PrintWriter output) {
        NativeMethodVisitor visitor = new NativeMethodVisitor();
        reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        classGenerateFunctionDeclarations(visitor, output);
    }

    private void classGenerateHeaderWithoutInclude(NativeMethodVisitor visitor, PrintWriter output) {
        String className = escape(visitor.getClassName()).replace("_00024", "_");

        output.println("/* Header for class " + className + " */");
        output.println();

        String includeHeader = "_Included_" + className;
        output.println("#ifndef " + includeHeader);
        output.println("#define " + includeHeader);

        output.println("#ifdef __cplusplus\n" +
                "extern \"C\" {\n" +
                "#endif");

        classGenerateFunctionDeclarations(visitor, output);

        output.println("#ifdef __cplusplus\n" +
                "}\n" +
                "#endif\n" +
                "#endif");
    }

    private void classGenerateHeaderWithoutInclude(ClassReader reader, PrintWriter output) {
        NativeMethodVisitor visitor = new NativeMethodVisitor();
        reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        classGenerateHeaderWithoutInclude(visitor, output);
    }

    private void classGenerateHeader(NativeMethodVisitor visitor, PrintWriter output) {
        output.println("/* DO NOT EDIT THIS FILE - it is machine generated */");
        output.println("#include <jni.h>");

        classGenerateHeaderWithoutInclude(visitor, output);
    }

    private void classGenerateHeader(ClassReader reader, PrintWriter output) {
        NativeMethodVisitor visitor = new NativeMethodVisitor();
        reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        classGenerateHeader(visitor, output);
    }

    private void classGenerateHeader(byte[] classFile, PrintWriter output) {
        classGenerateHeader(new ClassReader(classFile), output);
    }

    private void classGenerateHeader(byte[] classFileBuffer, int classFileOffset, int classFileLength, PrintWriter output) {
        classGenerateHeader(new ClassReader(classFileBuffer, classFileOffset, classFileLength), output);
    }

    private void classGenerateHeader(String className, PrintWriter output) throws IOException {
        classGenerateHeader(findClass(className), output);
    }

    private void classGenerateHeader(InputStream input, PrintWriter output) throws IOException {
        classGenerateHeader(new ClassReader(input), output);
    }

    private NativeMethodVisitor getRequiredNativeMethodVisitor(String className) {
        ClassReader reader;
        try {
            reader = findClass(className);
        } catch(IOException ioe) {
            System.err.println("Error: Could not find class file for '" + className + "'.");
            System.err.println("Classpath: ");
            for (Path p : classPaths) {
                System.err.println(" - " + p.toString());
            }
            System.exit(187);
            return null;
        }
        NativeMethodVisitor ret = new NativeMethodVisitor();
        reader.accept(ret, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return ret;
    }

}

