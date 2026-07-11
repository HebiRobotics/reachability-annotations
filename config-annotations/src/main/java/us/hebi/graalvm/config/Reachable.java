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
 * Annotation for defining reachability metadata.
 *
 * @author Florian Enner
 * @since 25 Nov 2025
 */
@Repeatable(Reachable.List.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Reachable {

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @interface List {
        Reachable[] value();
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
     * Classes that should be available for reflection. Defaults to the annotated class if
     * no other items (classes, resources, proxies, bundles, ...) are specified.
     *
     * @return classes that should be available for reflection
     */
    Class<?>[] classes() default {};

    /**
     * @return fully qualified names of classes that must be available for reflection, e.g., package.Class$Nested
     */
    String[] classNames() default {};

    /**
     * Specified classes automatically add their parent chain with the same options.
     */
    boolean includeClassHierarchy() default true;

    /**
     * @return whether the reflected classes should also be available through JNI
     */
    boolean jniAccessible() default false;

    /**
     * Register fields which would be returned by the java.lang.Class#getDeclaredFields call
     */
    boolean allDeclaredFields() default true;

    /**
     * Register methods which would be returned by the java.lang.Class#getDeclaredMethods call
     */
    boolean allDeclaredMethods() default true;

    /**
     * Register constructors which would be returned by the java.lang.Class#getDeclaredConstructors call
     */
    boolean allDeclaredConstructors() default true;

    /**
     * Unified routing path using glob syntax (e.g. "assets/**.png" or "META-INF/services/*").
     * Leading '/' are relative to the root, otherwise relative to the annotated type.
     */
    String[] resources() default {};

    /**
     * @return Dynamic proxy interface groups to register under this condition.
     */
    Proxy[] proxies() default {};

    /**
     * @return Resource bundles to register under this condition.
     */
    Bundle[] bundles() default {};

    /**
     * Defines a single dynamic proxy definition implementing a specific combination of interfaces.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({})
    @interface Proxy {
        /**
         * @return The fully qualified names of the interfaces that must be available for proxying.
         */
        String[] value();
    }

    /**
     * Defines a resource bundle configuration mapping specific locales to a bundle base name.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({})
    @interface Bundle {
        /**
         * @return Fully qualified name of the ResourceBundle base class (e.g., "com.example.Messages").
         */
        String name();

        /**
         * @return Locales to register for this specific bundle. Defaults to the default bundle locale.
         */
        String[] locales() default {""};
    }

}
