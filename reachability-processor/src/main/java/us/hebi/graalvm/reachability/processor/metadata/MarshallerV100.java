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

import us.hebi.graalvm.reachability.annotations.MemberAccess;
import us.hebi.graalvm.reachability.processor.metadata.schema.v1_0_0.*;
import us.hebi.graalvm.reachability.processor.util.ProtoUtil;
import us.hebi.quickbuf.JsonSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.*;
import static us.hebi.graalvm.reachability.processor.util.ProtoUtil.*;

/**
 * @author Florian Enner
 * @since 09 Jul 2026
 */
public class MarshallerV100 {

    public static ReachabilityMetadata mergeMetadataFrom(Path sourceDir, ReachabilityMetadata metadata) throws IOException {
        checkNotNull(sourceDir, "source path can't be null");

        // Reflection config
        for (var proto : parseJsonList(sourceDir.resolve("reflect-config.json"), ReflectConfigEntry.getFactory())) {
            var condition = proto.tryGetCondition().map(Condition::getTypeReachable).orElse(null);
            var reflectedType = metadata.getMetadata(condition).addReflectedType(proto.getName());
            copyMemberAccessFromProto(proto, reflectedType);
        }

        // JNI config (separate file in v1.0.0)
        for (var proto : parseJsonList(sourceDir.resolve("jni-config.json"), ReflectConfigEntry.getFactory())) {
            var condition = proto.tryGetCondition().map(Condition::getTypeReachable).orElse(null);
            var reflectedType = metadata.getMetadata(condition).addReflectedType(proto.getName());
            reflectedType.setJniAccessible(true);
            copyMemberAccessFromProto(proto, reflectedType);
        }

        // Resources & Bundles
        ResourceConfig resourceConfig = ProtoUtil.parseJsonObject(sourceDir.resolve("resource-config.json"), ResourceConfig.getFactory());
        for (var proto : resourceConfig.getResources().getIncludes()) {
            var condition = proto.tryGetCondition().map(Condition::getTypeReachable).orElse(null);
            metadata.getMetadata(condition).addResourcePattern(proto.getPattern());
        }
        for (var proto : resourceConfig.getBundles()) {
            var condition = proto.tryGetCondition().map(Condition::getTypeReachable).orElse(null);
            var bundle = metadata.getMetadata(condition).addBundle(proto.getName());
            for (String locale : proto.getLocales()) {
                bundle.locales.add(locale);
            }
        }

        // Proxies
        for (var proto : parseJsonList(sourceDir.resolve("proxy-config.json"), ProxyEntry.getFactory())) {
            var condition = proto.tryGetCondition().map(Condition::getTypeReachable).orElse(null);
            metadata.getMetadata(condition).addProxyInterfaces(toStringArray(proto.getInterfaces()));
        }

        return metadata;
    }

    private static Optional<JsonSource> tryReadFile(Path file) throws IOException {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(JsonSource.newInstance(Files.readAllBytes(file)));
    }

    private static void copyMemberAccessFromProto(ReflectConfigEntry proto, ReachabilityMetadata.ReflectionEntry entry) {
        if (proto.getAllDeclaredClasses()) entry.addMemberAccess(MemberAccess.ALL_DECLARED_CLASSES);
        if (proto.getAllDeclaredMethods()) entry.addMemberAccess(MemberAccess.ALL_DECLARED_METHODS);
        if (proto.getAllDeclaredFields()) entry.addMemberAccess(MemberAccess.ALL_DECLARED_FIELDS);
        if (proto.getAllDeclaredConstructors()) entry.addMemberAccess(MemberAccess.ALL_DECLARED_CONSTRUCTORS);
        if (proto.getAllPublicClasses()) entry.addMemberAccess(MemberAccess.ALL_PUBLIC_CLASSES);
        if (proto.getAllPublicMethods()) entry.addMemberAccess(MemberAccess.ALL_PUBLIC_METHODS);
        if (proto.getAllPublicFields()) entry.addMemberAccess(MemberAccess.ALL_PUBLIC_FIELDS);
        if (proto.getAllPublicConstructors()) entry.addMemberAccess(MemberAccess.ALL_PUBLIC_CONSTRUCTORS);
        if (proto.getAllRecordComponents()) entry.addMemberAccess(MemberAccess.ALL_RECORD_COMPONENTS);
        if (proto.getAllPermittedSubclasses()) entry.addMemberAccess(MemberAccess.ALL_PERMITTED_SUBCLASSES);
        if (proto.getAllNestMembers()) entry.addMemberAccess(MemberAccess.ALL_NEST_MEMBERS);
        if (proto.getAllSigners()) entry.addMemberAccess(MemberAccess.ALL_SIGNERS);
        if (proto.getQueryAllDeclaredMethods()) entry.addMemberAccess(MemberAccess.QUERY_ALL_DECLARED_METHODS);
        if (proto.getQueryAllDeclaredConstructors()) entry.addMemberAccess(MemberAccess.QUERY_ALL_DECLARED_CONSTRUCTORS);
        if (proto.getQueryAllPublicMethods()) entry.addMemberAccess(MemberAccess.QUERY_ALL_PUBLIC_METHODS);
        if (proto.getQueryAllPublicConstructors()) entry.addMemberAccess(MemberAccess.QUERY_ALL_PUBLIC_CONSTRUCTORS);
        if (proto.getUnsafeAllocated()) entry.addMemberAccess(MemberAccess.UNSAFE_ALLOCATED);
    }

    private static void copyMemberAccessToProto(ReachabilityMetadata.ReflectionEntry entry, ReflectConfigEntry proto) {
        for (MemberAccess memberAccess : entry.memberAccess) {
            switch (memberAccess) {
                case ALL_DECLARED_CLASSES -> proto.setAllDeclaredClasses(true);
                case ALL_DECLARED_METHODS -> proto.setAllDeclaredMethods(true);
                case ALL_DECLARED_FIELDS -> proto.setAllDeclaredFields(true);
                case ALL_DECLARED_CONSTRUCTORS -> proto.setAllDeclaredConstructors(true);
                case ALL_PUBLIC_CLASSES -> proto.setAllPublicClasses(true);
                case ALL_PUBLIC_METHODS -> proto.setAllPublicMethods(true);
                case ALL_PUBLIC_FIELDS -> proto.setAllPublicFields(true);
                case ALL_PUBLIC_CONSTRUCTORS -> proto.setAllPublicConstructors(true);
                case ALL_RECORD_COMPONENTS -> proto.setAllRecordComponents(true);
                case ALL_PERMITTED_SUBCLASSES -> proto.setAllPermittedSubclasses(true);
                case ALL_NEST_MEMBERS -> proto.setAllNestMembers(true);
                case ALL_SIGNERS -> proto.setAllSigners(true);
                case QUERY_ALL_DECLARED_METHODS -> proto.setQueryAllDeclaredMethods(true);
                case QUERY_ALL_DECLARED_CONSTRUCTORS -> proto.setQueryAllDeclaredConstructors(true);
                case QUERY_ALL_PUBLIC_METHODS -> proto.setQueryAllPublicMethods(true);
                case QUERY_ALL_PUBLIC_CONSTRUCTORS -> proto.setQueryAllPublicConstructors(true);
                case UNSAFE_ALLOCATED -> proto.setUnsafeAllocated(true);
            }
        }
    }

    public static void saveMetadataTo(ReachabilityMetadata source, Path destDir) throws IOException {
        ReflectConfig reflectConfig = ReflectConfig.newInstance();
        ReflectConfig jniConfig = ReflectConfig.newInstance();
        ResourceConfig resourceConfig = ResourceConfig.newInstance();
        ProxyConfig proxyConfig = ProxyConfig.newInstance();

        // Merge all conditional data into a single config file
        for (var metadata : source.getAll()) {
            var condition = Optional.of(metadata.condition.getTypeReachable())
                    .filter(Predicate.not(String::isBlank))
                    .map(name -> Condition.newInstance().setTypeReachable(name));

            // reflect-config and jni-config
            for (var type : metadata.reflectedTypes.values()) {

                var entry = reflectConfig.getMutableEntries().next()
                        .setName(type.name);

                copyMemberAccessToProto(type, entry);
                condition.ifPresent(entry::setCondition);

                if (type.jniAccessible) {
                    jniConfig.getMutableEntries().next().copyFrom(entry);
                }

            }

            // resource-config (1)
            for (var pattern : metadata.resourcePatterns) {
                var entry = resourceConfig.getMutableResources()
                        .getMutableIncludes()
                        .next()
                        .setPattern(pattern);
                condition.ifPresent(entry::setCondition);
            }

            // resource-config (2)
            for (var bundle : metadata.bundles.values()) {
                var entry = resourceConfig.getMutableBundles().next()
                        .setName(bundle.name)
                        .addAllLocales(bundle.locales.toArray(String[]::new));
                condition.ifPresent(entry::setCondition);
            }

            // proxy-config
            for (var interfaceNames : metadata.proxyInterfaceNames) {
                var entry = proxyConfig.getMutableEntries().next();
                for (var fqdn : interfaceNames) {
                    entry.getMutableInterfaces().add(fqdn.trim());
                }
                condition.ifPresent(entry::setCondition);
            }

        }

        // Save combined metadata to corresponding files
        writeBytes(destDir.resolve("reflect-config.json"), ProtoUtil.toJson(reflectConfig.getEntries()));
        writeBytes(destDir.resolve("jni-config.json"), ProtoUtil.toJson(jniConfig.getEntries()));
        writeBytes(destDir.resolve("resource-config.json"), ProtoUtil.toJson(resourceConfig));
        writeBytes(destDir.resolve("proxy-config.json"), ProtoUtil.toJson(proxyConfig.getEntries()));
    }

    private static void deleteExistingFile(Path target) throws IOException {
        if (Files.isRegularFile(target)) {
            Files.delete(target);
        }
    }

    private static void createParentDirectories(Path target) throws IOException {
        var parentDir = target.getParent();
        if (parentDir != null && !Files.isDirectory(parentDir)) {
            Files.createDirectories(parentDir);
        }
    }

    private static void writeBytes(Path target, byte[] data) throws IOException {
        deleteExistingFile(target);
        if (data.length > 2) { // min empty size "[]" or "{}"
            createParentDirectories(target);
            Files.write(target, data,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

}
