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

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.service.AutoService;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Florian Enner
 * @since 09 Jul 2026
 */
@AutoService(Processor.class)
public class ReachabilityAnnotationProcessor extends BasicAnnotationProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> options = new HashSet<>(super.getSupportedOptions());
        options.add("project");
        options.add("nativeconfig.processInjection");
        return options;
    }

    @Override
    protected Iterable<? extends BasicAnnotationProcessor.Step> steps() {
        return steps;
    }

    protected void postRound(RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            steps.forEach(AbstractMetadataStep::finish);
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
            new FxResourceStep(this::getEnv),
            new FxViewStep(this::getEnv),
            new DependencyInjectionStep(this::getEnv)
    );

}
