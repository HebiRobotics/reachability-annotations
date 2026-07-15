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
          // TODO: confirm that all these are all reasonable translations?
        checkConvertsTo("/directory/**/*.json", "/directory/.*/[^/]*\\.json");
        checkConvertsTo("directory/literal.txt", "\\Qdirectory/literal.txt\\E");
        checkConvertsTo("directory/**/literal.txt", "directory/.*/literal\\.txt");
        checkConvertsTo("directory/*.txt", "directory/[^/]*\\.txt");
        checkConvertsTo("/**/**/**/*/*.txt", "/.*/.*/.*/[^/]*/[^/]*\\.txt");
    }

    @Test
    void tryConvertRegexToGlob() {
        checkRoundTripReversal("/directory/**/*.json");
        checkRoundTripReversal("directory/literal.txt");
        checkRoundTripReversal("directory/**/literal.txt");
        checkRoundTripReversal("directory/*.txt");
        checkRoundTripReversal("/**/**/**/*/*.txt");
    }

    private void checkConvertsTo(String glob, String expected) {
        assertEquals(expected, GlobUtil.convertGlobToRegex(glob), glob);
    }

    private void checkRoundTripReversal(String glob) {
        String regex =  GlobUtil.convertGlobToRegex(glob);
        var reversed = GlobUtil.tryConvertRegexToGlob(regex);
        assertTrue(reversed.isPresent());
        assertEquals(glob, reversed.get());
    }

}
