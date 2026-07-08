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

import com.google.common.collect.ImmutableSetMultimap;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Florian Enner
 * @since 27 Nov 2025
 */
public class SpecAnnotationProcessor extends AbstractConfigStep {

    @Override
    public Set<String> annotations() {
        return Set.of(
                NativeConfig.class.getCanonicalName(),
                NativeConfig.List.class.getCanonicalName()
        );
    }

    @Override
    public void process0(ImmutableSetMultimap<String, Element> elementMap) {

        // Add manually specified resources
        for (Element element : elementMap.get(NativeConfig.class.getCanonicalName())) {
            if (element instanceof TypeElement type) {
                var mirror = getAnnotationMirror(type, NativeConfig.class);
                if (mirror != null) {
                    NativeConfig annotation = type.getAnnotation(NativeConfig.class);
                    processConfigAnnotation(type, annotation, mirror);
                }
            }
        }

        // Add manually specified resources from List annotations
        for (Element element : elementMap.get(NativeConfig.List.class.getCanonicalName())) {
            if (element instanceof TypeElement type) {
                var listMirror = getAnnotationMirror(type, NativeConfig.List.class);
                if (listMirror != null) {
                    NativeConfig.List listAnnotation = type.getAnnotation(NativeConfig.List.class);
                    var mirrors = getAnnotationArrayValue(listMirror, "value");
                    var annotations = listAnnotation.value();
                    for (int i = 0; i < annotations.length; i++) {
                        processConfigAnnotation(type, annotations[i], mirrors.get(i));
                    }
                }
            }
        }

    }

    private final Set<String> tmpLocales = new HashSet<>();

    private void processConfigAnnotation(TypeElement type, NativeConfig annotation, AnnotationMirror mirror) {
        final var config = getConfig(type, mirror);

        // Absolute resources
        for (var pattern : annotation.resources()) {
            config.addResourcePattern(pattern);
        }

        // Resource bundles
        for (String bundle : annotation.bundleNames()) {
            var id = config.getBundleIdentifier(bundle);
            tmpLocales.clear();
            id.getMutableLocales().forEach(tmpLocales::add);

            for (String locale : annotation.bundleLocales()) {
                if (!tmpLocales.contains(locale)) {
                    id.addLocales(locale);
                }
            }
        }

        // Resources (relative to the specified condition)
        var sourceDir = getSourceDirectory(type); // TODO: condition class or annotated type?
        for (String pattern : annotation.relativeResources()) {
            config.addResourcePattern(sourceDir + pattern);
        }

        // Explicitly added proxy interface names
        for (String name : annotation.proxyInterfaceNames()) {
            config.addProxyInterface(name);
        }

        // Reflectively accessible classes
        for (String name : annotation.classNames()) {
            config.addReflectedType(name);
        }
        try {
            for (var clazz : annotation.classes()) {
                config.addReflectedType(clazz.getName());
            }
        } catch (MirroredTypesException e) {
            for (var typeMirror : e.getTypeMirrors()) {
                Element classElement = processingEnv.getTypeUtils().asElement(typeMirror);
                if (classElement instanceof TypeElement typeElement) {
                    config.addReflectedType(typeElement);
                }
            }
        }

        // JNI-accessible types
        for (String name : annotation.jniClassNames()) {
            config.addJniType(name);
        }
        try {
            for (var clazz : annotation.jniClasses()) {
                config.addJniType(clazz.getName());
            }
        } catch (MirroredTypesException e) {
            for (var typeMirror : e.getTypeMirrors()) {
                Element classElement = processingEnv.getTypeUtils().asElement(typeMirror);
                if (classElement instanceof TypeElement typeElement) {
                    config.addJniType(typeElement);
                }
            }
        }

    }

    private AnnotationMirror getAnnotationMirror(TypeElement type, Class<?> annotationClass) {
        String annotationName = annotationClass.getCanonicalName();
        for (AnnotationMirror mirror : type.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().toString().equals(annotationName)) {
                return mirror;
            }
        }
        return null;
    }

    private java.util.List<AnnotationMirror> getAnnotationArrayValue(AnnotationMirror mirror, String key) {
        for (var entry : mirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(key)) {
                @SuppressWarnings("unchecked")
                var values = (java.util.List<? extends AnnotationValue>) entry.getValue().getValue();
                return values.stream()
                        .map(av -> (AnnotationMirror) av.getValue())
                        .collect(java.util.stream.Collectors.toList());
            }
        }
        return java.util.Collections.emptyList();
    }

    public SpecAnnotationProcessor(Supplier<ProcessingEnvironment> env) {
        super("generated-annotations", env);
    }

}
