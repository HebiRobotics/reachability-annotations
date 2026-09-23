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

import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Images on HiDPI screens probe for scaled siblings like logo@2x.png first (ImageStorage::loadAll)
 *
 * @author Florian Enner
 * @since 23 Sep 2026
 */
class ScaledImages {

    static void addProbedSiblings(Path image, Set<Path> resources) {
        var name = image.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || !IMAGE_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase()) || SCALED_NAME.matcher(name).matches()) {
            return;
        }
        for (int scale = 2; scale <= MAX_SCALE; scale++) {
            resources.add(image.resolveSibling(name.substring(0, dot) + "@" + scale + "x" + name.substring(dot)));
        }
    }

    // Screen scales get rounded, so @4x covers everything below 450%
    private static final int MAX_SCALE = 4;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp");
    private static final Pattern SCALED_NAME = Pattern.compile(".*@[1-9][0-9]?x(?:\\.[^.]+)?"); // ImageTools::hasScaledName

    private ScaledImages() {
    }

}
