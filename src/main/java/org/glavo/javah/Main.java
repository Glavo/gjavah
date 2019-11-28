package org.glavo.javah;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {
	public static final String VERSION = "0.1.2";

	private static final String message = "" +
		  "Usage: \n" +
		  "  javah [options] <classes>\n" +
		  "where [options] include:\n" +
		  "  -o <file>                Output file (only one of -d or -o may be used)\n" +
		  "  -d <dir>                 Output directory\n" +
		  "  -h  -help --help  -?     Print this message\n" +
		  "  -version                 Print version information\n" +
		  "  -classpath <path>        Path from which to load classes\n" +
		  "  -cp <path>               Path from which to load classes\n" +
		  "<classes> are specified with their fully qualified names\n" +
		  "(for example, java.lang.Object).";

	public static void main(String[] args) throws IOException {
		List<Path> cps = new ArrayList<>();
		List<String> classNames = new ArrayList<>();
		Path outputFile = null;
		Path outputDir = null;

		if (args.length == 0) {
			System.out.println(message);
			return;
		}
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith("-")) {
				switch (arg) {
					case "-h":
					case "-help":
					case "--help":
					case "-?":
						System.out.println(message);
						return;
					case "-version":
						System.out.println(VERSION);
						return;
					case "-cp":
					case "-classpath":
						i++;
						if (i == args.length) {
							System.err.println("javah: " + arg + " requires an argument.");
							return;
						}
						String[] s = args[i].split(File.pathSeparator);
						for (String ss : s) {
							Path p = Paths.get(ss);
							if (Files.exists(p)) {
								if (Files.isDirectory(p)) {
									cps.add(p);
								} else if (ss.toLowerCase().endsWith(".jar") || ss.toLowerCase().endsWith(".zip")) {
									try {
										cps.add(FileSystems.newFileSystem(p, null).getPath("/"));
									} catch (Exception ignored) {
									}
								}
							} else {
								System.err.println("WARNING: Classpath not found: " + p);
							}
						}
						break;
					case "-o":
						i++;
						if (i == args.length) {
							System.err.println("javah: " + arg + " requires an argument.");
							return;
						}
						if (outputDir != null) {
							System.err.println("Error: Can't mix options -d and -o.  Try -help.");
							return;
						}
						outputFile = Paths.get(args[i]);
						break;

					case "-d":
						i++;
						if (i == args.length) {
							System.err.println("javah: " + arg + " requires an argument.");
							return;
						}
						if (outputFile != null) {
							System.err.println("Error: Can't mix options -d and -o.  Try -help.");
							return;
						}
						outputDir = Paths.get(args[i]);

						break;
					default:
						System.err.println("Error: unknown option: " + arg);
						return;
				}

			} else {
				classNames.add(arg);
			}

			if (outputDir == null && outputFile == null) {
				outputDir = Paths.get(System.getProperty("user.dir", "."));
			}
			if (cps.isEmpty()) {
				cps.add(Paths.get(System.getProperty("user.dir", ".")));
			}

			if (outputDir != null && Files.notExists(outputDir)) {
				Files.createDirectories(outputDir);
			}

			if (outputFile != null && Files.notExists(outputFile)) {
				Files.createDirectories(outputFile.getParent());
			}

			HeaderGenerator generator = new HeaderGenerator(cps.toArray(new Path[0]));
			if (outputFile != null) {
				generator.generateSingleFile(outputFile, classNames);
			}
			if (outputDir != null) {
				generator.generateMultipleFiles(outputDir, classNames);
			}

		}
	}
}
