package org.glavo.javah;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class RuntimeSearchPathTests {

    @Test
    void test() throws Exception {
        Class<?>[] testClasses = {
                String.class,
                Test.class,
                RuntimeSearchPathTests.class,
                Main.class
        };

        for (Class<?> cls : testClasses) {
            try (InputStream in = cls.getResourceAsStream(cls.getSimpleName() + ".class")) {
                assertArrayEquals(
                        Files.readAllBytes(RuntimeSearchPath.searchClass(cls.getName())),
                        in.readAllBytes(),
                        "Search " + cls + " failed"
                );
            }
        }
    }
}
