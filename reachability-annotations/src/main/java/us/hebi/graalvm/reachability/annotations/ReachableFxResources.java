/*-
 * #%L
 * reachability-annotations
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
 * Creates reachability metadata for JavaFX resources where added FXML and CSS
 * files get parsed automatically and produce a reflection config.
 *
 * @author Florian Enner
 * @since 10 Jul 2026
 */
@Repeatable(ReachableFxResources.List.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ReachableFxResources {

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @interface List {
        ReachableFxResources[] value();
    }

    /**
     * The condition class that must be reached to activate this configuration.
     * If left as void.class, the annotation processor will default to the
     * annotated class itself. Specifying Object.class removes any condition.
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
     * Unified routing path using glob syntax (e.g. "assets/**.png" or "META-INF/services/*")
     * relative to the annotated type. Prefix '/' for absolute paths relative to the classpath root.
     */
    String[] value() default {};

    /**
     * Reflectively accessed classes automatically add their parent hierarchy
     */
    boolean includeClassHierarchy() default false;

    /**
     * Automatically parses known file extensions like .css and .fxml and automatically
     * adds rules for included files and reflectively instantiated classes where necessary.
     * Note that this requires files to be accessible from the current compilation output.
     */
    boolean parseContents() default true;

}
