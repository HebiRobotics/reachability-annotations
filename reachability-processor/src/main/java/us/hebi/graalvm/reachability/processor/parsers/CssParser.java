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

import com.google.mu.util.Substring;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
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
        var content = contentOpt.get();

        // Parse import statements
        Substring.between("@import", ";")
                .repeatedly()
                .match(content)
                .map(Substring.Match::toString)
                .map(String::trim)
                .map(CssParser::removeUrlAndQuotes)
                .filter(s -> !s.isEmpty())
                .map(file -> resolve(path, file))
                .forEach(this::addCssFile);

        // Parse other url() resources, e.g., in @font-face rules or background images
        Substring.between("url(", ")")
                .repeatedly()
                .match(content)
                .map(Object::toString)
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
        out.append("Resources:\n");
        for (var v : resources) {
            out.append("  ").append(v).append("\n");
        }
        return out.toString();
    }

    final Set<Path> resources = new TreeSet<>();

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
