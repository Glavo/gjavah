package org.glavo.javah;

import picocli.CommandLine;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import static picocli.CommandLine.*;

@Command(name = "gjavah", version = "gjavah %1", sortOptions = false)
public class Main {
    @Option(names = {"-p", "--module-path"}, description = "Path from which to search  modules")
    private String modulePath;
    @Option(names = {"-cp", "-classpath", "--classpath", "--class-path"}, description = "Path from which to search classes")
    private String classpath;

    @Option(names = {"-version", "--version"}, description = "Print version information")
    private boolean showVersion;

    @Option(names = {"-h", "--help", "-?"}, usageHelp = true, description = "Print this message")
    private boolean showHelp;

    @Option(names = {"-d"}, description = "Output directory")
    private Path outputDir = Paths.get("").toAbsolutePath().normalize();

    @Parameters(paramLabel = "classes")
    private List<String> classes;

    public static void main(String[] args) throws Exception {
        Main m = new Main();
        CommandLine cm = new CommandLine(m);
        if (args == null || args.length == 0) {
            cm.usage(System.err);
            System.exit(-1);
        }

        cm.parseArgs(args);
        if (m.showHelp) {
            cm.usage(System.out);
            return;
        }
        if (m.showVersion) {
            try (InputStream in = Main.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
                cm.printVersionHelp(System.out, Help.Ansi.AUTO, new Manifest(in).getMainAttributes().getValue("GJavah-Version"));
            }
            return;
        }
        if (m.classes == null || m.classes.isEmpty()) {
            cm.usage(System.err);
            System.exit(-1);
        }

        JavahTask task = new JavahTask();
        if (m.modulePath != null) {
            Arrays.stream(m.modulePath.split(File.pathSeparator))
                    .map(Paths::get)
                    .filter(Files::isDirectory)
                    .forEachOrdered(task::addModulePath);
        }
        if (m.classpath == null) {
            m.classpath = System.getenv("CLASSPATH");
        }
        if (m.classpath == null) {
            m.classpath = Paths.get("").toAbsolutePath().normalize().toString();
        }
        Arrays.stream(m.classpath.split(File.pathSeparator))
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
        task.setOutputDir(m.outputDir);
        task.addClasses(m.classes);
        task.setErrorHandle(new PrintWriter(System.err, true));
        task.addRuntimeSearchPath();
        task.run();
    }

    @Override
    public String toString() {
        return String.format("Main[modulePath='%s', classpath='%s', showVersion=%s, showHelp=%s, outputDir=%s, classes=%s]", modulePath, classpath, showVersion, showHelp, outputDir, classes);
    }
}
