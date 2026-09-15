# Best Practices

This document describes how to find the code that needs reachability metadata, which annotation fits each case, and how to verify the result. It is based on annotating the OpenJFX modules and several JavaFX applications.

The instructions are written so that they can also be followed by coding agents. You can point an agent at this file and ask it to sweep a module, e.g., "Follow BEST_PRACTICES.md to add reachability annotations to the `foo` module and verify the generated metadata".

## The Core Rule

Only annotate what native-image cannot figure out on its own. That is

 - a class or member that gets looked up by a string name, e.g., `Class.forName`, `getMethod`, `getDeclaredField`, or JNI `FindClass`/`GetMethodID`/`GetFieldID`
 - a resource or bundle that gets loaded by name, e.g., `getResource` or `ResourceBundle.getBundle`

Direct usage like `new Foo()` or `Foo.class` needs nothing. The analysis also folds reflective calls whose arguments are constants within the same method, including compile-time string concatenation and simple helper methods. For example, `Character.class.getMethod("isIdeographic", int.class)` needs no metadata. Folding does not work through a branch (`flag ? "A" : "B"`), a mutable static field, or a name that gets computed at runtime, so only those sites need annotations.

An annotation that covers a folded site only increases the image size. When in doubt, build and read the generated JSON (see [Verification](#verification)).

## Finding Candidates

### Tracing agent

A good starting point is the [tracing agent](https://www.graalvm.org/latest/reference-manual/native-image/metadata/AutomaticMetadataCollection/), which records every reflective, JNI, and resource access while the application runs on a regular JVM.

```bash
java -agentlib:native-image-agent=config-output-dir=agent-output -jar app.jar
```

Exercise as many features as possible, e.g., open every view and dialog, then compare the recorded entries against the metadata generated from the existing annotations. Anything that is not covered yet is a candidate. Use the recorded entries as a list of findings rather than shipping them directly. They contain JDK internals and sites that native-image folds on its own, and they only cover the code paths that actually ran.

### Source searches

The agent cannot find code paths that did not run, so run all of the searches below for a new module as well. Each one finds a different category, and skipping one has caused runtime failures in practice.

1. **Reflective calls in Java**
   ```bash
   grep -rn "Class.forName\|getMethod(\|getDeclared\|getConstructor(\|newInstance\|MethodHandles\|ServiceLoader" src/main/java
   ```
   Check whether the name reaches the call as a constant. If it does not, the site needs metadata.

2. **Resources and bundles**
   ```bash
   grep -rn "getResource\|getResourceAsStream\|ResourceBundle.getBundle" src/main/java
   ls src/main/resources
   ```

3. **Classes with native methods.** Native code often caches field and method ids of its own class in an `initIDs`-style function.
   ```bash
   grep -rln " native " src/main/java
   ```

4. **Classes that native code looks up by name.** These are string literals in C/C++/Objective-C sources, e.g., `FindClass(env, "com/example/Foo")`.
   ```bash
   grep -rhoE '"(java|javax|com|org|sun|jdk)/[A-Za-z0-9_/$]+"' src/main/native | sort -u
   ```
   Also look at the `GetMethodID`/`GetFieldID`/`NewObject` calls next to each `FindClass` to see which members get used.

5. **Forced class initialization**, e.g., helpers that call `Class.forName(X.class.getName(), true, loader)` to run a static initializer of another class.
   ```bash
   grep -rn "forceInit\|ensureInitialized" src/main/java
   ```

6. **Classes instantiated from strings in configuration**, e.g., `-fx-skin` in CSS, controllers and element types in FXML, or class names in properties files and `META-INF/services`. For JavaFX, prefer `@ReachableFxView` or `@ReachableFxResources`, which parse FXML and CSS files automatically.

7. **Generated code.** If native code calls a Java factory method that instantiates a large hierarchy of generated classes, only the factory needs metadata. The instantiated classes are reached through normal Java code.

## Choosing the Annotation

Place the annotation on the class that performs the lookup, not on the target, unless the target is a class that Java code already reaches.

| Situation | Annotation | Placed on |
|:--|:--|:--|
| A class whose native methods look up its own fields and methods | `@Reachable(jniAccessible = true)` | the class itself |
| Classes that a native library looks up via `FindClass` | `@Reachable(jniAccessible = true, classes = {...})` | the Java class that loads or owns the library |
| `Class.forName(name)` over a known set of candidates | `@Reachable(classNames = {...}, memberAccess = ...)` with the smallest member access that the follow-up call needs, e.g., `ALL_DECLARED_CONSTRUCTORS` for `newInstance` | the class doing the lookup |
| Forced initialization of a class that is only referenced by name | `@Reachable(classes = Foo.class, memberAccess = {})` | the class doing the lookup |
| A single method or field looked up reflectively | `@ReachableMember` | the member |
| A skin or other class instantiated from a string | `@Reachable(condition = Control.class, memberAccess = MemberAccess.ALL_PUBLIC_CONSTRUCTORS)` | the instantiated class |
| Resources read by a class | `@Reachable(resources = "images/*.png")`, relative to the class, or with a leading `/` for the classpath root | the class that reads them |
| Resource bundles | `@Reachable(bundles = "messages")` | the class that reads them |
| FXML views and stylesheets | `@ReachableFxView` or `@ReachableFxResources` | the view or application class |

Some guidelines for the attributes:

 - Prefer `classes` over `classNames`, because class literals get checked by the compiler. Use `classNames` for classes that are not accessible from the annotated class, e.g., package-private classes in another package, classes for other platforms, or JDK internals. Be careful when converting between the two. A package-private class that fails to compile as a literal must not be dropped from the list.
 - Use the smallest `memberAccess` that works, and `memberAccess = {}` when only the class name needs to resolve.
 - Specifying any `classes`, `classNames`, `resources`, `bundles`, or `proxies` means that the annotated class itself is no longer registered. Add it to `classes` if it needs metadata as well.
 - JNI `GetFieldID` and `GetMethodID` also search superclasses, so a subclass only needs `jniAccessible` if it declares the looked-up members itself.

### Third-party libraries

Metadata for libraries that you do not control can live in a dedicated holder class in your application. Use one annotation per library, conditioned on a type that indicates that the library is used. Annotations are repeatable.

```Java
@Reachable(condition = SLF4JServiceProvider.class, resources = "/META-INF/services/org.slf4j.spi.SLF4JServiceProvider")
@Reachable(condition = IkonResolver.class, classes = {
        IkonResolver.class,
        FontAwesomeIkonHandler.class,
}, resources = "/META-INF/resources/fontawesome/*/fonts/*.ttf")
class LibraryMetadata {}
```

## Conditions

 - The default condition is the annotated class. Keep it whenever Java code references the annotated class directly, so the metadata only gets included if the class is used.
 - Set a different `condition` when the annotated class is not what triggers the lookup. A condition on a class that no Java code ever references never fires. This happens for classes that only get looked up from native code, e.g., `FindClass` targets. Register them from the class that loads the native library instead.
 - The 1.2.0 format only supports `typeReached`, which applies once the type gets initialized at runtime. A type that is part of the image but never gets initialized does not activate the metadata. The default 1.0.0 format uses `typeReachable`, which gets decided during the build. Verify your application on 1.2.0 before switching.

## Verification

Do not rely on the annotation diff. Build the project and read the generated files in `META-INF/native-image/reachability-generated/<project>/`.

 - Every candidate from the searches above should appear in the generated metadata, and nothing unexpected should appear. For JNI, compare each `FindClass` literal against the JNI entries.
 - An entry that you expected but does not appear indicates a mistake in the annotation, e.g., a wrong class name.
 - Treat `Skipping invalid pattern` and similar native-image build warnings as errors. Invalid entries get dropped without failing the build.
 - Delete the generated directory before switching `reachability.outputFormat`. Files from the previous format are not removed and get packaged along with the new ones.
 - Test the native image along the real code paths, e.g., open every view and dialog. Missing metadata typically only fails when the lookup runs.

## Common Failures

 - **Missing JNI metadata can crash the process.** A missing `FindClass` or `GetMethodID` target can cause a segmentation fault instead of an exception, e.g., on the first access of a feature that calls into native code.
 - **Forced initialization is easy to miss.** A search for native `FindClass` literals does not find helpers that initialize other classes by name. A missing registration shows up as a `ClassNotFoundException` in a static initializer.
 - **Classes that set their own skin or delegate by string.** A control that sets a default skin class name in its constructor needs the skin registered, just like `-fx-skin` in application CSS.
 - **Resource URLs are not encoded.** Resource URLs in native images are not percent-encoded, so `getResource(...).toURI()` fails for names with spaces ([oracle/graal#14441](https://github.com/oracle/graal/issues/14441)). Avoid spaces in resource names.
 - **Platform-specific metadata.** Metadata for classes that only exist on some platforms is ignored when the condition class is missing, so it is safe to register all platforms. Verify each platform separately, because a registration only works on the platform whose condition class gets reached.
