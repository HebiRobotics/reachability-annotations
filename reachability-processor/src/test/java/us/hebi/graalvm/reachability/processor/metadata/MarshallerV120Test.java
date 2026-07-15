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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Florian Enner
 * @since 09 Jul 2026
 */
public class MarshallerV120Test {

    @Test
    public void testMergeMetadataFrom() throws Exception {
        var base = MarshallerV120Test.class.getResource("sampleV120").toURI();
        var metadata = MarshallerV120.mergeMetadataFrom(Path.of(base), new ReachabilityMetadata());
    }

    @Test
    public void testSaveMetadataTo() throws Exception {
        var base = MarshallerV120Test.class.getResource("sampleV120").toURI();
        var expected = MarshallerV120.mergeMetadataFrom(Path.of(base), new ReachabilityMetadata());
        MarshallerV120.saveMetadataTo(expected, tempDir);
        var actual = MarshallerV120.mergeMetadataFrom(tempDir, new ReachabilityMetadata());
        assertEquals(expected, actual);
    }

    @Test
    public void testMergeMetadata() throws Exception {
        var base = MarshallerV120Test.class.getResource("sampleV120").toURI();
        var expected = MarshallerV120.mergeMetadataFrom(Path.of(base), new ReachabilityMetadata());

        var merged = new ReachabilityMetadata();
        merged.merge(expected);
        assertEquals(expected, merged);
    }

    @Test
    @Disabled
    public void testCopyV1toV2() throws Exception {
        var metadata = new ReachabilityMetadata();
        MarshallerV100.mergeMetadataFrom(Path.of(MarshallerV100Test.class.getResource("sampleV100").toURI()), metadata);
        MarshallerV120.saveMetadataTo(metadata, Path.of(MarshallerV100Test.class.getResource("sampleV120").toURI()));
    }

    @TempDir
    Path tempDir;

}
