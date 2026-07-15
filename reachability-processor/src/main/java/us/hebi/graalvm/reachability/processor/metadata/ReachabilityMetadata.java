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

import com.google.common.base.Strings;
import lombok.*;
import us.hebi.graalvm.reachability.annotations.MemberAccess;
import us.hebi.graalvm.reachability.processor.util.GlobUtil;

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

    @Value
    @RequiredArgsConstructor
    public static class ReflectedMethodIdentifier implements Comparable<ReflectedMethodIdentifier> {
        String name;
        String[] parameterTypes;

        @Override
        public int compareTo(ReachabilityMetadata.ReflectedMethodIdentifier other) {
            int cmp = name.compareTo(other.name);
            return cmp != 0 ? cmp : Arrays.compare(parameterTypes, other.parameterTypes);
        }

    }

    @Data
    @RequiredArgsConstructor
    public static class ReflectionEntry {
        final String name;
        final EnumSet<MemberAccess> memberAccess = EnumSet.noneOf(MemberAccess.class);
        final Set<ReflectedMethodIdentifier> methods = new TreeSet<>();
        final Set<String> fields = new TreeSet<>();
        public boolean jniAccessible = false;
        public boolean serializable = false;

        void merge(ReflectionEntry other) {
            memberAccess.addAll(other.memberAccess);
            methods.addAll(other.methods);
            fields.addAll(other.fields);
            jniAccessible |= other.jniAccessible;
            serializable |= other.serializable;
        }

        public void addField(String fieldName) {
            fields.add(fieldName);
        }

        public void addMethod(String methodName, String... argTypes) {
            methods.add(new ReflectedMethodIdentifier(methodName, argTypes));
        }

        public void addConstructor(String... argTypes) {
            methods.add(new ReflectedMethodIdentifier("<init>", argTypes));
        }

        public void addMemberAccess(MemberAccess... flags) {
            for (MemberAccess flag : flags) {
                memberAccess.add(flag);
            }
        }

        public void enableFullReflection() {
            memberAccess.add(MemberAccess.ALL_DECLARED_CONSTRUCTORS);
            memberAccess.add(MemberAccess.ALL_DECLARED_METHODS);
            memberAccess.add(MemberAccess.ALL_DECLARED_FIELDS);
        }

    }

    /**
     * v1.0.0 used regex, but the v1.2.0 format uses globs. We want to be forwards
     * compatible with the newer format, so we limit it to blobs from the start.
     */
    @Value
    @RequiredArgsConstructor
    public static class ResourceEntry {

        public static ResourceEntry fromString(String baseDir, String resource) {
            var module = ReachabilityMetadata.getModulePrefix(resource);
            resource = ReachabilityMetadata.removeModulePrefix(resource);
            boolean isAbs = resource.startsWith("/");
            if (isAbs) resource = resource.substring(1);

            if (module.isPresent()) {
                // Module paths are relative to the target module root
                return new ResourceEntry(module.get(), resource);
            } else if (isAbs || Strings.isNullOrEmpty(baseDir)) {
                // Paths relative to this output directory
                return new ResourceEntry("", resource);
            } else {
                // Paths relative to the
                return new ResourceEntry("", GlobUtil.ensureForwardSlash(baseDir) + resource);
            }
        }

        public ResourceEntry toBundle() {
            if (GlobUtil.hasWildcards(module) || GlobUtil.hasWildcards(globOrName)) {
                throw new IllegalArgumentException("Bundles can't contain wildcards: " + toString());
            }
            if (globOrName.contains("/")) {
                return new ResourceEntry(module, globOrName.replace('/', '.'));
            }
            return this;
        }

        @Override
        public String toString() {
            return Strings.isNullOrEmpty(module) ? globOrName : module + ":" + globOrName;
        }

        String module;
        String globOrName;

    }

    private static void addResourceEntry(String module, String entry, Map<String, ResourceEntry> map) {
        String key = Strings.isNullOrEmpty(module) ? entry : module + ":" + entry;
        if (!map.containsKey(key)) {
            map.put(key, new ResourceEntry(module, entry));
        }
    }

    public static Optional<String> getModulePrefix(String input) {
        int index = input.indexOf(":");
        if (index >= 0) {
            return Optional.of(input.substring(0, index));
        }
        return Optional.empty();
    }

    public static String removeModulePrefix(String input) {
        int index = input.indexOf(":");
        if (index < 0) return input;
        return input.substring(index + 1);
    }

    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor
    public static class ConditionalMetadata {

        public ReflectionEntry addReflectedType(String typeName) {
            return reflectedTypes.computeIfAbsent(typeName, ReflectionEntry::new);
        }

        public void addGlob(ResourceEntry entry) {
            resourceGlobs.putIfAbsent(entry.toString(), entry);
        }

        public void addBundle(ResourceEntry entry) {
            var bundle = entry.toBundle();
            bundles.putIfAbsent(bundle.toString(), bundle);
        }

        public void addProxyInterfaces(String... fullyQualifiedNames) {
            proxyInterfaceNames.add(fullyQualifiedNames);
        }

        Set<String> getAsPatterns() {
            Set<String> patterns = new TreeSet<>(this.patterns);
            for (var entry : resourceGlobs.values()) {
                String pattern = GlobUtil.convertGlobToRegex(entry.globOrName);
                if (!Strings.isNullOrEmpty(entry.module)) {
                    pattern = entry.module + ":" + pattern;
                }
                patterns.add(pattern);
            }
            return patterns;
        }

        final Condition condition;
        final Map<String, ReflectionEntry> reflectedTypes = new TreeMap<>();
        final Map<String, ResourceEntry> resourceGlobs = new TreeMap<>();
        final Map<String, ResourceEntry> bundles = new TreeMap<>();
        final Set<String> patterns = new TreeSet<>(); // internal use for supporting both formats
        final Set<String[]> proxyInterfaceNames = new TreeSet<>(Arrays::compare);

    }

    public void merge(ReachabilityMetadata source) {
        for (ConditionalMetadata other : source.getAll()) {
            var local = getMetadata(other.condition.typeReachable);
            for (var entry : other.reflectedTypes.entrySet()) {
                local.addReflectedType(entry.getKey()).merge(entry.getValue());
            }
            local.resourceGlobs.putAll(other.resourceGlobs);
            local.bundles.putAll(other.bundles);
            local.patterns.addAll(other.patterns);
            local.proxyInterfaceNames.addAll(other.proxyInterfaceNames);
        }
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
