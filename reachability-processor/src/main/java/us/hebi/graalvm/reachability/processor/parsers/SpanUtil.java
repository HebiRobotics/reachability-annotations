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

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for parsing strings into non-overlapping and unterminated spans.
 *
 * @author Florian Enner
 * @since 24 Aug 2026
 */
@UtilityClass
class SpanUtil {

    /**
     * @return content between the delimiters, e.g., "a" for "<a>"
     */
    static List<String> findBetween(String content, String open, String close) {
        List<String> matches = new ArrayList<>();
        for (int index = 0; (index = content.indexOf(open, index)) >= 0; ) {
            int begin = index + open.length();
            int end = content.indexOf(close, begin);
            if (end < 0) {
                break;
            }
            matches.add(content.substring(begin, end));
            index = end + close.length();
        }
        return matches;
    }

    /**
     * @return content without the spans, e.g., "ab" for "a<!-- c -->b"
     */
    static String removeSpans(String content, String open, String close) {
        StringBuilder out = null;
        int copied = 0;
        for (int index = copied; (index = content.indexOf(open, index)) >= 0; ) {
            int end = content.indexOf(close, index + open.length());
            if (end < 0) {
                break;
            }
            if (out == null) {
                out = new StringBuilder(content.length());
            }
            out.append(content, copied, index);
            index = copied = end + close.length();
        }
        return out == null ? content : out.append(content, copied, content.length()).toString();
    }

}
