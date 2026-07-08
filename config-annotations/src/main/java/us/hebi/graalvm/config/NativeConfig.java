/*-
 * #%L
 * Native Config Generator
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

package us.hebi.graalvm.config;

import java.lang.annotation.*;

/**
 * @author Florian Enner
 * @since 25 Nov 2025
 */
@Repeatable(NativeConfig.List.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface NativeConfig {

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @interface List {
        NativeConfig[] value();
    }

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
     * @return classes that should be available for reflection
     */
    Class<?>[] classes() default {};

    /**
     * @return fully qualified names of classes that must be available for reflection, e.g., package.Class$Nested
     */
    String[] classNames() default {};

    /**
     * @return classes that should be registered for access through jni
     */
    Class<?>[] jniClasses() default {};

    /**
     * @return fully qualified names of classes that should be registered for access through jni, e.g., package.Class$Nested
     */
    String[] jniClassNames() default {};

    /**
     * @return fully qualified names of interfaces that must be available for proxying. Separated by ';' if more than 1
     */
    String[] proxyInterfaceNames() default {};

    /**
     * @return absolute path regex patterns (don't add '/' to specify root)
     */
    String[] resources() default {};

    /**
     * @return regex resource patterns relative to the annotated type (${typePath}/${pattern})
     */
    String[] relativeResources() default {};

    /**
     * @return fully qualified names of ResourceBundle classes to include
     */
    String[] bundleNames() default {};

    /**
     * @return locales for all bundles. Defaults to default bundle.
     */
    String[] bundleLocales() default {""};


}
