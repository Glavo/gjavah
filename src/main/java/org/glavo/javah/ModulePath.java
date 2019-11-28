package org.glavo.javah;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ModulePath implements SearchPath {
    private final Path path;
    private final List<Path> paths;

    public ModulePath(Path path) {
        Objects.requireNonNull(path);
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(path + "is not a dir");
        }

        this.path = path;
        try {
            paths = Files.list(path)
                    .filter(Files::isRegularFile)
                    .flatMap(p -> Utils.getPathsFrom(p).stream())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Path searchClass(String className) {
        return Utils.searchFrom(paths, className);
    }
}
