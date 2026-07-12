/*-
 * #%L
 * config-generator
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

import lombok.experimental.UtilityClass;

import java.util.Comparator;

/**
 * @author Florian Enner
 * @since 10 Jul 2026
 */
@UtilityClass
public class StringArrayComparator {

    private static final Comparator<String> STRING_COMPARATOR = Comparator.nullsFirst(Comparator.naturalOrder());

    public static final Comparator<String[]> INSTANCE = Comparator.nullsFirst((a, b) -> {
        // Compare individual strings
        int minLength = Math.min(a.length, b.length);
        for (int i = 0; i < minLength; i++) {
            int cmp = STRING_COMPARATOR.compare(a[i], b[i]);
            if (cmp != 0) return cmp;
        }

        // If the elements are the same, the shorter array goes first
        return Integer.compare(a.length, b.length);
    });

}
