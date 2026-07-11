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
import us.hebi.graalvm.config.util.ProcessorUtil;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Florian Enner
 * @since 25 Nov 2025
 */
public class AfterburnerStep extends AbstractMetadataStep {

    @Override
    public Set<String> annotations() {
        return Set.of(ReachableAfterburnerView.class.getName());
    }

    public void process0(ImmutableSetMultimap<String, Element> elementMap) {
        var views = elementMap.get(ReachableAfterburnerView.class.getName());
        if (views.isEmpty()) {
            return;
        }

        // Handle Afterburner.fx conventions
        var rootDir = getClassOutputDir();
        for (Element viewClass : views) {
            if (viewClass instanceof TypeElement type) {
                ReachableAfterburnerView annotation = type.getAnnotation(ReachableAfterburnerView.class);
                final var metadata = getConditionalMetadata(getConditionName(type, ReachableAfterburnerView.class));

                // Use Afterburner convention for the name
                var baseDir = ProcessorUtil.getSourceDirectory(env, type);
                var conventionalName = Optional.of(annotation.value())
                        .filter(s -> !s.isEmpty())
                        .orElseGet(() -> stripEnding(type.getSimpleName().toString()).toLowerCase());

                // Add annotated class so Afterburner can figure out the conventional name via reflection
                addReflectedType(metadata, type, false, entry -> {
                    entry.setAllDeclaredConstructors(true);
                });

                // Parse FXML contents
                var fxmlFile = rootDir.resolve(baseDir + conventionalName + ".fxml");
                addAbsFileResource(metadata, fxmlFile);
                if (Files.isRegularFile(fxmlFile)) {
                    addMetadataFromParsedFileContents(metadata, fxmlFile, annotation.includeClassHierarchy());
                }

                // Parse CSS contents
                var cssFile = rootDir.resolve(baseDir + conventionalName + ".css");
                addAbsFileResource(metadata, cssFile);
                if (Files.isRegularFile(cssFile)) {
                    addMetadataFromParsedFileContents(metadata, cssFile, annotation.includeClassHierarchy());
                }

                // Add wildcard for language files (NOTE: use property bundles instead?)
                addResourceGlob(metadata, baseDir + conventionalName + "*.properties");
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

    public AfterburnerStep(Supplier<ProcessingEnvironment> env) {
        super("afterburner-generated", env);
    }

}
