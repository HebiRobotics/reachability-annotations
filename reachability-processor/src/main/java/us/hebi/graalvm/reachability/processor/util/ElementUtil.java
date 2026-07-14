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

package us.hebi.graalvm.reachability.processor.util;

import lombok.experimental.UtilityClass;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author Florian Enner
 * @since 10 Jul 2026
 */
@UtilityClass
public class ElementUtil {

    /**
     * @return the fully qualified type name according to GraalVM metadata conventions
     */
    public static String getBinaryName(TypeElement typeElement) {
        Element enclosing = typeElement.getEnclosingElement();
        if (enclosing instanceof TypeElement) {
            // Nested class - use $ separator
            return getBinaryName((TypeElement) enclosing) + "$" + typeElement.getSimpleName();
        } else {
            // Top-level class - use qualified name
            return typeElement.getQualifiedName().toString();
        }
    }

    /**
     * @param binaryNameAction callback called for the current type as well as all parents types up to excluding java.lang.Object
     */
    public static void forEachHierarchicalBinaryName(ProcessingEnvironment env, TypeElement typeElement, Consumer<String> binaryNameAction) {
        binaryNameAction.accept(ElementUtil.getBinaryName(typeElement));
        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE) {
            TypeElement parent = (TypeElement) env.getTypeUtils().asElement(superclass);
            if (parent != null && !"java.lang.Object".equals(parent.getQualifiedName().toString())) {
                forEachHierarchicalBinaryName(env, parent, binaryNameAction);
            }
        }
    }

    public static TypeElement asTypeElement(ProcessingEnvironment env, Class<?> clazz) {
        try {
            return env.getElementUtils().getTypeElement(clazz.getCanonicalName());
        } catch (MirroredTypeException e) {
            for (var typeMirror : e.getTypeMirrors()) {
                Element classElement = env.getTypeUtils().asElement(typeMirror);
                if (classElement instanceof TypeElement typeElement) {
                    return typeElement;
                }
            }
            throw new IllegalStateException("Could not convert " + clazz);
        }
    }

    public static String[] getParameterTypes(ProcessingEnvironment env, Element element) {
        if (element instanceof ExecutableElement executableElement) {
            return executableElement.getParameters().stream()
                    .map(variableElement -> {
                        TypeMirror typeMirror = variableElement.asType();
                        return env.getTypeUtils().erasure(typeMirror).toString();
                    })
                    .toArray(String[]::new);
        }
        throw new IllegalArgumentException("Element %s is not an executable element".formatted(element));
    }

    public static Optional<String> getFieldType(ProcessingEnvironment env, Element element) {
        if (element instanceof VariableElement variable) {
            var erasedType = env.getTypeUtils().erasure(variable.asType());
            var mirror = env.getTypeUtils().asElement(erasedType);
            if (mirror instanceof TypeElement varType) {
                return Optional.of(ElementUtil.getBinaryName(varType));
            }
        }
        return Optional.empty();
    }

}
