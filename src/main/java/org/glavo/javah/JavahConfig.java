package org.glavo.javah;

import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JavahConfig {
    private PrintWriter errorHandle = null;
    private boolean outputToSignalFile = false;
    private Path outputPath = null;

    private final List<SearchPath> searchPaths = new ArrayList<>();


    //
    // Getters and Setters
    //

    public void addSearchPath(SearchPath searchPath) {
        Objects.requireNonNull(searchPath);
        searchPaths.add(searchPath);
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

    public JavahConfig setOutputToSignalFile(boolean outputToSignalFile) {
        this.outputToSignalFile = outputToSignalFile;
        return this;
    }
}
