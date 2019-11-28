package org.glavo.javah;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class ClassPath implements SearchPath {
    private final Path path;
    private final List<Path> searchPaths;

    public ClassPath(Path path) {
        Objects.requireNonNull(path);
        this.path = path;
        searchPaths = Utils.getPathsFrom(path);
    }

    @Override
    public Path searchClass(String className) {
        Objects.requireNonNull(className);
        return Utils.searchFrom(searchPaths, className);
    }

    @Override
    public String toString() {
        return "ClassPath[" + path + "]";
    }
}
