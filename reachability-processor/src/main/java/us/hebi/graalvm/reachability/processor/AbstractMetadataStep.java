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

import lombok.Getter;
import us.hebi.graalvm.reachability.annotations.MemberAccess;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata.ConditionalMetadata;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata.ReflectionEntry;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata.ResourceEntry;
import us.hebi.graalvm.reachability.processor.parsers.CssParser;
import us.hebi.graalvm.reachability.processor.parsers.FxmlParser;
import us.hebi.graalvm.reachability.processor.util.ElementUtil;
import us.hebi.graalvm.reachability.processor.util.ExceptionUtil;
import us.hebi.graalvm.reachability.processor.util.GlobUtil;
import us.hebi.graalvm.reachability.processor.util.ProcessorUtil;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
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
public abstract class AbstractMetadataStep {

    /**
     * @return fully qualified names of the supported annotations
     */
    public abstract Set<String> annotations();

    public final void process(Map<String, Set<Element>> elementMap) {
        this.env = environmentSupplier.get();
        try {
            // Initialize metadata
            if (reachabilityMetadata == null) {
                reachabilityMetadata = new ReachabilityMetadata();
            }

            // Process
            if (!elementMap.isEmpty()) {
                process0(elementMap);
            }

        } catch (Exception e) {
            printError(ExceptionUtil.getStackTrace(e));
        }
    }

    protected void printError(String message) {
        env.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }

    protected void printWarning(String message) {
        env.getMessager().printMessage(Diagnostic.Kind.WARNING, message);
    }

    public abstract void process0(Map<String, Set<Element>> elementMap);

    protected AnnotationMirror getAnnotationMirror(Element type, Class<?> annotationClass) {
        String annotationName = annotationClass.getCanonicalName();
        for (AnnotationMirror mirror : type.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().toString().equals(annotationName)) {
                return mirror;
            }
        }
        return null;
    }

    /**
     * @return true if the attribute was explicitly specified in the source
     */
    protected boolean isSpecified(AnnotationMirror mirror, String key) {
        for (var attribute : mirror.getElementValues().keySet()) {
            if (attribute.getSimpleName().toString().equals(key)) {
                return true;
            }
        }
        return false;
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

    protected void addAndTryParseResource(ConditionalMetadata metadata, Path resource, boolean includeHierarchy) {
        addAbsFileResource(metadata, resource);
        if (Files.isRegularFile(resource)) {
            addMetadataFromParsedFileContents(metadata, resource, includeHierarchy);
        }
    }

    protected void addMetadataFromParsedFileContents(ConditionalMetadata metadata, Path file, boolean includeHierarchy) {
        if (!Files.isRegularFile(file)) {
            printError(file + " is not a regular file.");
        }

        String lowercaseFile = file.getFileName().toString().toLowerCase();

        if (lowercaseFile.endsWith(".fxml")) {
            for (var fxmlFile : FxmlParser.parse(getClassOutputDir(), file)) {
                for (var name : fxmlFile.getImports()) {
                    if (name.contains("*")) {
                        continue;
                    }
                    addReflectedType(metadata, name, includeHierarchy, ReflectionEntry::enableFullReflection);
                }
                for (var entry : fxmlFile.getProperties().entrySet()) {
                    var className = resolveClassName(entry.getKey(), fxmlFile.getImports());
                    addReflectedType(metadata, className, includeHierarchy, ReflectionEntry::enableFullReflection);
                    addCoercedPropertyTypes(metadata, className, entry.getValue());
                }
                addAbsFileResource(metadata, fxmlFile.getPath());
                for (var resource : fxmlFile.getResources()) {
                    addAbsFileResource(metadata, resource);
                    // Referenced stylesheets need to be parsed as well, e.g. stylesheets="@../styles/style.css".
                    // Fxml files stay out: the parser already visited them, and re-dispatching could cycle.
                    if (resource.getFileName().toString().toLowerCase().endsWith(".css") && Files.isRegularFile(resource)) {
                        addMetadataFromParsedFileContents(metadata, resource, includeHierarchy);
                    }
                }
            }
        }

        if (lowercaseFile.endsWith(".css")) {
            var cssParser = new CssParser(getClassOutputDir());
            cssParser.addCssFile(file);
            for (var name : cssParser.getClasses()) {
                // Skins get looked up via Class::getConstructors and instantiated with the control
                addReflectedType(metadata, name, includeHierarchy, entry -> entry.addMemberAccess(MemberAccess.ALL_PUBLIC_CONSTRUCTORS));
            }
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

                    // We also add the default constructor for the field type in case it needs to be created via
                    // reflection. Note that we add the reachable condition on the target type due to an issue we
                    // encountered when using the 1.2.0 output format.
                    if (addDefaultConstructorForFields) {
                        ElementUtil.getFieldType(env, fieldOrMethod).ifPresent(fqdn ->
                                addReflectedType(getConditionalMetadata(fqdn), fqdn, false, ReflectionEntry::addConstructor)
                        );
                    }
                }
            }
        });
    }

    /**
     * @return the class name resolved against the file's imports, or the input if no import matches
     */
    private String resolveClassName(String name, List<String> imports) {
        // FXMLLoader treats names starting with a lowercase character as fully qualified.
        if (Character.isLowerCase(name.charAt(0))) {
            return name;
        }

        // Explicit imports win over wildcard packages (nested classes keep their hierarchy, so works as well)
        for (String fqname : imports) {
            if (fqname.endsWith("." + name)) {
                return fqname;
            }
        }

        // Try wildcards
        for (String fqname : imports) {
            if (fqname.endsWith(".*")) {
                var candidate = fqname.substring(0, fqname.length() - 1) + name;
                if (env.getElementUtils().getTypeElement(candidate) != null) {
                    return candidate;
                }
            }
        }

        // Fall back to original name. TODO: should we fail instead?
        return name;
    }

    /**
     * The reflectively used classes are already fully added, but a property setter like
     * <Button alignment="CENTER" /> would also need the enum parameter. This adds metadata
     * for the setters.
     * <p>
     * FXMLLoader coerces attribute strings into the setter's parameter type, and BeanAdapter's
     * fallback looks up valueOf(String) reflectively. String, primitives and BigDecimal/BigInteger
     * need no metadata.
     */
    private void addCoercedPropertyTypes(ConditionalMetadata metadata, String className, Set<String> propertyNames) {
        var type = env.getElementUtils().getTypeElement(className);
        if (type == null) {
            return;
        }

        var setterNames = new TreeSet<String>();
        for (var propertyName : propertyNames) {
            setterNames.add("set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1));
        }

        for (var member : env.getElementUtils().getAllMembers(type)) {
            if (member.getKind() != ElementKind.METHOD
                    || !member.getModifiers().contains(Modifier.PUBLIC)
                    || !setterNames.contains(member.getSimpleName().toString())) {
                continue;
            }
            var parameters = ((ExecutableElement) member).getParameters();
            int setterParameters = member.getModifiers().contains(Modifier.STATIC) ? 2 : 1;
            if (parameters.size() != setterParameters) {
                continue;
            }
            var valueType = parameters.get(parameters.size() - 1).asType();
            if (!(env.getTypeUtils().asElement(valueType) instanceof TypeElement propertyType)) {
                continue;
            }
            var binaryName = ElementUtil.getBinaryName(propertyType);
            if (propertyType.getKind() == ElementKind.ENUM) {
                // Enum.valueOf internally reaches values() as well
                addReflectedType(metadata, binaryName, false, entry -> entry.addMemberAccess(MemberAccess.ALL_DECLARED_METHODS));
            } else if (!binaryName.startsWith("java.") && hasDeclaredValueOfString(propertyType)) {
                addReflectedType(metadata, binaryName, false, entry -> entry.addMethod("valueOf", "java.lang.String"));
            }
        }
    }

    private boolean hasDeclaredValueOfString(TypeElement type) {
        for (var member : type.getEnclosedElements()) {
            if (member.getKind() != ElementKind.METHOD
                    || !member.getModifiers().contains(Modifier.PUBLIC)
                    || !member.getModifiers().contains(Modifier.STATIC)
                    || !"valueOf".contentEquals(member.getSimpleName())) {
                continue;
            }
            var parameters = ((ExecutableElement) member).getParameters();
            if (parameters.size() == 1 && "java.lang.String".equals(parameters.get(0).asType().toString())) {
                return true;
            }
        }
        return false;
    }

    private void addAbsFileResource(ConditionalMetadata metadata, Path path) {
        var relativeFile = getClassOutputDir().relativize(path);
        var glob = GlobUtil.ensureForwardSlashPath(relativeFile);
        metadata.addGlob(new ResourceEntry("", glob));
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
    protected final String stepId;
    private final Supplier<ProcessingEnvironment> environmentSupplier;
    protected ProcessingEnvironment env;

    @Getter
    private ReachabilityMetadata reachabilityMetadata;

}
