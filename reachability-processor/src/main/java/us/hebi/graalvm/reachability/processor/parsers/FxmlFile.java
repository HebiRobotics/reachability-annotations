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

import lombok.Getter;
import us.hebi.graalvm.reachability.processor.util.StringUtil;

import java.nio.file.Path;
import java.util.*;

/**
 * Everything found in a single fxml file. The resources are absolute,
 * but the class names remain unresolved to support wildcard imports.
 *
 * @author Florian Enner
 * @since 14 Sep 2026
 */
@Getter
public class FxmlFile {

    FxmlFile(Path path) {
        this.path = path;
    }

    /**
     * Adds a reflectively accessed class without any properties, e.g., the controller
     */
    public void addClass(String className) {
        properties.computeIfAbsent(className, k -> new TreeSet<>());
    }

    public void addProperty(String className, String property) {
        if (StringUtil.isNullOrEmpty(className)) return;
        properties.computeIfAbsent(className, k -> new TreeSet<>()).add(property);
    }

    final Path path;
    final List<String> imports = new ArrayList<>();
    final Map<String, Set<String>> properties = new TreeMap<>(); // reflectively accessed classes and the property names set on each
    final Set<Path> resources = new TreeSet<>(); // referenced files other than fxml

}
