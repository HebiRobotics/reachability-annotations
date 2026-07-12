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

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Florian Enner
 * @since 10 Jul 2026
 */
class FxmlParserTest {

    @Test
    void addFxmlFile() throws URISyntaxException {
        var resource = Path.of(FxmlParserTest.class.getResource("tests/javafx.fxml").toURI());
        var parser = new FxmlParser();
        parser.addFxmlFile(resource);
        assertEquals(3, parser.getResources().size());
        assertEquals(2, parser.getControllers().size());
        assertEquals(4, parser.getImports().size());
    }

}
