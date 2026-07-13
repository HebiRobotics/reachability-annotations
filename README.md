# Reachability Annotations

This library provides zero-dependency annotations for generating [GraalVM Native Image](https://www.graalvm.org/native-image/) [Reachability Metadata](https://www.graalvm.org/latest/reference-manual/native-image/metadata/) at compile time. They are compatible with standard Java code and are independent of any framework like Quarkus or Micronaut.

The annotations also make extensive use of `typeReached` conditions. By default, the settings are only applied if the annotated type is reachable in the native image. This mitigates binary size explosion for features that aren't used. The condition can be changed by setting a custom `condition`, or disabled by setting `condition = Object.class`.

## @Reachable

Declares classes, resources, dynamic proxies, and bundles that should be available in the native image. Below are some examples to get started. Check the JavaDoc for more detailed documentation.

```Java
// Enables full reflection if the annotated class is reached from any point
@Reachable
public enum ReflectivelyAccessedEnum {
}

// Full reflection of all specified classes and their parent hierarchy
@Reachable(
        includeClassHierarchy = true,
        classes = {InetAddress.class, InetSocketAddress.class},
        classNames = {"some.internal.class$Nested", "other.internal.class"}
)
public class ReferencingOtherClasses {
}

// Resource globs relative to the annotated class
@Reachable(condition = ImageLoader.class, resources = {
        "images/*.png",
        "images/*.jpg",
})
public class RelativeResourceGlobs {
}

// Resource globs relative to the class path root (start with '/')
@Reachable(conditionName = "custom.condition.ImageLoader", resources = {
        "/assets/*.png",
        "/assets/*.jpg",
})
public class AbsoluteResourceGlobs {
}

// Unconditional custom access of a hidden class w/ JNI
@Reachable(
        condition = Object.class,
        classNames = "sun.misc.Unsafe", 
        memberAccess = { MemberAccess.ALL_DECLARED_FIELDS},
        jniAccessible = true
)
public static class FineGrainedAccess {
}
```

## @ReachableFxView

This is a special annotation for working with JavaFX FXML - a markup language for GUI layouts that makes extensive use of reflection.

Matching FXML and CSS files on the output classpath are automatically parsed, and add appropriate reachability configuration for included files, `fx:controller` classes as well as imported types.

The default naming convention follows established conventions (see [FxmlKit](https://github.com/dlsc-software-consulting-gmbh/FxmlKit) or [Afterburner.fx](https://github.com/adambien/afterburner.fx)) to determine the resource names based on the lowercased view name. For example,
 - `${Name}View.java` -> `DialogView.java`
 - `${name}.fxml` -> `dialog.fxml`
 - `${name}.css` -> `dialog.css`
 - `${name}*.properties` -> `dialog_en.properties`, `dialog_de.properties`, ...

```Java
@ReachableFxView("dialog") // checks for dialog.fxml, dialog.css, dialog.properties etc.
public enum DialogView extends com.airhacks.afterburner.views.FXMLView {
}
```


## @ReachableFxResources

Another JavaFX related annotation that provides more resource control and can parse multiple FXML/CSS files based on globs.

```Java
// Enables full reflection of all found classes and their parent hierarchy
@ReachableFxResources({
        "/assets/images/*.png", // relative to classpath root
        "views/**.fxml", // relative to annotated type
        "views/**.css", // relative to annotated type
})
public class MyApp extends Application {
}
```

## @Inject, @PostConstruct, @PreDestroy, @FXML

The processor also looks at annotations commonly used for dependency injection and adds the corresponding fields, methods, or constructors. Both `javafx.*` and `jakarta.*` namespaces are supported.

```Java
// Add reflection access to the field
public class SomeClass {
    @Inject
    SomeOtherClass field;
}
```

## Generated Metadata

The metadata gets generated into the `META-INF/native-image/reachable-generated/${project}/<annotation>/` directory. The `${project}` name should be unique and needs to be set via a compiler argument.

For example, in Maven it should be set to `-Aproject=${project.groupId}/${project.artifactId}`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <compilerArgs>
            <arg>-Aproject=${project.groupId}/${project.artifactId}</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

Note that the output format is currently the legacy `1.0.0` metadata schema (separate files) to be compatible with older releases. However, the annotations are closely aligned with the `1.2.0` metadata schema (single `reachability-metadata.json` using globs instead of regex), so a future change will not break backwards compatiblity with user code.

## Maven Instructions

You need add a compile-time dependency on the annotations, and add the annotation-processor to the list of executed annotation processors. 

Note that starting with JDK23, `javac` no longer automatically discovers or runs annotation processors from the standard classpath, so you need to explicitly enable it in the compiler arguments.

```xml
<dependencies>
    <dependency> <!-- compile time annotations -->
        <groupId>us.hebi.graalvm</groupId>
        <artifactId>reachability-annotations</artifactId>
        <version>${reachability.version}</version>
        <scope>provided</scope> <!-- not needed at runtime -->
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <annotationProcessorPaths>
                    <path> <!-- annotation processor -->
                        <groupId>us.hebi.graalvm</groupId>
                        <artifactId>reachability-processor</artifactId>
                        <version>${reachability.version}</version>
                    </path>
                </annotationProcessorPaths>
                <compilerArgs> <!-- unique identifier -->
                    <arg>-Aproject=${project.groupId}/${project.artifactId}</arg>
                </compilerArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

So far this project is in an early adopters stage, so there has not been an official release to maven central yet. However, if you have snapshots enabled, you can use `<version>1.0-SNAPSHOT</version>`.

## JitPack Snapshots

This project supports jitpack, so you can directly depend on specific commits. You can manually truncate the desired commit hash to the first 10 digits, or check [jitpack](https://jitpack.io/private#HebiRobotics/reachability-annotations) for available versions.

1. Enable jitpack releases in the pom.xml

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

2. Specify the version (first 10 digits of the commit hash)

```xml
<!-- Annotations -->
<dependency>
    <groupId>com.github.HebiRobotics</groupId>
    <artifactId>reachability-annotations</artifactId>
    <version>${reachability.hash}</version>
    <scope>provided</scope>
</dependency>

<!-- Annotation Processor -->
<dependency>
    <groupId>com.github.HebiRobotics</groupId>
    <artifactId>reachability-processor</artifactId>
    <version>${reachability.hash}</version>
    <scope>provided</scope>
</dependency>
```

## Building from source

``` bash
mvn clean verify
```
