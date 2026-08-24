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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Expected values were generated with mug's Substring
 *
 * @author Florian Enner
 * @since 24 Aug 2026
 */
class SpanUtilTest {

    @Test
    void findBetweenScansForward() {
        assertThat(SpanUtil.findBetween("<a><b>", "<", ">")).containsExactly("a", "b");
        assertThat(SpanUtil.findBetween("<?import A?><?import B?>", "<?import ", "?>")).containsExactly("A", "B");
        assertThat(SpanUtil.findBetween("x=\"1\" y=\"2\"", "=\"", "\"")).containsExactly("1", "2");
        assertThat(SpanUtil.findBetween("url(a) url(b)", "url(", ")")).containsExactly("a", "b");
        assertThat(SpanUtil.findBetween("@import \"a\"; @import \"b\";", "@import", ";")).containsExactly(" \"a\"", " \"b\"");
    }

    @Test
    void findBetweenIgnoresNestedOpenAndConsumesTheClose() {
        assertThat(SpanUtil.findBetween("<a<b>", "<", ">")).containsExactly("a<b");
        assertThat(SpanUtil.findBetween("url(url(a))", "url(", ")")).containsExactly("url(a");
        assertThat(SpanUtil.findBetween("<a>>", "<", ">")).containsExactly("a");
        assertThat(SpanUtil.findBetween("aXbXc", "X", "X")).containsExactly("b");
        assertThat(SpanUtil.findBetween("aXXbXXc", "XX", "XX")).containsExactly("b");
    }

    @Test
    void findBetweenStopsAtAnUnterminatedOpen() {
        assertThat(SpanUtil.findBetween("<a>x<b", "<", ">")).containsExactly("a");
        assertThat(SpanUtil.findBetween("@import \"a\" no semicolon", "@import", ";")).isEmpty();
        assertThat(SpanUtil.findBetween("no delims", "<", ">")).isEmpty();
        assertThat(SpanUtil.findBetween("", "<", ">")).isEmpty();
    }

    @Test
    void findBetweenKeepsEmptyMatches() {
        assertThat(SpanUtil.findBetween("<>", "<", ">")).containsExactly("");
        assertThat(SpanUtil.findBetween("<a><>", "<", ">")).containsExactly("a", "");
    }

    @Test
    void removeSpansRemovesTheDelimitersToo() {
        assertThat(SpanUtil.removeSpans("a<!--c-->b", "<!--", "-->")).isEqualTo("ab");
        assertThat(SpanUtil.removeSpans("a<!--c-->b<!--d-->e", "<!--", "-->")).isEqualTo("abe");
        assertThat(SpanUtil.removeSpans("a<!---->b", "<!--", "-->")).isEqualTo("ab");
        assertThat(SpanUtil.removeSpans("plain", "<!--", "-->")).isEqualTo("plain");
    }

    @Test
    void removeSpansEndsAtTheFirstCloseAndKeepsUnmatchedText() {
        assertThat(SpanUtil.removeSpans("a<!--c<!--d-->b", "<!--", "-->")).isEqualTo("ab");
        assertThat(SpanUtil.removeSpans("a<!--c-->b-->d", "<!--", "-->")).isEqualTo("ab-->d");
        assertThat(SpanUtil.removeSpans("a-->b", "<!--", "-->")).isEqualTo("a-->b");
        assertThat(SpanUtil.removeSpans("a<!--unterminated", "<!--", "-->")).isEqualTo("a<!--unterminated");
        assertThat(SpanUtil.removeSpans("a<!--x-->b<!--unterminated", "<!--", "-->")).isEqualTo("ab<!--unterminated");
    }

    @Test
    void removeSpansDoesNotRescanTheRemainder() {
        assertThat(SpanUtil.removeSpans("<!<!--x-->--><!--y-->", "<!--", "-->")).isEqualTo("<!-->");
    }

}
