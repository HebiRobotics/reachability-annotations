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

package us.hebi.graalvm.reachability.processor.metadata;

import lombok.*;
import us.hebi.graalvm.reachability.processor.util.GlobUtil;
import us.hebi.graalvm.reachability.processor.util.StringArrayComparator;

import java.util.*;

/**
 * @author Florian Enner
 * @since 09 Jul 2026
 */
@ToString
@EqualsAndHashCode
public class ReachabilityMetadata {

    @Value
    @RequiredArgsConstructor
    public static class Condition {
        String typeReachable;
    }

    @Data
    @RequiredArgsConstructor
    public static class ReflectionEntry {
        final String name;
        public boolean allDeclaredFields = false;
        public boolean allDeclaredMethods = false;
        public boolean allDeclaredConstructors = false;
        public boolean jniAccessible = false;

        public void enableFullReflection() {
            allDeclaredFields = true;
            allDeclaredMethods = true;
            allDeclaredConstructors = true;
        }

    }

    @Data
    @RequiredArgsConstructor
    public static class BundleEntry {
        final String name;
        Set<String> locales = new TreeSet<>();
    }

    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor
    public static class ConditionalMetadata {

        public ReflectionEntry addReflectedType(String typeName,
                                                boolean allDeclaredConstructors,
                                                boolean allDeclaredFields,
                                                boolean allDeclaredMethods,
                                                boolean jni) {
            var entry = reflectedTypes.computeIfAbsent(typeName, ReflectionEntry::new);
            entry.allDeclaredConstructors |= allDeclaredConstructors;
            entry.allDeclaredFields |= allDeclaredFields;
            entry.allDeclaredMethods |= allDeclaredMethods;
            entry.jniAccessible |= jni;
            return entry;
        }

        public ReflectionEntry addReflectedType(String typeName) {
            return reflectedTypes.computeIfAbsent(typeName, ReflectionEntry::new);
        }

        public BundleEntry addBundle(String name) {
            return bundles.computeIfAbsent(name, BundleEntry::new);
        }

        /**
         * v1.0.0 used regex, but the v1.2.0 format uses globs. We want to be forwards
         * compatible with the newer format, so we limit it to blobs from the start.
         */
        public void addResourceGlob(String glob) {
            addResourcePattern(GlobUtil.convertGlobToRegex(glob));
        }

        void addResourcePattern(String pattern) {
            resourcePatterns.add(pattern);
        }

        public void addProxyInterfaces(String... fullyQualifiedNames) {
            proxyInterfaceNames.add(fullyQualifiedNames);
        }

        final Condition condition;
        final Map<String, ReflectionEntry> reflectedTypes = new TreeMap<>();
        final Set<String> resourcePatterns = new TreeSet<>();
        final Set<String[]> proxyInterfaceNames = new TreeSet<>(StringArrayComparator.INSTANCE);
        final Map<String, BundleEntry> bundles = new TreeMap<>();

    }

    public ConditionalMetadata getMetadata(String condition) {
        if (condition == null || condition.isBlank() || condition.equals("java.lang.Object")) {
            condition = "";
        }
        return conditionalMetadata.computeIfAbsent(condition, key -> new ConditionalMetadata(new Condition(key)));
    }

    public Collection<ConditionalMetadata> getAll() {
        return conditionalMetadata.values();
    }

    final Map<String, ConditionalMetadata> conditionalMetadata = new TreeMap<>();

}
