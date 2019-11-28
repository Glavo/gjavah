package org.glavo.javah;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class JavahTask {
    private PrintWriter errorHandle = new PrintWriter(System.err);
    private boolean outputToSignalFile = false;
    private Path outputPath = null;

    private final List<SearchPath> searchPaths = new ArrayList<>();
    private final List<String> classList = new ArrayList<>();

    public void run() throws IOException {
        if (outputPath == null) {
            outputPath = Paths.get(".").toAbsolutePath();
            outputToSignalFile = false;
        }

        if (outputToSignalFile) {
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath))) {
                writer.write(JNIGenerator.FILE_HEADER);
                classList.stream()
                        .map(this::search)
                        .filter(Objects::nonNull)
                        .filter(Files::isReadable)
                        .map(p -> new JNIGenerator(writer, p))
                        .forEachOrdered(JNIGenerator::generate);

                writer.write(JNIGenerator.FILE_END);
            }
        } else {
            for (String c : classList) {
                Path p = search(c);
                if (p == null) {
                    continue;
                }
                try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(
                        outputPath.resolve(Utils.encode(c).replace('.', '_') + ".h")))) {
                    output.write(JNIGenerator.FILE_HEADER);
                    new JNIGenerator(output, p).generate();
                    output.write(JNIGenerator.FILE_END);
                }
            }
        }
    }

    private Path search(String className) {
        for (SearchPath searchPath : searchPaths) {
            Path c = searchPath.searchClass(className);
            if (c != null) {
                return c;
            }
        }
        errorHandle.println("class" + className + " not found");
        return null;
    }

    //
    // Getters and Setters
    //

    public void addSearchPath(SearchPath searchPath) {
        Objects.requireNonNull(searchPath);
        searchPaths.add(searchPath);
    }

    public void addModulePath(Path modulePath) {
        Objects.requireNonNull(modulePath);
        searchPaths.add(new ModulePath(modulePath));
    }

    public void addModulePaths(String modulePaths) {
        Objects.requireNonNull(modulePaths);
        Arrays.stream(modulePaths.split(File.pathSeparator))
                .filter(s -> !"".equals(s))
                .map(Paths::get)
                .filter(Files::isDirectory)
                .forEachOrdered(this::addModulePath);
    }

    public void addClasspath(Path classPath) {
        Objects.requireNonNull(classPath);
        searchPaths.add(new ClassPath(classPath));
    }

    public void addClasspaths(String cps) {
        Arrays.stream(cps.split(File.pathSeparator))
                .filter(s -> !"".equals(s))
                .flatMap(s -> {
                    if (s.endsWith(File.separatorChar + "*")) {
                        try {
                            return Files.list(Paths.get(s.substring(0, s.length() - 2)))
                                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".jar"));
                        } catch (IOException e) {
                            return Stream.empty();
                        }
                    }
                    return Stream.of(Paths.get(s));
                })
                .filter(Files::exists)
                .map(ClassPath::new)
                .forEachOrdered(searchPaths::add);
    }

    public void addRuntimeClasspath() {
        searchPaths.add(RuntimeClassPath.INSTANCE);
    }

    public List<SearchPath> searchPaths() {
        return searchPaths;
    }

    public PrintWriter getErrorHandle() {
        return errorHandle;
    }

    public void setErrorHandle(Writer handle) {
        if (handle == null || handle instanceof PrintWriter) {
            errorHandle = (PrintWriter) handle;
        } else {
            errorHandle = new PrintWriter(handle);
        }
    }

    public boolean isOutputToSignalFile() {
        return outputToSignalFile;
    }

    public void setOutputToSignalFile(boolean outputToSignalFile) {
        this.outputToSignalFile = outputToSignalFile;
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(Path outputPath) {
        this.outputPath = outputPath;
    }

    public void addClass(String name) {
        classList.add(name);
    }

    public List<String> getClassList() {
        return classList;
    }
}
