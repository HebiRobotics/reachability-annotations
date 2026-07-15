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

import com.google.common.collect.ImmutableSetMultimap;
import us.hebi.graalvm.reachability.annotations.Reachable;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata.ReflectionEntry;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata.ResourceEntry;
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
public class ReachableStep extends RepeatedMetadataStep<Reachable> {

    public ReachableStep(Supplier<ProcessingEnvironment> env) {
        super("reachable", env, Reachable.class, Reachable.List.class);
    }

    @Override
    protected void processAnnotation(ReachabilityMetadata.ConditionalMetadata metadata, TypeElement type, Reachable annotation) {
        boolean fieldsEmpty = true;

        // Absolute & relative resource paths
        if (annotation.resources().length > 0 || annotation.bundles().length > 0) {
            fieldsEmpty = false;
            var baseDir = ProcessorUtil.getSourceDirectory(env, type);

            // Resource globs
            for (var resource : annotation.resources()) {
                metadata.addGlob(ResourceEntry.fromString(baseDir, resource));
            }

            // Bundles
            for (var resource : annotation.bundles()) {
                metadata.addBundle(ResourceEntry.fromString(baseDir, resource));
            }

        }

        // Explicitly added proxy interface names
        for (var proxy : annotation.proxies()) {
            fieldsEmpty = false;
            metadata.addProxyInterfaces(proxy.value());
        }

        // JNI and reflectively accessible classes
        final Consumer<ReflectionEntry> updateReflectEntry = entry -> {
            entry.addMemberAccess(annotation.memberAccess());
            entry.jniAccessible |= annotation.jniAccessible();
            entry.serializable |= annotation.serializable();
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
            addReflectedType(metadata, type, annotation.includeClassHierarchy(), updateReflectEntry);
        }
    }

}
