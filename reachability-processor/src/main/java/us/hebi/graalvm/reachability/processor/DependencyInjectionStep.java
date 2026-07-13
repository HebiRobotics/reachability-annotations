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
import us.hebi.graalvm.reachability.annotations.Reachable.MemberAccess;
import us.hebi.graalvm.reachability.processor.util.ElementUtil;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Florian Enner
 * @since 27 Nov 2025
 */
public class DependencyInjectionStep extends AbstractMetadataStep {

    @Override
    public Set<String> annotations() {
        return Set.of(
                // Standard annotations used for Afterburner
                "jakarta.inject.Inject",
                "jakarta.annotation.PostConstruct",
                "jakarta.annotation.PreDestroy",
                "javax.inject.Inject",
                "javax.annotation.PreDestroy",
                "javax.annotation.PostConstruct",
                "javafx.fxml.FXML"
        );
    }

    public void process0(ImmutableSetMultimap<String, Element> elementMap) {
        // Handle @FXML & DI annotations on nested fields and methods
        for (var element : elementMap.values()) {

            // ignore type annotations
            if (element instanceof TypeElement typeElement) {
                continue;
            }

            // allow all reflection
            if (element.getEnclosingElement() instanceof TypeElement typeElement) {
                var metadata = getConditionalMetadata(ElementUtil.getBinaryName(typeElement));

                // 1) add the containing class
                // TODO: limit to only the annotated fields and methods?
                addReflectedType(metadata, typeElement, false, entry -> {
                    switch (element.getKind()) {
                        case CONSTRUCTOR -> {
                            entry.addMemberAccess(MemberAccess.ALL_DECLARED_CONSTRUCTORS);
                        }
                        case METHOD -> {
                            entry.addMemberAccess(MemberAccess.ALL_DECLARED_METHODS);
                        }
                        case FIELD -> {
                            entry.addMemberAccess(MemberAccess.ALL_DECLARED_FIELDS);
                        }
                    }
                });

                // 2) add the field type in case it needs to be created via reflection
                if (element instanceof VariableElement variable) {
                    var erasedType = env.getTypeUtils().erasure(variable.asType());
                    var mirror = env.getTypeUtils().asElement(erasedType);
                    if (mirror instanceof TypeElement varType) {
                        addReflectedType(metadata, ElementUtil.getBinaryName(varType), false, entry -> {
                            entry.addMemberAccess(MemberAccess.ALL_DECLARED_CONSTRUCTORS);
                        });
                    }
                }

            } else {
                printWarning("Parent is not a TypeElement: " + element);
            }

        }
    }

    public DependencyInjectionStep(Supplier<ProcessingEnvironment> env) {
        super("di", env);
    }

}
