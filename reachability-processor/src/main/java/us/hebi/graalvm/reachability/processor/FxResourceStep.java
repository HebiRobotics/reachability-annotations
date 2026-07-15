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
import us.hebi.graalvm.reachability.annotations.ReachableFxResources;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata.ConditionalMetadata;
import us.hebi.graalvm.reachability.processor.util.GlobUtil;
import us.hebi.graalvm.reachability.processor.util.ProcessorUtil;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Florian Enner
 * @since 10 Jul 2026
 */
public class FxResourceStep extends RepeatedMetadataStep<ReachableFxResources> {

    public FxResourceStep(Supplier<ProcessingEnvironment> env) {
        super("fx-resources", env, ReachableFxResources.class, ReachableFxResources.List.class);
    }

    @Override
    protected void processAnnotation(ConditionalMetadata metadata, TypeElement type, ReachableFxResources annotation) {
        var sourceDir = ProcessorUtil.getSourceDirectory(env, type);
        var searchBaseDir = getClassOutputDir();

        for (var glob : annotation.value()) {
            var entry = ReachabilityMetadata.ResourceEntry.fromString(sourceDir, glob);
            metadata.addGlob(entry);
            glob = entry.getGlobOrName();

            if (annotation.parseContents()) {
                try {
                    GlobUtil.forEachFile(searchBaseDir, glob, file -> addMetadataFromParsedFileContents(metadata, file, annotation.includeClassHierarchy()));
                } catch (IOException e) {
                    printWarning("Failed to execute glob scanning for resource target: " + glob + " -> " + e.getMessage());
                }
            }

        }
    }

}
