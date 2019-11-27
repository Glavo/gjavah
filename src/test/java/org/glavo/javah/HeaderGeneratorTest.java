package org.glavo.javah;

import java.io.IOException;
import java.io.PrintWriter;

public class HeaderGeneratorTest {
    public static void main(String[] args) throws IOException {
        PrintWriter writer = new PrintWriter(System.out);
        HeaderGenerator generator = new HeaderGenerator();
        generator.generateHeader("java.lang.System", writer);
        writer.flush();
    }
}
