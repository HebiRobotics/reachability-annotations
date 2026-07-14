/*-
 * #%L
 * reachability-processor
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

package us.hebi.graalvm.reachability.processor;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.common.collect.ImmutableSetMultimap;
import us.hebi.graalvm.reachability.processor.metadata.MarshallerV100;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata.ConditionalMetadata;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata.ReflectionEntry;
import us.hebi.graalvm.reachability.processor.parsers.CssParser;
import us.hebi.graalvm.reachability.processor.parsers.FxmlParser;
import us.hebi.graalvm.reachability.processor.util.ElementUtil;
import us.hebi.graalvm.reachability.processor.util.ExceptionUtil;
import us.hebi.graalvm.reachability.processor.util.GlobUtil;
import us.hebi.graalvm.reachability.processor.util.ProcessorUtil;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
                if (!coordinates.endsWith("/")) {
                    coordinates += "/";
                }
                metadataDirectory = getClassOutputDir().resolve("META-INF/native-image/reachability-generated/" + coordinates + stepId);
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

    protected AnnotationMirror getAnnotationMirror(Element type, Class<?> annotationClass) {
        String annotationName = annotationClass.getCanonicalName();
        for (AnnotationMirror mirror : type.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().toString().equals(annotationName)) {
                return mirror;
            }
        }
        return null;
    }

    protected List<AnnotationMirror> getAnnotationArrayValue(AnnotationMirror mirror, String key) {
        for (var entry : mirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(key)) {
                @SuppressWarnings("unchecked")
                var values = (List<? extends AnnotationValue>) entry.getValue().getValue();
                return values.stream()
                        .map(av -> (AnnotationMirror) av.getValue())
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
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

    protected String getConditionName(TypeElement fallbackType, AnnotationMirror mirror) {
        return tryGetDefinedCondition(mirror).orElseGet(() -> ElementUtil.getBinaryName(fallbackType));
    }

    protected Optional<String> tryGetDefinedCondition(AnnotationMirror mirror) {
        if (mirror == null) {
            return Optional.empty();
        }

        String conditionType = null;
        String conditionName = null;

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
            switch (entry.getKey().getSimpleName().toString()) {
                case "condition" -> {
                    // May be a type element or a type mirror, depending on whether it's a list
                    Object value = entry.getValue().getValue();
                    if (value instanceof TypeElement type) {
                        conditionType = ElementUtil.getBinaryName(type);
                    } else if (value instanceof TypeMirror typeMirror) {
                        Element element = env.getTypeUtils().asElement(typeMirror);
                        if (element instanceof TypeElement type) {
                            conditionType = ElementUtil.getBinaryName(type);
                        }
                    } else {
                        throw new IllegalStateException("Invalid annotation value for " + value.getClass().getSimpleName());
                    }
                }
                case "conditionName" -> conditionName = (String) entry.getValue().getValue();
            }
        }

        if (conditionType != null && !"void".equals(conditionType)) {
            return Optional.of(conditionType);
        }
        if (conditionName != null && !conditionName.isBlank()) {
            return Optional.of(conditionName);
        }
        return Optional.empty();
    }

    protected void addReflectedType(ConditionalMetadata metadata, TypeElement type, boolean includeHierarchy, Consumer<ReflectionEntry> onEntry) {
        if (!includeHierarchy) {
            addReflectedType(metadata, ElementUtil.getBinaryName(type), onEntry);
        } else {
            ElementUtil.forEachHierarchicalBinaryName(env, type, name -> addReflectedType(metadata, name, onEntry));
        }
    }

    protected void addReflectedType(ConditionalMetadata metadata, String fullyQualifiedName, boolean includeHierarchy, Consumer<ReflectionEntry> onEntry) {
        if (includeHierarchy && env.getElementUtils().getTypeElement(fullyQualifiedName) instanceof TypeElement type) {
            addReflectedType(metadata, type, includeHierarchy, onEntry);
            return;
        }
        addReflectedType(metadata, fullyQualifiedName, onEntry);
    }

    private void addReflectedType(ConditionalMetadata metadata, String typeName, Consumer<ReflectionEntry> onEntry) {
        onEntry.accept(metadata.addReflectedType(typeName));
    }

    protected void addResourceGlob(ConditionalMetadata metadata, String glob) {
        if (glob.startsWith("/")) {
            printError("globs must not start with a slash '/'.");
        }
        metadata.addResourceGlob(glob);
    }

    protected void addMetadataFromParsedFileContents(ConditionalMetadata metadata, Path file, boolean includeHierarchy) {
        if (!Files.isRegularFile(file)) {
            printError(file + " is not a regular file.");
        }

        String lowercaseFile = file.getFileName().toString().toLowerCase();

        if (lowercaseFile.endsWith(".fxml")) {
            var fxmlParser = new FxmlParser(getClassOutputDir());
            fxmlParser.addFxmlFile(file);
            for (var name : fxmlParser.getImports()) {
                if (name.contains("*")) {
                    printWarning("Ignoring unsupported wildcard import: " + name);
                    continue;
                }
                addReflectedType(metadata, name, includeHierarchy, ReflectionEntry::enableFullReflection);

            }
            for (var name : fxmlParser.getControllers()) {
                addReflectedType(metadata, name, includeHierarchy, ReflectionEntry::enableFullReflection);
            }
            for (var resource : fxmlParser.getResources()) {
                addAbsFileResource(metadata, resource);
            }
        }

        if (lowercaseFile.endsWith(".css")) {
            var cssParser = new CssParser(getClassOutputDir());
            cssParser.addCssFile(file);
            for (var resource : cssParser.getResources()) {
                addAbsFileResource(metadata, resource);
            }
        }
    }

    protected void addReflectedFieldOrMethod(ConditionalMetadata metadata, TypeElement type, Element fieldOrMethod, boolean addDefaultConstructorForFields) {
        addReflectedType(metadata, type, false, entry -> {
            switch (fieldOrMethod.getKind()) {
                case CONSTRUCTOR -> {
                    entry.addConstructor(ElementUtil.getParameterTypes(env, fieldOrMethod));
                }
                case METHOD -> {
                    entry.addMethod(fieldOrMethod.getSimpleName().toString(), ElementUtil.getParameterTypes(env, fieldOrMethod));
                }
                case FIELD -> {
                    entry.addField(fieldOrMethod.getSimpleName().toString());

                    // Also add the default constructor for the field type in case it needs
                    // to be created via reflection
                    if (addDefaultConstructorForFields) {
                        ElementUtil.getFieldType(env, fieldOrMethod).ifPresent(fqdn ->
                                addReflectedType(metadata, fqdn, false, ReflectionEntry::addConstructor));
                    }
                }
            }
        });
    }

    protected void addAbsFileResource(ConditionalMetadata metadata, Path path) {
        addResourceGlob(metadata, GlobUtil.convertPathToGlob(getClassOutputDir(), path));
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

    protected AbstractMetadataStep(String stepId, Supplier<ProcessingEnvironment> env) {
        this.stepId = stepId;
        this.environmentSupplier = env;
    }

    private Path classOutputDir;
    private final String stepId;
    private final Supplier<ProcessingEnvironment> environmentSupplier;
    protected ProcessingEnvironment env;
    protected final ReachabilityMetadata reachabilityMetadata = new ReachabilityMetadata();
    protected Path metadataDirectory;

}
