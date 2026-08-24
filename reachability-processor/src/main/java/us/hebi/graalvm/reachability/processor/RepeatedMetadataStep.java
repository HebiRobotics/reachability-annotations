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
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata.ConditionalMetadata;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Florian Enner
 * @since 15 Jul 2026
 */
public abstract class RepeatedMetadataStep<A extends Annotation> extends AbstractMetadataStep {

    protected RepeatedMetadataStep(String stepId, Supplier<ProcessingEnvironment> env, Class<A> annotationClass, Class<? extends Annotation> listClass) {
        super(stepId, env);
        this.annotationClass = annotationClass;
        this.annotationListClass = listClass;
    }

    @Override
    public Set<String> annotations() {
        return Set.of(annotationClass.getCanonicalName(), annotationListClass.getCanonicalName());
    }

    @Override
    public void process0(ImmutableSetMultimap<String, Element> elementMap) {
        // Single annotation
        for (Element element : elementMap.get(annotationClass.getCanonicalName())) {
            if (element instanceof TypeElement type) {
                var mirror = getAnnotationMirror(type, annotationClass);
                if (mirror != null) {
                    var annotation = type.getAnnotation(annotationClass);
                    processAnnotation(type, annotation, mirror);
                }
            }
        }

        // Multiple annotations
        for (Element element : elementMap.get(annotationListClass.getCanonicalName())) {
            if (element instanceof TypeElement type) {
                var listMirror = getAnnotationMirror(type, annotationListClass);
                if (listMirror != null) {
                    var listAnnotation = type.getAnnotation(annotationListClass);
                    var mirrors = getAnnotationArrayValue(listMirror, "value");
                    var annotations = getContainerValues(listAnnotation);
                    for (int i = 0; i < annotations.length; i++) {
                        processAnnotation(type, annotations[i], mirrors.get(i));
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private A[] getContainerValues(Annotation listAnnotation) {
        try {
            // Every standard repeatable container annotation defines a value() method
            return (A[]) listAnnotation.getClass().getMethod("value").invoke(listAnnotation);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to extract annotations from container: " + listAnnotation.annotationType(), e
            );
        }
    }

    protected void processAnnotation(TypeElement type, A annotation, AnnotationMirror mirror) {
        processAnnotation(getConditionalMetadata(getConditionName(type, mirror)), type, annotation);
    }

    protected abstract void processAnnotation(ConditionalMetadata metadata, TypeElement type, A annotation);

    private final Class<A> annotationClass;
    private final Class<? extends Annotation> annotationListClass;

}
