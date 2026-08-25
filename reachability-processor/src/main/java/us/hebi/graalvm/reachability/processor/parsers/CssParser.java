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

package us.hebi.graalvm.reachability.processor.parsers;

import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Florian Enner
 * @since 25 Nov 2025
 */
@RequiredArgsConstructor
public class CssParser {

    public void addCssFile(Path path) {
        if (resources.contains(path)) {
            return;
        }
        resources.add(path);

        var contentOpt = FxmlParser.tryReadContent(path);
        if (contentOpt.isEmpty()) {
            return; // TODO: warn if a file does not exist?
        }

        // Remove comments (TODO: line comments as well, but gets more complex with url:// etc)
        var content = SpanUtil.removeSpans(contentOpt.get(), "/*", "*/");

        // Parse import statements
        SpanUtil.findBetween(content, "@import", ";").stream()
                .map(String::trim)
                .map(CssParser::removeUrlAndQuotes)
                .filter(s -> !s.isEmpty())
                .map(file -> resolve(path, file))
                .forEach(this::addCssFile);

        // Parse skins that get instantiated reflectively, e.g., -fx-skin: "com.example.CustomSkin";
        classes.addAll(findPropertyValues(content, "-fx-skin"));

        // Parse other url() resources, e.g., in @font-face rules or background images
        SpanUtil.findBetween(content, "url(", ")").stream()
                .map(CssParser::removeUrlAndQuotes)
                .filter(s -> !s.isEmpty())
                .map(file -> resolve(path, file))
                .forEach(this::addResource);

    }

    private void addResource(Path path) {
        if (resources.contains(path)) {
            return;
        }
        if (path.endsWith(".css")) {
            addCssFile(path);
        } else {
            resources.add(path);
        }
    }

    /**
     * @return values of a property, e.g., "com.example.CustomSkin" for '-fx-skin: "com.example.CustomSkin";'
     */
    private static List<String> findPropertyValues(String content, String property) {
        List<String> values = new ArrayList<>();
        for (String match : SpanUtil.findBetween(content, property, ";")) {
            // Ignore properties that merely start with the same name, e.g., "-fx-skinny-border"
            var value = match.stripLeading();
            if (!value.startsWith(":")) {
                continue;
            }
            value = removeUrlAndQuotes(value.substring(1));
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private static String removeUrlAndQuotes(String url) {
        // Trim whitespace
        url = url.trim();

        // Remove url() wrapper if present
        if (url.startsWith("url(") && url.endsWith(")")) {
            url = url.substring(4, url.length() - 1).trim();
        }

        // Remove quotes
        if ((url.startsWith("\"") && url.endsWith("\"")) || (url.startsWith("'") && url.endsWith("'"))) {
            url = url.substring(1, url.length() - 1);
        }
        return url.trim();
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("Classes:\n");
        for (var v : classes) {
            out.append("  ").append(v).append("\n");
        }
        out.append("Resources:\n");
        for (var v : resources) {
            out.append("  ").append(v).append("\n");
        }
        return out.toString();
    }

    final Set<String> classes = new TreeSet<>();
    final Set<Path> resources = new TreeSet<>();

    public Set<String> getClasses() {
        return classes;
    }

    public Set<Path> getResources() {
        return resources;
    }

    public Set<Path> getRelativeResources() {
        Set<Path> set = new TreeSet<>();
        for (Path resource : resources) {
            set.add(rootDir.relativize(resource));
        }
        return set;
    }

    private Path resolve(Path origin, String path) {
        return path.startsWith("/") ? rootDir.resolve(path.substring(1)) : origin.resolveSibling(path);
    }

    private final Path rootDir;

}
