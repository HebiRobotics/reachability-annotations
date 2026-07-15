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

package us.hebi.graalvm.reachability.annotations;

import java.lang.annotation.*;

/**
 * Creates reachability configuration based on common conventions
 * (e.g. Afterburner.fx or FXMLKit) where resource files have the
 * same name as the lowercased view. For example,
 * <p>
 * - ${Name}View.java
 * - ${name}.fxml
 * - ${name}.css
 * - ${name}*.properties
 * <p>
 * The ${name} can also be applied by setting a custom value. Files
 * that are found in the class output are automatically parsed and
 * add rules for FXML includes, CSS imports, and reflective controllers.
 *
 * @author Florian Enner
 * @since 25 Nov 2025
 */
@Repeatable(ReachableFxView.List.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ReachableFxView {

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @interface List {
        ReachableFxView[] value();
    }

    /**
     * The value specifies the name of the fxml and optional css
     * files. Defaults to the conventional lowercase view name.
     *
     * @return fxml, css, and property file names
     */
    String value() default "";

    /**
     * Specifies the condition, i.e., the class that must be reachable,
     * such that the rules take effect. Object.class removes any condition.
     *
     * @return condition class (defaults to the annotated type)
     */
    Class<?> condition() default void.class;

    /**
     * Specifies the condition class using a type name string. This is useful
     * for cases that are conditioned based on private types. Only takes effect
     * if the class condition is not set.
     *
     * @return name of the conditional class, e.g., package.Class$Nested
     */
    String conditionName() default "";

    /**
     * Reflectively accessed classes automatically add their parent hierarchy. UI
     * classes often need it (e.g. access VBox properties from a subclass), so it's
     * enabled by default.
     */
    boolean includeClassHierarchy() default true;

}
