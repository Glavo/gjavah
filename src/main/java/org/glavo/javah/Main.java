package org.glavo.javah;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.jar.Manifest;

public class Main {
    private static final String HELP_MESSAGE =
            "usage:\n" +
                    "  gjavah [options] <classes>\n" +
                    "where [options] include:\n" +
                    "    -o <file>                       Output file (only one of -d or -o may be used)\n" +
                    "    -d <dir>                        Output directory\n" +
                    "    --module-path <path>            Path from which to search  modules\n" +
                    "    --class-path <path> | " +
                    "-classpath <path> | -cp <path>\n" +
                    "                                    " +
                    "                                Path from which to search classes\n" +
                    "    --version                       Print version information\n" +
                    "    -h  --help  -?                  Print this message";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println(HELP_MESSAGE);
            System.exit(-1);
        }
        JavahTask task = new JavahTask();
        task.addRuntimeClasspath();
        loop:
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--version":
                case "-version":
                    try (InputStream in = Main.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
                        Manifest manifest = new Manifest(in);
                        System.out.println("gjavah version: " + manifest.getMainAttributes().getValue("GJavah-Version"));
                    }
                    return;
                case "-h":
                case "--help":
                case "-help":
                case "-?":
                    System.out.println(HELP_MESSAGE);
                    return;
                case "-o":
                    task.setOutputToSignalFile(true);
                    task.setOutputPath(Paths.get(args[++i]));
                    break;
                case "-d":
                    task.setOutputToSignalFile(false);
                    task.setOutputPath(Paths.get(args[++i]));
                    break;
                case "--module-path":
                    task.addModulePaths(args[++i]);
                    break;
                case "--class-path":
                case "--classpath":
                case "-classpath":
                case "-cp":
                    task.addClasspaths(args[++i]);
                    break;
                default:
                    for (int j = i; j < args.length; j++) {
                        task.addClass(args[i]);
                    }
                    break loop;
            }
            if (task.getClassList().isEmpty()) {
                System.err.println("error: no classes specified");
            }
            task.run();
        }
    }
}
