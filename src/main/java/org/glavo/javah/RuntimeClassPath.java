package org.glavo.javah;

import java.net.URI;
import java.net.URL;
import java.nio.file.*;

public class RuntimeClassPath implements SearchPath {
    public static final RuntimeClassPath INSTANCE = new RuntimeClassPath();

    private RuntimeClassPath() {
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public Path searchClass(String className) {
        className = Utils.fullClassNameOf(className);
        URI uri = null;
        try {
            Class<?> cls = Class.forName(className);
            URL url = cls.getResource(Utils.simpleNameOf(className) + ".class");
            if (url == null) {
                return null;
            }
            uri = url.toURI();
            return Paths.get(uri);
        } catch (FileSystemNotFoundException e) {
            try {
                FileSystem fs = FileSystems.newFileSystem(uri, null);
                Path p = fs.getPath(className.replace('.', '/') + ".class");
                if (Files.isRegularFile(p)) {
                    return p;
                }
                return null;
            } catch (Exception ex) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
