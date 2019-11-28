package org.glavo.javah;

import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.Manifest;

class Utils {
    static String encode(String str) {
        StringBuilder buffer = new StringBuilder(str.length());

        int len = str.length();
        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);
            if (ch == '_') {
                buffer.append("_1");
            } else if (ch == ';') {
                buffer.append("_2");
            } else if (ch == '[') {
                buffer.append("_3");
            } else if ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                buffer.append(ch);
            } else {
                buffer.append(String.format("_0%04x", (int) ch));
            }
        }
        return buffer.toString();
    }

    static String mapToNativeType(Type jt) {
        switch (jt.toString()) {
            case "Z":
                return "jboolean";
            case "B":
                return "jbyte";
            case "C":
                return "jchar";
            case "S":
                return "jshort";
            case "I":
                return "jint";
            case "J":
                return "jlong";
            case "F":
                return "jfloat";
            case "D":
                return "jdouble";
            case "V":
                return "void";
            case "Ljava/lang/Class;":
                return "jclass";
            case "Ljava/lang/String;":
                return "jstring";
            case "Ljava/lang/Throwable ;":
                return "jthrowable";
            case "[Z":
                return "jbooleanArray";
            case "[B":
                return "jbyteArray";
            case "[C":
                return "jcharArray";
            case "[S":
                return "jshortArray";
            case "[I":
                return "jintArray";
            case "[J":
                return "jlongArray";
            case "[F":
                return "jfloatArray";
            case "[D":
                return "jdoubleArray";
        }

        if (jt.toString().startsWith("[")) {
            return "jobjectArray";
        }

        return "jobject";
    }

    static String mangledArgSignature(Type methodType) {
        Objects.requireNonNull(methodType);
        String str = methodType.toString();
        int idx = str.indexOf(')');
        if (idx == -1) {
            throw new IllegalArgumentException(methodType.toString() + "is not a method type");
        }
        return encode(str.substring(1, idx));
    }

    static boolean isRegularClassName(String name) {
        Objects.requireNonNull(name);
        return !name.contains("//")
                && !name.contains("..")
                && name.indexOf('\\') == -1
                && name.indexOf(';') == -1
                && name.indexOf('[') == -1;
    }

    static String fullClassNameOf(String name) {
        Objects.requireNonNull(name);
        int idx = name.indexOf('/');
        if (idx == -1) {
            return name;
        }
        String cn = name.substring(idx + 1);
        if (cn.isEmpty() || cn.indexOf('/') == -1) {
            throw new IllegalArgumentException("Illegal class name: " + name);
        }
        return cn;
    }

    static String simpleNameOf(String name) {
        int idx = name.lastIndexOf('.');
        return name.substring(idx + 1);
    }

    static Path searchFromPathList(List<Path> paths, String className) {
        Objects.requireNonNull(paths);
        Objects.requireNonNull(className);
        String fp = fullClassNameOf(className).replace('.', '/') + ".class";
        for (Path path : paths) {
            Objects.requireNonNull(path);
            Path p = path.resolve(fp);
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return null;
    }

    static List<Path> getPathsFrom(Path root) {
        Objects.requireNonNull(root);
        root = root.toAbsolutePath();
        if (Files.isRegularFile(root)) {
            String name = root.toString().toLowerCase();
            try {
                FileSystem fs = FileSystems.newFileSystem(root, (ClassLoader) null);
                if (name.endsWith(".jar")) {
                    root = fs.getPath("/");
                } else if (name.endsWith(".jmod")) {
                    root = fs.getPath("/classes");
                } else {
                    return Collections.emptyList();
                }
            } catch (IOException e) {
                return Collections.emptyList();
            }
        }
        if (!Files.isDirectory(root)) {
            return Collections.emptyList();
        }

        boolean isMultiRelease = false;
        Path manifest = root.resolve("META-INF").resolve("MANIFEST.MF");
        if (Files.exists(manifest) && Files.isRegularFile(manifest)) {
            try (InputStream input = Files.newInputStream(manifest)) {
                isMultiRelease = Boolean.parseBoolean(
                        new Manifest(input).getMainAttributes().getOrDefault("Multi-Release", "false").toString()
                );

            } catch (Exception ignored) {
            }
        }

        if (!isMultiRelease) {
            return Collections.singletonList(root);
        }
        LinkedList<Path> list = new LinkedList<>();
        Path mr = root.resolve("META-INF").resolve("versions");
        try {
            Files.list(mr)
                    .filter(Utils::checkMultiReleaseVersion)
                    .sorted(Comparator.comparingInt(a -> Integer.parseInt(a.getFileName().toString())))
                    .forEachOrdered(list::addFirst);

        } catch (IOException ignored) {
        }

        list.add(root);
        return list;
    }

    private static final String[] MULTI_VERSIONS = {"9", "10", "11", "12", "13"};

    private static boolean checkMultiReleaseVersion(Path p) {
        Objects.requireNonNull(p);
        String n = p.getFileName().toString();
        for (String version : MULTI_VERSIONS) {
            if (version.equals(n)) {
                return true;
            }
        }
        return false;
    }

    static Path searchFrom(List<Path> searchPaths, String name) {
        Objects.requireNonNull(searchPaths);
        Objects.requireNonNull(name);

        name = simpleNameOf(name).replace('.', '/') + ".class";

        for (Path searchPath : searchPaths) {
            Path p = searchPath.resolve(name);
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return null;
    }
}
