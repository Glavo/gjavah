package org.glavo.javah;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

class Utils {
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
                FileSystem fs = FileSystems.newFileSystem(root, null);
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
