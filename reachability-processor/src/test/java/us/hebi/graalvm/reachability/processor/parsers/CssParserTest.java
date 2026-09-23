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

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Florian Enner
 * @since 10 Jul 2026
 */
class CssParserTest {

    @Test
    void addCssFile() throws URISyntaxException {
        var rootDir = Path.of(FxmlParserTest.class.getResource("/").toURI());
        var testDir = Path.of(FxmlParserTest.class.getResource("tests").toURI());
        var parser = new CssParser(rootDir);

        parser.addCssFile(testDir.resolve("javafx.css"));
        assertThat(parser.getResources()).filteredOn(path -> !path.getFileName().toString().contains("@")).containsExactlyInAnyOrder(
                testDir.resolve("javafx.css"),
                testDir.resolve("javafx.bss"),
                testDir.resolve("theme-base.css"),
                testDir.resolve("theme-base.bss"),
                rootDir.resolve("root/path/custom.css"),
                rootDir.resolve("root/path/custom.bss"),
                testDir.resolve("images/plain.png"),
                testDir.resolve("images/wrapped.png"),
                testDir.resolve("images/layer1.png"),
                testDir.resolve("images/layer2.png"),
                rootDir.resolve("root/path/border.png"),
                testDir.resolve("fonts/font.ttf")
        );
        assertThat(parser.getResources()).filteredOn(path -> path.getFileName().toString().startsWith("plain@"))
                .containsExactlyInAnyOrder(
                        testDir.resolve("images/plain@2x.png"),
                        testDir.resolve("images/plain@3x.png"),
                        testDir.resolve("images/plain@4x.png")
                );
        assertThat(parser.getResources()).filteredOn(path -> path.getFileName().toString().startsWith("font")).hasSize(1);
        assertThat(parser.getClasses()).containsExactlyInAnyOrder("com.example.CustomSkin");

    }

}
