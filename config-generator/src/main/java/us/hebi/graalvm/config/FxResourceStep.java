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
import us.hebi.graalvm.config.metadata.ReachabilityMetadata;
import us.hebi.graalvm.config.metadata.ReachabilityMetadata.ConditionalMetadata;
import us.hebi.graalvm.config.parsers.CssParser;
import us.hebi.graalvm.config.parsers.FxmlParser;
import us.hebi.graalvm.config.util.GlobUtil;
import us.hebi.graalvm.config.util.ProcessorUtil;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

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

                        if (Files.isRegularFile(absPath)) {
                            addParsedFileContents(metadata, absPath);

                        } else if (GlobUtil.hasWildcards(glob) && Files.isDirectory(searchBaseDir)) {

                            String absPattern = absPath.toString().replace('\\', '/');
                            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + absPattern);

                            try (Stream<Path> stream = Files.walk(searchBaseDir)) {
                                stream.filter(Files::isRegularFile)
                                        .filter(matcher::matches)
                                        .forEach(file -> addParsedFileContents(metadata, file));
                            } catch (IOException e) {
                                printWarning("Failed to execute glob scanning for resource target: " + glob + " -> " + e.getMessage());
                            }

                        }
                    }

                }
            }
        }
    }

    private void addParsedFileContents(ConditionalMetadata metadata, Path file) {
        String lowercaseFile = file.getFileName().toString().toLowerCase();

        if (lowercaseFile.endsWith(".fxml")) {
            var fxmlParser = new FxmlParser();
            fxmlParser.addFxmlFile(file);
            for (var name : fxmlParser.getImports()) {
                addReflectedType(metadata, name, ReachabilityMetadata.ReflectionEntry::enableFullReflection);
            }
            for (var name : fxmlParser.getControllers()) {
                addReflectedType(metadata, name, ReachabilityMetadata.ReflectionEntry::enableFullReflection);
            }
            for (var resource : fxmlParser.getResources()) {
                addResourceFile(metadata, resource);
            }
        }

        if (lowercaseFile.endsWith(".css")) {
            var cssParser = new CssParser();
            cssParser.addCssFile(file);
            for (var resource : cssParser.getResources()) {
                addResourceFile(metadata, resource);
            }
        }
    }

    protected FxResourceStep(Supplier<ProcessingEnvironment> env) {
        super("jfx-generated", env);
    }

}
