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

package us.hebi.graalvm.config.util;

import lombok.experimental.UtilityClass;

/**
 * @author Florian Enner
 * @since 09 Jul 2026
 */
@UtilityClass
public class GlobUtil {

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

}
