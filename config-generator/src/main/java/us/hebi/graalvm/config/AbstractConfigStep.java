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

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Florian Enner
 * @since 27 Nov 2025
 */
public abstract class AbstractConfigStep implements BasicAnnotationProcessor.Step {

    @Override
    public final Set<? extends Element> process(ImmutableSetMultimap<String, Element> elementMap) {
        this.processingEnv = env.get();
        configWriter.setProcessingEnvironment(processingEnv);
        configWriter.setRootDirectory(getClassOutputUri());
        try {
            if (!elementMap.isEmpty()) {
                process0(elementMap);
            }
        } catch (Exception e) {
            printError(getStackTrace(e));
        }
        return Collections.emptySet();
    }

    protected void printError(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }

    protected void printWarning(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, message);
    }

    public abstract void process0(ImmutableSetMultimap<String, Element> elementMap);

    public void finish() {
        // Write files to disk
        try {
            configWriter.writeConfig((Filer) (directory, fileName) -> env.get().getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", directory + "/" + fileName)
                    .openWriter()
            );
        } catch (IOException e) {
            printError(getStackTrace(e));
        }
    }

    private String getStackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    protected URI getClassOutputUri() {
        if (classOutputUri != null) {
            return classOutputUri;
        }
        try {
            return classOutputUri = processingEnv.getFiler()
                    .getResource(StandardLocation.CLASS_OUTPUT, "", "dummy")
                    .toUri()
                    .resolve(".");
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            throw new RuntimeException(e);
        }
    }


    protected String getSourceDirectory(TypeElement type) {
        var pkg = processingEnv.getElementUtils().getPackageOf(type);
        var directory = pkg
                .getQualifiedName()
                .toString()
                .replace('.', '/');
        return directory.isEmpty() ? directory : directory + "/";
    }

    protected GraalConfigWriter.ConditionalConfig getConfig(TypeElement element, Class<? extends Annotation> annotation) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (!mirror.getAnnotationType().toString().equals(annotation.getName())) {
                continue;
            }
            return getConfig(element, mirror);
        }
        throw new IllegalStateException("No annotation found for " + annotation);
    }

    protected GraalConfigWriter.ConditionalConfig getConfig(TypeElement annotatedType, AnnotationMirror mirror) {
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
                        Element element = processingEnv.getTypeUtils().asElement(typeMirror);
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
        return getConfig(specifiedClass, className, annotatedType);
    }

    protected GraalConfigWriter.ConditionalConfig getConfig(TypeElement specifiedClass, String className, TypeElement annotatedType) {
        // Try to get the class type from the annotation using TypeMirror
        try {
            // This will throw MirroredTypeException, but we catch it to get the TypeMirror
            if (specifiedClass != null && !Objects.equals(specifiedClass, asTypeElement(void.class))) {
                return configWriter.getConfigByType(specifiedClass);
            }
        } catch (MirroredTypeException ignored) {
            // Expected when accessing Class<?> during annotation processing
        }

        if (className != null && !className.isEmpty()) {
            return configWriter.getConfigByName(className);
        }
        return configWriter.getConfigByType(annotatedType);
    }

    protected TypeElement asTypeElement(Class<?> clazz) {
        try {
            return processingEnv.getElementUtils().getTypeElement(clazz.getCanonicalName());
        } catch (MirroredTypeException e) {
            for (var typeMirror : e.getTypeMirrors()) {
                Element classElement = processingEnv.getTypeUtils().asElement(typeMirror);
                if (classElement instanceof TypeElement typeElement) {
                    return typeElement;
                }
            }
            throw new IllegalStateException("Could not convert " + clazz);
        }
    }

    protected AbstractConfigStep(String name, Supplier<ProcessingEnvironment> env) {
        this.env = env;
        configWriter = new GraalConfigWriter(name);
    }

    private URI classOutputUri;
    private final Supplier<ProcessingEnvironment> env;
    protected ProcessingEnvironment processingEnv;
    protected final GraalConfigWriter configWriter;

}
