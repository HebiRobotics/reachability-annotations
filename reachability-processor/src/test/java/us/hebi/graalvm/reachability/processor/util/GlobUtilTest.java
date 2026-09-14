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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Florian Enner
 * @since 15 Jul 2026
 */
class GlobUtilTest {

      @Test
    void convertGlobToRegex() {
        checkConvertsTo("/directory/**/*.json", "/directory/(?:.*/)?[^/]*\\.json");
        checkConvertsTo("directory/literal.txt", "\\Qdirectory/literal.txt\\E");
        checkConvertsTo("directory/**/literal.txt", "directory/(?:.*/)?literal\\.txt");
        checkConvertsTo("directory/*.txt", "directory/[^/]*\\.txt");
        checkConvertsTo("/**/**/**/*/*.txt", "/(?:.*/)?(?:.*/)?(?:.*/)?[^/]*/[^/]*\\.txt");
        checkConvertsTo("**", ".*");
        checkConvertsTo("directory/**", "directory/.*");
        checkConvertsTo("**/literal.txt", "(?:.*/)?literal\\.txt");
        // non-standalone '**' degrades to a single-level '*' like GraalVM
        checkConvertsTo("directory/**.txt", "directory/[^/]*\\.txt");
        checkConvertsTo("x/**hello/*.json", "x/[^/]*hello/[^/]*\\.json");
        checkConvertsTo("x**/y", "x[^/]*/y");
    }

    @Test
    void globstarMatchesZeroOrMoreDirectories() {
        // reference behavior of the GraalVM glob format, e.g. 'fxml/**/*.fxml' includes 'fxml/overview.fxml'
        checkMatches("fxml/**/*.fxml", "fxml/overview.fxml", true);
        checkMatches("fxml/**/*.fxml", "fxml/blueprints/music-player.fxml", true);
        checkMatches("fxml/**/*.fxml", "fxml/a/b/c.fxml", true);
        checkMatches("fxml/**/*.fxml", "other/overview.fxml", false);
        checkMatches("**/literal.txt", "literal.txt", true);
        checkMatches("**/literal.txt", "a/b/literal.txt", true);
        checkMatches("directory/**", "directory/file.txt", true);
        checkMatches("directory/**", "directory/a/file.txt", true);
        checkMatches("directory/*.txt", "directory/a/file.txt", false);
        // verified against GraalVM: mixed '**' stays within one level
        checkMatches("x/**hello/*.json", "x/hello/f.json", true);
        checkMatches("x/**hello/*.json", "x/deephello/f.json", true);
        checkMatches("x/**hello/*.json", "x/d/hello/f.json", false);
        checkMatches("a**.txt", "ab.txt", true);
        checkMatches("a**.txt", "a.txt", true);
    }

    @Test
    void tryConvertRegexToGlob() {
        checkRoundTripReversal("/directory/**/*.json");
        checkRoundTripReversal("directory/literal.txt");
        checkRoundTripReversal("directory/**/literal.txt");
        checkRoundTripReversal("directory/*.txt");
        checkRoundTripReversal("/**/**/**/*/*.txt");
        checkRoundTripReversal("directory/**");
        checkRoundTripReversal("**/literal.txt");
    }

    private void checkConvertsTo(String glob, String expected) {
        assertEquals(expected, GlobUtil.convertGlobToRegex(glob), glob);
    }

    private void checkMatches(String glob, String path, boolean expected) {
        assertEquals(expected, path.matches(GlobUtil.convertGlobToRegex(glob)), glob + " vs " + path);
    }

    private void checkRoundTripReversal(String glob) {
        String regex =  GlobUtil.convertGlobToRegex(glob);
        var reversed = GlobUtil.tryConvertRegexToGlob(regex);
        assertTrue(reversed.isPresent());
        assertEquals(glob, reversed.get());
    }

}
