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
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Florian Enner
 * @since 27 Nov 2025
 */
public class DependencyInjectionProcessor extends AbstractConfigStep {

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
        for (var entry : elementMap.entries()) {
            var element = entry.getValue();
            if (element instanceof TypeElement typeElement) {
                continue; // ignore type annotations
            }

            if (element.getEnclosingElement() instanceof TypeElement typeElement) {
                var config = configWriter.getConfigByType(typeElement);

                // 1) add the containing class
                config.addReflectedType(typeElement);

                // 2) add the field type in case it needs to be created via reflection
                if (element instanceof VariableElement variable) {
                    Element mirror = processingEnv.getTypeUtils().asElement(variable.asType());
                    if (mirror instanceof TypeElement varType) {
                        config.addReflectedType(varType);
                    }
                }

            } else {
                printWarning("Parent is not a TypeElement: " + element);
            }

        }
    }

    public DependencyInjectionProcessor(Supplier<ProcessingEnvironment> env) {
        super("generated-di", env);
    }

}
