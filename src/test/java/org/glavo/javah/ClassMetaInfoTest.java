package org.glavo.javah;

import org.glavo.javah.util.ClassMetaInfo;
import org.glavo.javah.util.NativeMethod;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ClassMetaInfoTest {
    static class TestData {
        ClassName name;
        ClassName superName;

        TestData(ClassName name, ClassName superName) {
            this.name = name;
            this.superName = superName;
        }
    }

    class C1 {
        public native int f();
    }

    @Test
    void test() throws IOException, ClassNotFoundException {
        TestData[] data = new TestData[]{
                new TestData(ClassName.ofFullName("java.lang.Object"), null),
                new TestData(ClassName.ofFullName("java.lang.String"), ClassName.ofFullName("java.lang.Object")),
                new TestData(ClassName.ofFullName(C1.class.getName()), ClassName.ofFullName(C1.class.getSuperclass().getName()))
        };
        for (TestData d : data) {
            Class<?> cls = Class.forName(d.name.className());
            List<NativeMethod> methods = Arrays.stream(cls.getDeclaredMethods())
                    .filter(m -> (m.getModifiers() & Modifier.NATIVE) != 0)
                    .map(m -> NativeMethod.of(m.getModifiers(), m.getName(), Type.getMethodDescriptor(m)))
                    .collect(Collectors.toList());

            ClassMetaInfo info = new ClassMetaInfo();
            ClassReader reader = new ClassReader(d.name.className());
            reader.accept(info, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

            assertEquals(info.name, d.name);
            assertEquals(info.superClassName, d.superName);

            assertTrue(info.methods.containsAll(methods) && methods.containsAll(info.methods));
        }
    }
}
