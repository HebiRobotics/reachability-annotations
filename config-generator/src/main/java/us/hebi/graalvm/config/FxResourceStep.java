/*-
 * #%L
 * config-generator
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
import us.hebi.graalvm.config.util.GlobUtil;
import us.hebi.graalvm.config.util.ProcessorUtil;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Florian Enner
 * @since 10 Jul 2026
 */
public class FxResourceStep extends AbstractMetadataStep {

    @Override
    public Set<String> annotations() {
        return Set.of(ReachableFxResources.class.getName());
    }

    @Override
    public void process0(ImmutableSetMultimap<String, Element> elementMap) {
        var annotatedElements = elementMap.get(ReachableFxResources.class.getName());
        if (annotatedElements.isEmpty()) {
            return;
        }

        var rootDir = getClassOutputDir();
        for (Element viewClass : annotatedElements) {

            if (viewClass instanceof TypeElement type) {
                ReachableFxResources annotation = type.getAnnotation(ReachableFxResources.class);
                final var metadata = getConditionalMetadata(getConditionName(type, ReachableFxResources.class));
                var typeDir = rootDir.resolve(ProcessorUtil.getSourceDirectory(env, type));

                for (var glob : annotation.value()) {

                    Path searchBaseDir = typeDir;
                    if (glob.startsWith("/")) {
                        glob = glob.substring(1);
                        searchBaseDir = rootDir;
                    }

                    // Always add the raw glob
                    Path absPath = searchBaseDir.resolve(glob);
                    addResourceFile(metadata, absPath);

                    if (annotation.parseContents()) {
                        try {
                            GlobUtil.forEachFile(searchBaseDir, glob, file -> addMetadataFromParsedFileContents(metadata, file));
                        } catch (IOException e) {
                            printWarning("Failed to execute glob scanning for resource target: " + glob + " -> " + e.getMessage());
                        }
                    }

                }
            }
        }
    }

    protected FxResourceStep(Supplier<ProcessingEnvironment> env) {
        super("jfx-generated", env);
    }

}
