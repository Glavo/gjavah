# gjavah [![](https://jitpack.io/v/org.glavo.glavo/gjavah.svg)](https://jitpack.io/#org.glavo.glavo/gjavah)

Gradle :
```
// Temporarily unavailable, please wait for the new version

/*
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compile 'org.glavo.glavo:gjavah:0.3.0' 
}
8?
```

## Usage

```java
import org.glavo.javah.*;

JavahTask task = new JavahTask();
task.setOutputDir(Paths.get(""));
task.addRuntimeSearchPath();
task.addClass("java.lang.Object");
task.addClass("java.lang.String");
task.run();
```