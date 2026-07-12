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

package us.hebi.graalvm.reachability.processor.util;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static us.hebi.quickbuf.ProtoUtil.*;

/**
 * @author Florian Enner
 * @since 09 Jul 2026
 */
@UtilityClass
public class GlobUtil {

    public static String convertPathToGlob(Path rootDir, Path file) {
        return ensureForwardSlashPath(rootDir.relativize(file));
    }

    private static String ensureForwardSlashPath(Path path) {
        var string = path.toString();
        if (string.contains("\\")) {
            string = string.replace('\\', '/');
        }
        return string;
    }

    private static String ensureForwardSlashDir(Path path) {
        var string = ensureForwardSlashPath(path);
        return string.endsWith("/") ? string : string + "/";
    }

    public static boolean hasWildcards(String glob) {
        return glob.contains("*") | glob.contains("?");
    }

    public static String convertGlobToRegex(String glob) {
        if (glob == null || glob.isEmpty()) {
            return "";
        }

        // If there are no glob wildcards, wrap the entire path in a literal regex block.
        if (!glob.contains("*") && !glob.contains("?")) {
            return "\\Q" + glob + "\\E";
        }

        StringBuilder regex = new StringBuilder();
        int i = 0;
        int len = glob.length();

        while (i < len) {
            char c = glob.charAt(i++);
            switch (c) {
                case '\\':
                    // Handle escape characters
                    checkArgument(i < len, "'\\' is only allowed as an escape character");
                    char next = glob.charAt(i++);
                    regex.append('\\').append(next);
                    break;
                case '*':
                    // Handle double-star recursive wildcard (**) vs single-star (*)
                    if (i < len && glob.charAt(i) == '*') {
                        regex.append(".*");
                        i++; // skip second star
                    } else {
                        regex.append("[^/]*"); // match within package level
                    }
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    // Escape standard regex control meta-characters
                    regex.append('\\').append(c);
                    break;
                default:
                    regex.append(c);
                    break;
            }
        }
        return regex.toString();
    }

    public static void forEachFile(Path searchBaseDir, String glob, Consumer<Path> onFile) throws IOException {
        if (!Files.isDirectory(searchBaseDir)) {
            return; // nothing to do
        }

        if (!GlobUtil.hasWildcards(glob)) {

            // Case 1 -> directly resolve file path
            Path fullPath = searchBaseDir.resolve(glob);
            if (Files.isRegularFile(fullPath)) {
                onFile.accept(fullPath);
            }

        } else {

            // Case 2 -> walk file tree with wildcards
            String absGlob = ensureForwardSlashDir(searchBaseDir) + glob;
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + absGlob);

            try (Stream<Path> stream = Files.walk(searchBaseDir)) {
                stream.filter(Files::isRegularFile)
                        .filter(matcher::matches)
                        .forEach(onFile);
            }

        }

    }

}
