# Reachability Annotations

This library provides standalone zero-dependency annotations for generating [GraalVM Native Image](https://www.graalvm.org/native-image/) [Reachability Metadata](https://www.graalvm.org/latest/reference-manual/native-image/metadata/) at compile time. They are compatible with standard Java code and are independent of any framework like Quarkus or Micronaut.

The annotations also make extensive use of reachability conditions. By default, the settings are only applied if the annotated type is reachable in the native image. This mitigates binary size explosion for features that aren't used. The condition can be changed by setting a custom `condition`, or disabled by setting `condition = Object.class`.

You can look at [BEST_PRACTICES.md](BEST_PRACTICES.md) for examples of how to find reflective, JNI, and resource lookups, which annotation fits each case, and how to verify the result.

Note that the current default output is the older 1.0.0 format with separate `*-config.json` files, but this will likely change to 1.2.0 with a combined `reachability-metadata.json` in the future. The newer format has a narrower reachability condition (`typeReached` instead of `typeReachable`), so you should always specify the format to avoid surprises (see [Generated Metadata](#generated-metadata)). The examples below show the 1.2.0 output for readability.

## Default Case

```Java
@Reachable
public class Example {}
```

The default configuration enables full reflection with a condition on the annotated type

```json
{
  "condition": { "typeReached": "reachability.sample.Example" },
  "type": "reachability.sample.Example",
  "allDeclaredMethods": true,
  "allDeclaredFields": true,
  "allDeclaredConstructors": true
}
```

Specifying other targets (`classes`, `classNames`, `resources`, `bundles`, or `proxies`) replaces the default class. If the annotated class is needed as well, it needs to be added to the list

```Java
@Reachable // registers Example
public class Example {}

@Reachable(classes = { Other.class }) // registers only Other
public class Example {}

@Reachable(classes = { Example.class, Other.class }) // registers Example and Other
public class Example {}

@Reachable(resources = { "images/*.png" }) // registers only resources
public class Example {}

@Reachable(classes = Example.class, resources = { "images/*.png" }) // registers Example and resources
public class Example {}
```

## Fine Control

```Java
@Reachable(
        condition = InetSocketAddress.class,
        classes = { Example.class },
        memberAccess = {
                MemberAccess.ALL_DECLARED_CONSTRUCTORS,
                MemberAccess.ALL_PUBLIC_METHODS,
        },
        resources = { "images/*.png" },
        bundles = { "bundle" },
        jniAccessible = true)
public class Example {

	@ReachableMember
	String field;

}
```

The deeper annotation settings enable access to all of the fields available in the metadata format. Relative resource/bundle paths are relative to the annotated type.

```json
    {
      "condition": { "typeReached": "java.net.InetSocketAddress" },
      "type": "reachability.sample.Example",
      "fields": [
        { "name": "field" }
      ],
      "allDeclaredConstructors": true,
      "allPublicMethods": true,
      "jniAccessible": true
    }
    
    {
      "condition": { "typeReached": "java.net.InetSocketAddress" },
      "glob": "reachability/sample/images/*.png"
    }
    
    {
      "condition": { "typeReached": "java.net.InetSocketAddress" },
      "bundle": "reachability.sample.bundle"
    }
```

## Supported Annotations

Below is a list of all supported annotations and some examples to get started. Check the JavaDoc for more detailed documentation.

### @Reachable

Declares classes, resources, dynamic proxies, and bundles that should be available in the native image. 

```Java
// Enables full reflection if the annotated class is reached from any point
@Reachable
public enum ReflectivelyAccessedEnum {}

// Full reflection of all specified classes and their parent hierarchy. The annotated
// class itself is not part of the list unless explicitly specified
@Reachable(
        includeClassHierarchy = true,
        classes = {InetAddress.class, InetSocketAddress.class},
        classNames = {"some.internal.class$Nested", "other.internal.class"}
)
public class ReferencingOtherClasses {}

// Resource globs relative to the annotated class
@Reachable(condition = ImageLoader.class, resources = {
        "images/*.png",
        "images/*.jpg",
})
public class RelativeResourceGlobs {}

// Resource globs relative to the class path root (start with '/')
@Reachable(conditionName = "custom.condition.ImageLoader", resources = {
        "/assets/*.png",
        "/assets/*.jpg",
})
public class AbsoluteResourceGlobs {}

// Unconditional custom access of a hidden class w/ JNI
@Reachable(
        condition = Object.class,
        classNames = "sun.misc.Unsafe", 
        memberAccess = { MemberAccess.ALL_DECLARED_FIELDS},
        jniAccessible = true
)
public static class FineGrainedAccess {}
```

### @ReachableMember, @FXML

The `@ReachableMember` annotation provides more fine-grained access for configuring access to individual fields and methods. The JavaFX annotation `@FXML` gets picked up as well with the same behavior.

```java
public static class IndividualFieldsAndMethods {
    
    @ReachableMember
    void doNothing(String input, Object output) {}

    @ReachableMember
    String field1;

}
```

### @ReachableFxView

This is a special annotation for working with JavaFX FXML - a markup language for GUI layouts that makes extensive use of reflection.

Matching FXML and CSS files on the output classpath are automatically parsed, and add reachability configuration for the classes and resources they use.

The default naming convention follows established conventions (see [FxmlKit](https://github.com/dlsc-software-consulting-gmbh/FxmlKit) or [Afterburner.fx](https://github.com/adambien/afterburner.fx)) to determine the resource names based on the lowercased view name. For example,
 - `${Name}View.java` -> `DialogView.java`
 - `${name}.fxml` -> `dialog.fxml`
 - `${name}.css` -> `dialog.css`
 - `${name}*.properties` -> `dialog_en.properties`, `dialog_de.properties`, ...

```Java
@ReachableFxView("dialog") // checks for dialog.fxml, dialog.css, dialog.properties etc.
public class DialogView extends com.airhacks.afterburner.views.FXMLView {}
```


### @ReachableFxResources

Another JavaFX related annotation that provides more resource control and can parse multiple FXML/CSS files based on globs.

```Java
// Enables full reflection of all found classes and their parent hierarchy
@ReachableFxResources({
        "/assets/images/*.png", // relative to classpath root
        "views/**/*.fxml", // relative to annotated type
        "views/**/*.css", // relative to annotated type
})
public class MyApp extends Application {}
```

<!--- TODO: maybe integrate? seems unnecessary.
### FXML and CSS Parsing

Both JavaFX annotations parse the FXML and CSS files they find and register what `FXMLLoader` and the CSS engine look up reflectively at runtime.

FXML files register
 - all explicitly imported classes. `FXMLLoader` loads every explicit import eagerly, so unused imports need metadata as well
 - element tags that construct objects, e.g., `<VBox/>`, including fully qualified tags and nested classes like `ButtonBar.ButtonData`
 - `fx:controller` and `fx:root` types, and owners of static properties like `GridPane.vgrow`
 - enum and `valueOf(String)` parameter types of setters that receive attribute values, e.g., `Priority` for `HBox.hgrow="ALWAYS"`
 - files referenced via `fx:include` or `@` locations like `url="@../images/logo.png"`. Referenced FXML and CSS files get parsed as well

Class names are resolved like `FXMLLoader` does it, i.e., names starting with a lowercase character are fully qualified, then explicit imports, then wildcard imports. Wildcard imports only register the classes that are actually used. Resolving names requires the referenced types to be on the compile classpath, e.g., a `provided` dependency on `javafx-controls`. Names that cannot be resolved are written as they are and have no effect.

CSS files register
 - resources referenced via `url()` and `@import`
 - image and font properties that omit the `url()` wrapper, e.g., `-fx-image: "icon.png"`
 - skin classes referenced via `-fx-skin`, with their public constructors

Note that the CSS parser does not remove JavaFX-specific `//` line comments yet, so commented-out declarations still get registered. Inline `data:` URIs are not supported.
-->

### @Inject, @PostConstruct, @PreDestroy

This processor also includes an opt-in feature that looks at common dependency injection annotations (both `javafx.*` and `jakarta.*`), and generates reflection configurations that covers most standard usage. It is opt-in to avoid interfering with static code generators of other frameworks.

You can enable it using the compiler argument `-Areachability.processDependencyInjection=true`. See below for more details.

For example,

```Java
public class InjectionSample {

    @Inject
    InjectionSample(String name) {}

    @PostConstruct
    void postConstruct() {}

    @PreDestroy
    void preDestroy() {}

    @Inject
    InjectedType injectedField;

}
```

generates metadata for the class

```json
{
  "condition": { "typeReached": "demo.InjectionSample" },
  "type": "demo.InjectionSample",
  "methods": [
    { "name": "<init>", "parameterTypes": ["java.lang.String"] },
    { "name": "postConstruct", "parameterTypes": [] },
    { "name": "preDestroy", "parameterTypes": [] }
  ],
  "fields": [
    { "name": "injectedField" }
  ]
}
```

as well as default constructors for the types of injected fields

```json
{
  "condition": { "typeReached": "demo.InjectionSample" },
  "type": "demo.InjectedType",
  "methods": [
    { "name": "<init>", "parameterTypes": [] }
  ]
}
```

## Generated Metadata

The metadata gets generated into the `META-INF/native-image/reachability-generated/${project}/` directory. The `${project}` name should be unique and needs to be set via a compiler argument. This is compatible with [picocli-codegen](https://github.com/remkop/picocli/blob/main/picocli-codegen/README.adoc#224-maven).

The processor currently defaults to the older 1.0.0 format (separate `*-config.json` files) rather than the modern 1.2.0 format (combined `reachability-metadata.json`) for three reasons: (1) the modern format is not yet fully implemented, (2) the older format is still supported by all GraalVM versions, and (3) narrowed condition behavior: the older `typeReachable` condition triggers as soon as a type is included in the image, while the newer `typeReached` condition adds a runtime trigger that requires the class to be initialized. In practice this is too limiting for some use cases and can cause issues at runtime (e.g., when the lookup can run before the condition class gets initialized).

However, for most applications the difference does not matter, and it is likely that the default will change to the newer format in the future. We recommend always setting the format explicitly with the compiler option `-Areachability.outputFormat=1.0.0` to avoid future surprises.

For example, a Maven configuration could look like this:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <compilerArgs>
            <arg>-Aproject=${project.groupId}/${project.artifactId}</arg>
            <arg>-Areachability.outputFormat=1.0.0</arg> <!-- or 1.2.0 -->
        </compilerArgs>
    </configuration>
</plugin>
```

### Generator Options

| Compiler Argument (`-A`)                  | Values                             | Comment                                                                                           |
|:------------------------------------------|:-----------------------------------|:--------------------------------------------------------------------------------------------------|
| `project`                                 | `directory`                         | Should be `${project.groupId}/${project.artifactId}`                                              |
| `reachability.outputFormat`              | `1.0.0` (default), `1.2.0`, `all`  | The output format of the metadata                                                                 |
| `reachability.mergeSteps`                 | `true` (default), `false`          | Whether the different processing steps should be merged into a single file. Mainly for debugging. |
| `reachability.processDependencyInjection` | `false` (default), `true`          | True enables metadata generation for `@Inject` annotations.

## Maven Instructions

You need add a compile-time dependency on the annotations, and add the annotation-processor to the list of executed annotation processors. 

Note that starting with JDK23, `javac` no longer runs annotation processors by default. You can set the `annotationProcessorPaths` as shown below, or use `<proc>full</proc>` in the compiler plugin configuration (`-proc:full`).

```xml
<properties>
    <reachability.version>1.0.0-RC4</reachability.version>
</properties>

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
                    <arg>-Areachability.processDependencyInjection=true</arg>
                    <arg>-Areachability.outputFormat=1.0.0</arg>
                </compilerArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Building from source

``` bash
mvn clean verify
```

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
