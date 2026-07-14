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
import us.hebi.graalvm.reachability.annotations.ReachableFxView;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata.ResourceEntry;
import us.hebi.graalvm.reachability.processor.util.ProcessorUtil;

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
public class FxViewStep extends AbstractMetadataStep {

    @Override
    public Set<String> annotations() {
        return Set.of(ReachableFxView.class.getCanonicalName());
    }

    public void process0(ImmutableSetMultimap<String, Element> elementMap) {
        var views = elementMap.get(ReachableFxView.class.getCanonicalName());
        if (views.isEmpty()) {
            return;
        }

        // Handle Afterburner.fx conventions
        var rootDir = getClassOutputDir();
        for (Element viewClass : views) {
            if (viewClass instanceof TypeElement type) {
                ReachableFxView annotation = type.getAnnotation(ReachableFxView.class);
                final var metadata = getConditionalMetadata(getConditionName(type, ReachableFxView.class));

                // Use Afterburner convention for the name
                var sourceDir = ProcessorUtil.getSourceDirectory(env, type);
                var conventionalName = Optional.of(annotation.value())
                        .filter(s -> !s.isEmpty())
                        .orElseGet(() -> stripEnding(type.getSimpleName().toString()).toLowerCase());

                // Add annotated class so Afterburner can figure out the conventional name via reflection
                addReflectedType(metadata, type, false, ReachabilityMetadata.ReflectionEntry::addConstructor);

                // Parse FXML & CSS contents
                var baseDir = rootDir.resolve(sourceDir);
                addAndTryParseResource(metadata, baseDir.resolve(conventionalName + ".fxml"), annotation.includeClassHierarchy());
                addAndTryParseResource(metadata, baseDir.resolve(conventionalName + ".css"), annotation.includeClassHierarchy());

                // Add language bundles
                metadata.addBundle(ResourceEntry.fromString(sourceDir, conventionalName));

                // TODO: also add resource entry for the langeuage bundles until confirmed working
                metadata.addGlob(ResourceEntry.fromString(sourceDir, conventionalName + "*.properties"));
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

    public FxViewStep(Supplier<ProcessingEnvironment> env) {
        super("fx-view", env);
    }

}
