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

package us.hebi.graalvm.reachability.processor.metadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Florian Enner
 * @since 09 Jul 2026
 */
public class MarshallerV100Test {

    @Test
    public void testMergeMetadataFrom() throws Exception {
        var base = MarshallerV100Test.class.getResource("sampleV100").toURI();
        var metadata = MarshallerV100.mergeMetadataFrom(Path.of(base), new ReachabilityMetadata());
    }

    @Test
    public void testSaveMetadataTo() throws Exception {
        var base = MarshallerV100Test.class.getResource("sampleV100").toURI();
        var expected = MarshallerV100.mergeMetadataFrom(Path.of(base), new ReachabilityMetadata());
        MarshallerV100.saveMetadataTo(expected, tempDir);
        var actual = MarshallerV100.mergeMetadataFrom(tempDir, new ReachabilityMetadata());
        assertEquals(expected, actual);
    }

    @TempDir
    Path tempDir;

}
