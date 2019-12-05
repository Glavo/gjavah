# gjavah [![](https://jitpack.io/v/org.glavo.glavo/gjavah.svg)](https://jitpack.io/#org.glavo.glavo/gjavah)

Gradle :
```
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compile 'org.glavo.glavo:gjavah:0.2.0'
}
```

## Usage

```java
import org.glavo.javah.*;

var task = new JavahTask();
task.setOutputDir(Paths.get(""));
task.addRuntimeSearchPath();
task.addClass("java.lang.Object");
task.addClass("java.lang.String");
task.run();
```