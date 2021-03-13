package org.glavo.javah;

import org.glavo.javah.util.NativeMethod;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class NativeMethodTests {
    @Test
    void testFactoryMethod() {
        Map<String, Type> qualified = new LinkedHashMap<>() {
            {
                put("method0", Type.getType("()I"));
                put("method1", Type.getType("(Ljava/lang/String;)I"));
            }
        };

        Map<String, Type> wrongs = new LinkedHashMap<>() {
            {
                put("method0", Type.getType(String.class));
                put("method1", Type.getType("()"));
                put("method2", Type.getType("L;"));
            }
        };

        qualified.forEach((name, type) -> assertDoesNotThrow(() -> NativeMethod.of(name, type)));
        wrongs.forEach((name, type) -> assertThrows(IllegalArgumentException.class, () -> NativeMethod.of(name, type)));


    }
}
