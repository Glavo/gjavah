package org.glavo.javah;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class NativeMethodVisitor extends ClassVisitor {
    private String className;

    private Map<String, Object> constants = new LinkedHashMap<>();
    private Map<String, Set<MethodDesc>> methods = new LinkedHashMap<>();

    NativeMethodVisitor() {
        super(Opcodes.ASM5);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if (value instanceof Number) {
            constants.put(name, value);
        }
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if ((access & Opcodes.ACC_NATIVE) != 0) {
            if (methods.containsKey(name)) {
                methods.get(name).add(new MethodDesc((access & Opcodes.ACC_STATIC) != 0, descriptor));
            } else {
                LinkedHashSet<MethodDesc> set = new LinkedHashSet<>();
                set.add(new MethodDesc((access & Opcodes.ACC_STATIC) != 0, descriptor));
                methods.put(name, set);
            }
        }
        return null;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, Set<MethodDesc>> getMethods() {
        return methods;
    }

    public Map<String, Object> getConstants() {
        return constants;
    }

    public boolean hasConstants() {
        return constants.size() > 0;
    }

    public boolean hasNativeMethods() {
        return methods.size() > 0;
    }

    public boolean isEmpty() {
        return !hasConstants() && !hasNativeMethods();
    }
}
