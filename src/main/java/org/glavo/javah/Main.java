package org.glavo.javah;

import org.glavo.javah.resource.Resource;
import org.glavo.javah.resource.Version;
import org.glavo.javah.util.ClassName;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.stream.Stream;

public final class Main {

    public static void main(String[] args) throws Throwable {
        JavahTask task = new JavahTask();

        Iterator<String> it = Arrays.asList(args).iterator();
        if (!it.hasNext()) {
            System.out.println(Resource.getText("javah.help"));
            System.exit(-1);
        }

        boolean hasPath = false;

        while (it.hasNext()) {
            String c = it.next();
            switch (c) {
                case "--locale":
                    if (!it.hasNext()) {
                        System.err.println(Resource.getText("javah.error.missArg", c));
                        System.exit(-1);
                    }
                    Resource.reload(Locale.forLanguageTag(it.next()));
                    break;
                case "-version":
                case "--version":
                    System.out.println(Resource.getText("javah.version", Version.VERSION));
                    return;
                case "-h":
                case "-help":
                case "--help":
                case "-?":
                    System.out.println(Resource.getText("javah.help"));
                    return;
                case "-p":
                case "--module-path": {
                    if (!it.hasNext()) {
                        System.err.println(Resource.getText("javah.error.missArg", c));
                        System.exit(-1);
                    }
                    String modulePath = it.next();
                    hasPath = true;
                    for (String s : modulePath.split(File.pathSeparator)) {
                        Path path = Paths.get(s);
                        if (Files.isDirectory(path)) {
                            task.addModulePath(path);
                        }
                    }
                    break;
                }
                case "-cp":
                case "-classpath":
                case "--classpath":
                case "--class-path": {
                    if (!it.hasNext()) {
                        System.err.println(Resource.getText("javah.error.missArg", c));
                        System.exit(-1);
                    }
                    hasPath = true;
                    Arrays.stream(it.next().split(File.pathSeparator))
                            .flatMap(p -> {
                                if (p.endsWith("/*") || p.equals("*")) {
                                    try {
                                        return Files.list(Paths.get(p.substring(0, p.length() - 1)))
                                                .filter(Files::isRegularFile)
                                                .filter(t -> t.toAbsolutePath().getFileName().toString().toLowerCase().endsWith(".jar"));
                                    } catch (Exception e) {
                                        return Stream.empty();
                                    }
                                }
                                return Stream.of(Paths.get(p));
                            })
                            .filter(Files::exists)
                            .map(Path::toAbsolutePath)
                            .forEachOrdered(task::addClassPath);
                    break;
                }
                case "-d": {
                    if (!it.hasNext()) {
                        System.err.println(Resource.getText("javah.error.missArg", c));
                        System.exit(-1);
                    }
                    String outputDir = it.next();
                    task.setOutputFile(null);
                    task.setOutputDir(Paths.get(outputDir));
                    break;
                }
                case "-o": {
                    if (!it.hasNext()) {
                        System.err.println(Resource.getText("javah.error.missArg", c));
                        System.exit(-1);
                    }
                    String outputFile = it.next();
                    task.setOutputDir(null);
                    task.setOutputFile(Paths.get(outputFile));
                    break;
                }
                default:
                    task.addClass(ClassName.of(c));
                    break;
            }
        }
        if (!task.hasClasses()) {
            System.err.println(Resource.getText("javah.error.noClasses"));
            System.exit(-1);
        }
        if (!hasPath) {
            String path = System.getenv("CLASSPATH");
            task.addClassPath(Paths.get(path == null ? System.getProperty("user.dir") : path).toAbsolutePath());
        }
        task.addRuntimeSearchPath();
        if (task.getOutputFile() == null && task.getOutputDir() == null) {
            task.setOutputDir(Paths.get(System.getProperty("user.dir")));
        }
        task.setErrorHandle(new PrintWriter(System.err, true));
        task.run();
    }
}
