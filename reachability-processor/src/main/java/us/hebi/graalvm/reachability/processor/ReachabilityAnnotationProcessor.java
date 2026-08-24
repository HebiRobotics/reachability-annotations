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

import com.google.auto.service.AutoService;
import us.hebi.graalvm.reachability.processor.metadata.MarshallerV100;
import us.hebi.graalvm.reachability.processor.metadata.MarshallerV120;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata;
import us.hebi.graalvm.reachability.processor.util.ExceptionUtil;
import us.hebi.graalvm.reachability.processor.util.ProcessorUtil;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Florian Enner
 * @since 09 Jul 2026
 */
@AutoService(Processor.class)
public class ReachabilityAnnotationProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> options = new HashSet<>(super.getSupportedOptions());
        options.add("project");
        options.add("reachability.processDependencyInjection");
        options.add("reachability.outputFormat");
        options.add("reachability.mergeSteps");
        return options;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        for (var step : steps) {
            annotations.addAll(step.annotations());
        }
        return annotations;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Same dispatch as auto-common's BasicAnnotationProcessor without the deferral
        if (!roundEnv.processingOver()) {
            for (var step : steps) {
                var elementMap = getAnnotatedElements(step, roundEnv);
                if (!elementMap.isEmpty()) {
                    step.process(elementMap);
                }
            }
        }
        postRound(roundEnv);
        return false;
    }

    private Map<String, Set<Element>> getAnnotatedElements(AbstractMetadataStep step, RoundEnvironment roundEnv) {
        Map<String, Set<Element>> elementMap = new LinkedHashMap<>();
        for (String annotation : step.annotations()) {
            // Annotation is not on the classpath
            TypeElement type = getEnv().getElementUtils().getTypeElement(annotation);
            if (type == null) {
                continue;
            }
            var elements = roundEnv.getElementsAnnotatedWith(type);
            if (!elements.isEmpty()) {
                elementMap.put(annotation, new LinkedHashSet<>(elements));
            }
        }
        return elementMap;
    }

    private void postRound(RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {

            var baseDir = ProcessorUtil.getClassOutputDirectory(getEnv())
                    .resolve("META-INF/native-image/reachability-generated/")
                    .resolve(getEnv().getOptions().getOrDefault("project", ""));

            boolean mergeSteps = ProcessorUtil.getBooleanOption(getEnv(), "reachability.mergeSteps", true);
            ReachabilityMetadata merged = mergeSteps ? new ReachabilityMetadata() : null;
            boolean hasMerged = false;

            ReachabilityMetadata metadata;
            for (var step : steps) {
                if ((metadata = step.getReachabilityMetadata()) == null) {
                    continue;
                }

                if (mergeSteps) {
                    hasMerged = true;
                    merged.merge(metadata);
                } else {
                    saveMetadata(metadata, baseDir.resolve(step.stepId));
                }
            }

            if (hasMerged) {
                saveMetadata(merged, baseDir);
            }
        }
    }

    /**
     * Saves the metadata under the given base path. Before saving, it merges any existing files
     * in case we did a partial compilation and might otherwise lose data.
     */
    private void saveMetadata(ReachabilityMetadata metadata, Path outputDir) {
        try {
            var outputFormat = getEnv().getOptions().getOrDefault("reachability.outputFormat", "");
            switch (outputFormat) {
                case "all":
                    MarshallerV120.mergeExistingAndSaveMetadataTo(metadata, outputDir);
                    MarshallerV100.mergeExistingAndSaveMetadataTo(metadata, outputDir);
                    break;
                case "120":
                case "1.2.0":
                    MarshallerV120.mergeExistingAndSaveMetadataTo(metadata, outputDir);
                    break;
                case "": // unset defaults to 1.0.0
                case "100":
                case "1.0.0":
                    MarshallerV100.mergeExistingAndSaveMetadataTo(metadata, outputDir);
                    break;
                default:
                    getEnv().getMessager().printError("Unknown reachability.outputFormat '" + outputFormat
                            + "'. Expected one of: all, 120, 1.2.0, 100, 1.0.0");
                    break;
            }
        } catch (IOException e) {
            getEnv().getMessager().printError(ExceptionUtil.getStackTrace(e));
        }
    }

    private ProcessingEnvironment getEnv() {
        if (processingEnv == null) {
            throw new IllegalStateException("Processing environment not initialized yet");
        }
        return processingEnv;
    }

    final List<AbstractMetadataStep> steps = Arrays.asList(
            new ReachableStep(this::getEnv),
            new ReachableMemberStep(this::getEnv),
            new FxResourceStep(this::getEnv),
            new FxViewStep(this::getEnv),
            new DependencyInjectionStep(this::getEnv)
    );

}
