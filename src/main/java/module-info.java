module org.glavo.javah {
    requires org.objectweb.asm;
    requires info.picocli;
    requires jdk.zipfs;

    exports org.glavo.javah;
    opens org.glavo.javah to info.picocli;
}