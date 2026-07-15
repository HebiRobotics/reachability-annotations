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