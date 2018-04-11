# gjavah [![](https://jitpack.io/v/org.glavo.glavo/gjavah.svg)](https://jitpack.io/#org.glavo.glavo/gjavah)

Gradle :
```
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compile 'org.glavo.glavo:gjavah:0.1.0'
}
```

## Usage

```java
import org.glavo.javah.*;

var writer = new PrintWriter(System.out);
HeaderGenerator.generateHeader("java.lang.Object", writer);
writer.flush();

```