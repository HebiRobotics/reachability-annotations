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

package us.hebi.graalvm.config.parsers;

import com.google.mu.util.Substring;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * @author Florian Enner
 * @since 25 Nov 2025
 */
public class FxmlParser {

    public static Optional<String> tryReadContent(Path path) {
        try {
            return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public void addFxmlFile(Path path) {
        // Ignore files that we already looked at (e.g. via includes)
        if (resources.contains(path)) {
            return;
        }

        var contentOpt = tryReadContent(path);
        if (contentOpt.isEmpty()) {
            return; // TODO: warn if a file does not exist?
        }
        var content = contentOpt.get();

        // Remove comments
        resources.add(path);
        content = Substring.spanningInOrder("<!--", "-->")
                .repeatedly()
                .removeAllFrom(content);

        // Look at special statements that figure out all classes that may get reflectively
        // accessed. We keep all fields and methods in the config, so we don't need to parse
        // individual field accesses.
        List<String> localImports = Substring.between("<?import ", "?>")
                .repeatedly()
                .match(content)
                .map(Substring.Match::toString)
                .toList(); // TODO: fail on wildcard imports?
        imports.addAll(localImports);

        // Controllers may not be fully-qualified, so we check for import matches first
        Consumer<String> addController = name -> {
            if (!name.contains(".")) {
                for (String fqname : localImports) {
                    if (fqname.endsWith(name)) {
                        controllers.add(fqname);
                        return;
                    }
                }
            }
            controllers.add(name);
        };

        onAttribute(content, "", "fx:controller", addController);
        onAttribute(content, "fx:root", "type", addController);

        /*
         * handle relative image loading and CSS Files
         *     <Image url="@../images/ProbeDimensionsCustom.png" />
         *     <URL value="@../styles/style.css" />
         */
        Consumer<String> addResourceUrl = url -> {
            // @../path are relative to the file while ../path are relative to the
            // working directory, i.e., outside of the jar.
            if (url.startsWith("@")) {
                resources.add(path.resolveSibling(url.substring(1)));
            }
        };

        onAttribute(content, "Image", "url", addResourceUrl);
        onAttribute(content, "URL", "value", addResourceUrl);

        // Nested files
        onAttribute(content, "fx:include", "source", source -> {
            addFxmlFile(path.resolveSibling(source));
        });

        return;
    }

    public static void onAttribute(String content, String tag, String attribute, Consumer<String> consumer) {
        Substring.between("<" + tag, ">")
                .repeatedly()
                .match(content)
                .forEach(element -> {
                    Substring.between(attribute + "=\"", "\"")
                            .repeatedly()
                            .match(element.toString())
                            .map(Substring.Match::toString)
                            .forEach(consumer::accept);
                });
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("Imports:\n");
        for (var v : imports) {
            out.append("  ").append(v).append("\n");
        }
        out.append("Controllers:\n");
        for (var v : controllers) {
            out.append("  ").append(v).append("\n");
        }
        out.append("Resources:\n");
        for (var v : resources) {
            out.append("  ").append(v).append("\n");
        }
        return out.toString();
    }

    public Set<String> getImports() {
        return imports;
    }

    public Set<String> getControllers() {
        return controllers;
    }

    public Set<Path> getResources() {
        return resources;
    }

    final Set<String> imports = new TreeSet<>();
    final Set<String> controllers = new TreeSet<>();
    final Set<Path> resources = new TreeSet<>();

}
