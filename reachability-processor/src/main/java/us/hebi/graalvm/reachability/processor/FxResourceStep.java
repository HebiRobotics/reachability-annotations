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

package us.hebi.graalvm.reachability.processor;

import com.google.common.collect.ImmutableSetMultimap;
import us.hebi.graalvm.reachability.annotations.ReachableFxResources;
import us.hebi.graalvm.reachability.processor.util.GlobUtil;
import us.hebi.graalvm.reachability.processor.util.ProcessorUtil;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
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
        return Set.of(
                ReachableFxResources.class.getCanonicalName(),
                ReachableFxResources.List.class.getCanonicalName()
        );
    }

    @Override
    public void process0(ImmutableSetMultimap<String, Element> elementMap) {
        // Single annotation
        for (Element element : elementMap.get(ReachableFxResources.class.getCanonicalName())) {
            if (element instanceof TypeElement type) {
                var mirror = getAnnotationMirror(type, ReachableFxResources.class);
                if (mirror != null) {
                    var annotation = type.getAnnotation(ReachableFxResources.class);
                    processAnnotation(type, annotation, mirror);
                }
            }
        }

        // Multiple annotations
        for (Element element : elementMap.get(ReachableFxResources.List.class.getCanonicalName())) {
            if (element instanceof TypeElement type) {
                var listMirror = getAnnotationMirror(type, ReachableFxResources.List.class);
                if (listMirror != null) {
                    var listAnnotation = type.getAnnotation(ReachableFxResources.List.class);
                    var mirrors = getAnnotationArrayValue(listMirror, "value");
                    var annotations = listAnnotation.value();
                    for (int i = 0; i < annotations.length; i++) {
                        processAnnotation(type, annotations[i], mirrors.get(i));
                    }
                }
            }
        }
    }

    private void processAnnotation(TypeElement type, ReachableFxResources annotation, AnnotationMirror mirror) {
        final var metadata = getConditionalMetadata(getConditionName(type, mirror));
        var sourceDir = ProcessorUtil.getSourceDirectory(env, type);

        var rootDir = getClassOutputDir();
        for (var glob : annotation.value()) {

            final Path searchBaseDir;
            if (glob.startsWith("/")) {
                glob = glob.substring(1);
                searchBaseDir = rootDir;
                addResourceGlob(metadata, glob);
            } else {
                searchBaseDir = rootDir.resolve(sourceDir);
                addResourceGlob(metadata, sourceDir + glob);
            }

            if (annotation.parseContents()) {
                try {
                    GlobUtil.forEachFile(searchBaseDir, glob, file -> addMetadataFromParsedFileContents(metadata, file, annotation.includeClassHierarchy()));
                } catch (IOException e) {
                    printWarning("Failed to execute glob scanning for resource target: " + glob + " -> " + e.getMessage());
                }
            }

        }

    }

    protected FxResourceStep(Supplier<ProcessingEnvironment> env) {
        super("fxresources-generated", env);
    }

}
