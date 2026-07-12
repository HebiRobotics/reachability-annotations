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

package us.hebi.graalvm.reachability.processor;

import com.google.common.collect.ImmutableSetMultimap;
import us.hebi.graalvm.reachability.annotations.Reachable;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata.ReflectionEntry;
import us.hebi.graalvm.reachability.processor.util.ProcessorUtil;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Florian Enner
 * @since 27 Nov 2025
 */
public class ReachableStep extends AbstractMetadataStep {

    @Override
    public Set<String> annotations() {
        return Set.of(
                Reachable.class.getCanonicalName(),
                Reachable.List.class.getCanonicalName()
        );
    }

    @Override
    public void process0(ImmutableSetMultimap<String, Element> elementMap) {
        // Single annotation
        for (Element element : elementMap.get(Reachable.class.getCanonicalName())) {
            if (element instanceof TypeElement type) {
                var mirror = getAnnotationMirror(type, Reachable.class);
                if (mirror != null) {
                    Reachable annotation = type.getAnnotation(Reachable.class);
                    processAnnotation(type, annotation, mirror);
                }
            }
        }

        // Multiple annotations
        for (Element element : elementMap.get(Reachable.List.class.getCanonicalName())) {
            if (element instanceof TypeElement type) {
                var listMirror = getAnnotationMirror(type, Reachable.List.class);
                if (listMirror != null) {
                    var listAnnotation = type.getAnnotation(Reachable.List.class);
                    var mirrors = getAnnotationArrayValue(listMirror, "value");
                    var annotations = listAnnotation.value();
                    for (int i = 0; i < annotations.length; i++) {
                        processAnnotation(type, annotations[i], mirrors.get(i));
                    }
                }
            }
        }
    }

    private final Set<String> tmpLocales = new HashSet<>();

    private void processAnnotation(TypeElement annotatedType, Reachable annotation, AnnotationMirror mirror) {
        final var metadata = getConditionalMetadata(getConditionName(annotatedType, mirror));
        boolean fieldsEmpty = true;

        // Absolute & relative resource paths
        if (annotation.resources().length > 0) {
            fieldsEmpty = false;
            var baseDir = ProcessorUtil.getSourceDirectory(env, annotatedType);
            for (var pattern : annotation.resources()) {
                if (pattern.startsWith("/")) {
                    metadata.addResourceGlob(pattern.substring(1));
                } else {
                    metadata.addResourceGlob(baseDir + pattern);
                }
            }
        }

        // Resource bundles
        for (var bundle : annotation.bundles()) {
            fieldsEmpty = false;
            var entry = metadata.addBundle(bundle.name());
            for (var locale : bundle.locales()) {
                entry.getLocales().add(locale);
            }
        }

        // Explicitly added proxy interface names
        for (var proxy : annotation.proxies()) {
            fieldsEmpty = false;
            metadata.addProxyInterfaces(proxy.value());
        }

        // JNI and reflectively accessible classes
        final Consumer<ReflectionEntry> updateReflectEntry = entry -> {
            entry.allDeclaredConstructors |= annotation.allDeclaredConstructors();
            entry.allDeclaredMethods |= annotation.allDeclaredMethods();
            entry.allDeclaredFields |= annotation.allDeclaredFields();
            entry.jniAccessible |= annotation.jniAccessible();
        };
        for (String name : annotation.classNames()) {
            fieldsEmpty = false;
            addReflectedType(metadata, name, annotation.includeClassHierarchy(), updateReflectEntry);
        }
        try {
            // Processing classes immediately hit a MirroredTypeException,
            // so this code doesn't actually run.
            for (var clazz : annotation.classes()) {
                fieldsEmpty = false;
                addReflectedType(metadata, clazz.getName(), annotation.includeClassHierarchy(), updateReflectEntry);
            }
        } catch (MirroredTypesException e) {
            // Add all target classes and their parents
            for (var typeMirror : e.getTypeMirrors()) {
                fieldsEmpty = false;
                if (env.getTypeUtils().asElement(typeMirror) instanceof TypeElement typeElement) {
                    addReflectedType(metadata, typeElement, annotation.includeClassHierarchy(), updateReflectEntry);
                }
            }
        }

        // Nothing else defined -> enable reflection of the annotated type itself
        if (fieldsEmpty) {
            addReflectedType(metadata, annotatedType, annotation.includeClassHierarchy(), updateReflectEntry);
        }

    }

    public ReachableStep(Supplier<ProcessingEnvironment> env) {
        super("reachable-generated", env);
    }

}
