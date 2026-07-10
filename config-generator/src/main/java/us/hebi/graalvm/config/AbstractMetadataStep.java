/*-
 * #%L
 * Native Config Generator
 * %%
 * Copyright (C) 2026 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package us.hebi.graalvm.config;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.common.collect.ImmutableSetMultimap;
import us.hebi.graalvm.config.metadata.MarshallerV100;
import us.hebi.graalvm.config.metadata.ReachabilityMetadata;
import us.hebi.graalvm.config.metadata.ReachabilityMetadata.ConditionalMetadata;
import us.hebi.graalvm.config.metadata.ReachabilityMetadata.ReflectionEntry;
import us.hebi.graalvm.config.parsers.CssParser;
import us.hebi.graalvm.config.parsers.FxmlParser;
import us.hebi.graalvm.config.util.ElementUtil;
import us.hebi.graalvm.config.util.ExceptionUtil;
import us.hebi.graalvm.config.util.ProcessorUtil;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Florian Enner
 * @since 27 Nov 2025
 */
public abstract class AbstractMetadataStep implements BasicAnnotationProcessor.Step {

    @Override
    public final Set<? extends Element> process(ImmutableSetMultimap<String, Element> elementMap) {
        this.env = environmentSupplier.get();
        try {
            // Merge any existing files in case we do a partial compilation
            if (metadataDirectory == null) {
                String coordinates = env.getOptions().getOrDefault("project", "");
                metadataDirectory = getClassOutputDir().resolve("META-INF/native-image/" + name + "/" + coordinates);
                MarshallerV100.mergeMetadataFrom(metadataDirectory, reachabilityMetadata);
            }

            // Process
            if (!elementMap.isEmpty()) {
                process0(elementMap);
            }

        } catch (Exception e) {
            printError(ExceptionUtil.getStackTrace(e));
        }
        return Collections.emptySet();
    }

    protected void printError(String message) {
        env.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }

    protected void printWarning(String message) {
        env.getMessager().printMessage(Diagnostic.Kind.WARNING, message);
    }

    public abstract void process0(ImmutableSetMultimap<String, Element> elementMap);

    public void finish() {
        try {
            // Save result to disk. Abort if nothing has been processed
            if (metadataDirectory != null) {
                MarshallerV100.saveMetadataTo(reachabilityMetadata, metadataDirectory);
            }
        } catch (IOException e) {
            printError(ExceptionUtil.getStackTrace(e));
        }
    }

    protected String getConditionName(TypeElement element, Class<? extends Annotation> annotation) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (!mirror.getAnnotationType().toString().equals(annotation.getName())) {
                continue;
            }
            return getConditionName(element, mirror);
        }
        throw new IllegalStateException("No annotation found for " + annotation);
    }

    protected String getConditionName(TypeElement annotatedType, AnnotationMirror mirror) {
        TypeElement specifiedClass = null;
        String className = null;

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
            switch (entry.getKey().getSimpleName().toString()) {
                case "condition" -> {
                    // May be a type element or a type mirror, depending on whether it's a list or not
                    Object value = entry.getValue().getValue();
                    if (value instanceof TypeElement type) {
                        specifiedClass = type;
                    } else if (value instanceof TypeMirror typeMirror) {
                        Element element = env.getTypeUtils().asElement(typeMirror);
                        if (element instanceof TypeElement typeElement) {
                            specifiedClass = typeElement;
                        }
                    } else {
                        throw new IllegalStateException("Invalid annotation value for " + value.getClass().getSimpleName());
                    }
                }
                case "conditionName" -> className = (String) entry.getValue().getValue();
            }
        }
        return getConditionName(specifiedClass, className, annotatedType);
    }

    protected String getConditionName(TypeElement conditionType, String conditionName, TypeElement annotatedType) {
        // Try to get the class type from the annotation using TypeMirror
        try {
            // This will throw MirroredTypeException, but we catch it to get the TypeMirror
            if (conditionType != null && !Objects.equals(conditionType, ElementUtil.asTypeElement(env, void.class))) {
                return ElementUtil.getBinaryName(conditionType);
            }
        } catch (MirroredTypeException ignored) {
            // Expected when accessing Class<?> during annotation processing
        }

        if (conditionName != null && !conditionName.isBlank()) {
            return conditionName;
        }
        return annotatedType == null ? "" : ElementUtil.getBinaryName(annotatedType);
    }

    protected void addReflectedType(ConditionalMetadata metadata, String typeName, Consumer<ReflectionEntry> onEntry) {
        onEntry.accept(metadata.addReflectedType(typeName));
    }

    protected void addReflectedType(ConditionalMetadata metadata, TypeElement type, Consumer<ReflectionEntry> onEntry) {
        ElementUtil.forEachHierarchicalBinaryName(env, type, name -> {
            addReflectedType(metadata, name, onEntry);
        });
    }

    protected void addResourceGlob(ConditionalMetadata metadata, String glob) {
        if (glob.startsWith("/")) {
            printError("globs must not start with a slash '/'.");
        }
        metadata.addResourceGlob(glob);
    }

    protected void addResourceFile(ConditionalMetadata metadata, Path path) {
        String glob = getClassOutputDir().relativize(path).toString();
        addResourceGlob(metadata, glob.replace('\\', '/'));
    }

    protected void addMetadataFromParsedFileContents(ConditionalMetadata metadata, Path file) {
        if (!Files.isRegularFile(file)) {
            printError(file + " is not a regular file.");
        }

        String lowercaseFile = file.getFileName().toString().toLowerCase();

        if (lowercaseFile.endsWith(".fxml")) {
            var fxmlParser = new FxmlParser();
            fxmlParser.addFxmlFile(file);
            for (var name : fxmlParser.getImports()) {
                if (name.contains("*")) {
                    printWarning("Ignoring unsupported wildcard import: " + name);
                    continue;
                }
                addReflectedType(metadata, name, ReachabilityMetadata.ReflectionEntry::enableFullReflection);

            }
            for (var name : fxmlParser.getControllers()) {
                addReflectedType(metadata, name, ReachabilityMetadata.ReflectionEntry::enableFullReflection);
            }
            for (var resource : fxmlParser.getResources()) {
                addResourceFile(metadata, resource);
            }
        }

        if (lowercaseFile.endsWith(".css")) {
            var cssParser = new CssParser();
            cssParser.addCssFile(file);
            for (var resource : cssParser.getResources()) {
                addResourceFile(metadata, resource);
            }
        }
    }

    protected Path getClassOutputDir() {
        if (classOutputDir == null) {
            classOutputDir = ProcessorUtil.getClassOutputDirectory(env);
        }
        return classOutputDir;
    }

    protected ConditionalMetadata getConditionalMetadata(String condition) {
        return reachabilityMetadata.getMetadata(condition);
    }

    protected AbstractMetadataStep(String name, Supplier<ProcessingEnvironment> env) {
        this.name = name;
        this.environmentSupplier = env;
    }

    private Path classOutputDir;
    private final String name;
    private final Supplier<ProcessingEnvironment> environmentSupplier;
    protected ProcessingEnvironment env;
    protected final ReachabilityMetadata reachabilityMetadata = new ReachabilityMetadata();
    protected Path metadataDirectory;

}
