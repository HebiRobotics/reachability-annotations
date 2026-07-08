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
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Florian Enner
 * @since 25 Nov 2025
 */
public class AfterburnerProcessor extends AbstractConfigStep {

    @Override
    public Set<String> annotations() {
        return Set.of(
                NativeAfterburnerView.class.getName()
        );
    }

    public void process0(ImmutableSetMultimap<String, Element> elementMap) {
        var rootUri = getClassOutputUri();

        // Handle Afterburner.fx conventions
        var views = elementMap.get(NativeAfterburnerView.class.getName());
        if (!views.isEmpty()) {
            for (Element viewClass : views) {
                if (viewClass instanceof TypeElement type) {
                    NativeAfterburnerView annotation = type.getAnnotation(NativeAfterburnerView.class);
                    final var config = getConfig(type, NativeAfterburnerView.class);

                    // Use Afterburner convention for the name
                    var sourceDir = getSourceDirectory(type);
                    var conventionalName = Optional.of(annotation.value())
                            .filter(s -> !s.isEmpty())
                            .orElseGet(() -> stripEnding(type.getSimpleName().toString()).toLowerCase());

                    // Add annotated class so Afterburner can figure out the conventional name via reflection
                    config.addReflectedType(type);

                    // Add language files
                    config.addResourcePattern(sourceDir + conventionalName + ".*\\.properties");

                    // Add CSS files and all includes
                    var cssParser = new CssParser();
                    cssParser.addCssFile(rootUri.resolve(sourceDir + conventionalName + ".css"));
                    config.addAllResources(cssParser.getResources());

                    // Add FXML files and all includes
                    var fxmlParser = new FxmlParser();
                    fxmlParser.addFxmlFile(rootUri.resolve(sourceDir + conventionalName + ".fxml"));
                    config.addAllReflectedTypes(fxmlParser.getImports());
                    config.addAllReflectedTypes(fxmlParser.getControllers());
                    config.addAllResources(fxmlParser.getResources());

                    // Sanity check that we don't have wildcards
                    for (String clazz : fxmlParser.getImports()) {
                        if (clazz.endsWith("*")) {
                            printWarning("Ignoring unsupported wildcard import: " + clazz);
                            continue;
                        }
                    }

                }
            }

        }
    }

    public static final String DEFAULT_ENDING = "View";

    protected static String stripEnding(String className) {
        if (!className.endsWith(DEFAULT_ENDING)) {
            return className;
        }
        int viewIndex = className.lastIndexOf(DEFAULT_ENDING);
        return className.substring(0, viewIndex);
    }

    public AfterburnerProcessor(Supplier<ProcessingEnvironment> env) {
        super("generated-fxml", env);
    }

}
