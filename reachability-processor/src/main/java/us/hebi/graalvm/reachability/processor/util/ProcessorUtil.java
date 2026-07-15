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

package us.hebi.graalvm.reachability.processor.util;

import lombok.experimental.UtilityClass;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Florian Enner
 * @since 10 Jul 2026
 */
@UtilityClass
public class ProcessorUtil {

    /**
     * @return path of the output classes, e.g., target/classes/
     */
    public static Path getClassOutputDirectory(ProcessingEnvironment env) {
        try {
            var uri = env.getFiler()
                    .getResource(StandardLocation.CLASS_OUTPUT, "", "dummy")
                    .toUri();
            return Path.of(uri).resolveSibling(".").normalize();
        } catch (IOException e) {
            env.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * @return directory of the type relative to the root, e.g., us/hebi/package/
     */
    public static String getSourceDirectory(ProcessingEnvironment env, TypeElement type) {
        var pkg = env.getElementUtils().getPackageOf(type);
        var directory = pkg
                .getQualifiedName()
                .toString()
                .replace('.', '/');
        return directory.isBlank() ? "" : directory + "/";
    }

    public boolean getBooleanOption(ProcessingEnvironment env, String name, boolean fallback) {
        return Optional.ofNullable(env)
                .map(ProcessingEnvironment::getOptions)
                .map(options -> options.get(name))
                .map(Boolean::parseBoolean)
                .orElse(fallback);
    }

}
