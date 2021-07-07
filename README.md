# gjavah 
[![](https://jitpack.io/v/Glavo/gjavah.svg)](https://jitpack.io/#Glavo/gjavah)

Gradle :

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'org.glavo.glavo:gjavah:0.3.1'
}
```

## Usage

### Use gjavah in Java

```java
import org.glavo.javah.*;

JavahTask task = new JavahTask();
task.setOutputDir(Paths.get(""));
task.addRuntimeSearchPath();
task.addClass("java.lang.Object");
task.addClass("java.lang.String");
task.run();
```

### Use gjavah in console

```
java -jar gjavah-0.3.1.jar -d . java.lang.Object java.lang.System
```
