package org.glavo.javah;

class MethodDesc {
    final boolean isStatic;
    final String descriptor;

    MethodDesc(boolean isStatic, String descriptor) {
        this.isStatic = isStatic;
        this.descriptor = descriptor;
    }
}
