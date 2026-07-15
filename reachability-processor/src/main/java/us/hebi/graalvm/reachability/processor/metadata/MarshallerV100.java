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
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata.ResourceEntry;
import us.hebi.graalvm.reachability.processor.metadata.schema.v1_0_0.*;
import us.hebi.graalvm.reachability.processor.util.GlobUtil;
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
            copyEntryFromProto(proto, reflectedType);
        }

        // JNI config (separate file in v1.0.0)
        for (var proto : parseJsonList(sourceDir.resolve("jni-config.json"), ReflectConfigEntry.getFactory())) {
            var condition = proto.tryGetCondition().map(Condition::getTypeReachable).orElse(null);
            var reflectedType = metadata.getMetadata(condition).addReflectedType(proto.getName());
            reflectedType.setJniAccessible(true);
            copyEntryFromProto(proto, reflectedType);
        }

        // Serialization config
        for (var proto : parseJsonObject(sourceDir.resolve("serialization-config.json"), SerializationConfig.getFactory()).getTypes()) {
            var condition = proto.tryGetCondition().map(Condition::getTypeReachable).orElse(null);
            metadata.getMetadata(condition).addReflectedType(proto.getName()).setSerializable(true);
        }

        // Resources & Bundles
        ResourceConfig resourceConfig = ProtoUtil.parseJsonObject(sourceDir.resolve("resource-config.json"), ResourceConfig.getFactory());
        for (var proto : resourceConfig.getResources().getIncludes()) {
            var condition = proto.tryGetCondition().map(Condition::getTypeReachable).orElse(null);
            var glob = GlobUtil.tryConvertRegexToGlob(proto.getPattern());
            if (glob.isPresent()) {
                metadata.getMetadata(condition).addGlob(ResourceEntry.fromString("", glob.get()));
            } else {
                metadata.getMetadata(condition).patterns.add(proto.getPattern());
            }
        }
        for (var proto : resourceConfig.getBundles()) {
            var condition = proto.tryGetCondition().map(Condition::getTypeReachable).orElse(null);
            metadata.getMetadata(condition).addBundle(ResourceEntry.fromString("", proto.getName()));
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

    private static void copyEntryFromProto(ReflectConfigEntry proto, ReachabilityMetadata.ReflectionEntry entry) {
        if (proto.getAllDeclaredMethods()) entry.addMemberAccess(MemberAccess.ALL_DECLARED_METHODS);
        if (proto.getAllDeclaredFields()) entry.addMemberAccess(MemberAccess.ALL_DECLARED_FIELDS);
        if (proto.getAllDeclaredConstructors()) entry.addMemberAccess(MemberAccess.ALL_DECLARED_CONSTRUCTORS);
        if (proto.getAllPublicMethods()) entry.addMemberAccess(MemberAccess.ALL_PUBLIC_METHODS);
        if (proto.getAllPublicFields()) entry.addMemberAccess(MemberAccess.ALL_PUBLIC_FIELDS);
        if (proto.getAllPublicConstructors()) entry.addMemberAccess(MemberAccess.ALL_PUBLIC_CONSTRUCTORS);
        if (proto.getUnsafeAllocated()) entry.addMemberAccess(MemberAccess.UNSAFE_ALLOCATED);

        for (var method : proto.getMethods()) {
            entry.addMethod(method.getName(), ProtoUtil.toStringArray(method.getParameterTypes()));
        }
        for (ReflectConfigEntry.FieldIdentifier field : proto.getFields()) {
            entry.addField(field.getName());
        }
    }

    private static void copyEntryToProto(ReachabilityMetadata.ReflectionEntry entry, ReflectConfigEntry proto) {
        for (MemberAccess memberAccess : entry.memberAccess) {
            switch (memberAccess) {
                case ALL_DECLARED_METHODS -> proto.setAllDeclaredMethods(true);
                case ALL_DECLARED_FIELDS -> proto.setAllDeclaredFields(true);
                case ALL_DECLARED_CONSTRUCTORS -> proto.setAllDeclaredConstructors(true);
                case ALL_PUBLIC_METHODS -> proto.setAllPublicMethods(true);
                case ALL_PUBLIC_FIELDS -> proto.setAllPublicFields(true);
                case ALL_PUBLIC_CONSTRUCTORS -> proto.setAllPublicConstructors(true);
                case UNSAFE_ALLOCATED -> proto.setUnsafeAllocated(true);
            }
        }

        for (var method : entry.getMethods()) {
            proto.getMutableMethods().next()
                    .setName(method.getName())
                    .addAllParameterTypes(method.getParameterTypes());
        }
        for (var fieldName : entry.getFields()) {
            proto.getMutableFields().next().setName(fieldName);
        }
    }

    public static void mergeExistingAndSaveMetadataTo(ReachabilityMetadata source, Path destDir) throws IOException {
        mergeMetadataFrom(destDir, source);
        saveMetadataTo(source, destDir);
    }

    public static void saveMetadataTo(ReachabilityMetadata source, Path destDir) throws IOException {
        ReflectConfig reflectConfig = ReflectConfig.newInstance();
        ReflectConfig jniConfig = ReflectConfig.newInstance();
        ResourceConfig resourceConfig = ResourceConfig.newInstance();
        ProxyConfig proxyConfig = ProxyConfig.newInstance();
        SerializationConfig serializationConfig = SerializationConfig.newInstance();

        // Merge all conditional data into a single config file
        for (var metadata : source.getAll()) {
            var condition = Optional.of(metadata.condition.getTypeReachable())
                    .filter(Predicate.not(String::isBlank))
                    .map(name -> Condition.newInstance().setTypeReachable(name));

            // reflect-config and jni-config
            for (var type : metadata.reflectedTypes.values()) {

                var entry = reflectConfig.getMutableEntries().next()
                        .setName(type.name);

                copyEntryToProto(type, entry);
                condition.ifPresent(entry::setCondition);

                if (type.jniAccessible) {
                    jniConfig.getMutableEntries().next().copyFrom(entry);
                }

                if (type.isSerializable()) {
                    var serialized = serializationConfig.getMutableTypes().next()
                            .setName(type.name);
                    condition.ifPresent(serialized::setCondition);
                }

            }

            // resource-config (1)
            for (var pattern : metadata.getAsPatterns()) {
                var entry = resourceConfig.getMutableResources()
                        .getMutableIncludes()
                        .next()
                        .setPattern(pattern);
                condition.ifPresent(entry::setCondition);
            }

            // resource-config (2)
            for (var bundle : metadata.bundles.values()) {
                var entry = resourceConfig.getMutableBundles().next()
                        .setName(bundle.getGlobOrName())
                        .clearLocales(); // Note: cleared locales should include everything and match 1.2.0 behavior
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

        // Fill in any required empty lists
        if (!resourceConfig.isEmpty()) {
            resourceConfig.getMutableBundles();
            resourceConfig.getMutableResources();
            resourceConfig.getMutableResources().getMutableIncludes();
        }
        if (!serializationConfig.isEmpty()) {
            serializationConfig.getMutableTypes();
            serializationConfig.getMutableProxies();
            serializationConfig.getMutableLambdaCapturingTypes();
        }

        // Save combined metadata to corresponding files
        writeBytes(destDir.resolve("reflect-config.json"), ProtoUtil.toJson(reflectConfig.getEntries()));
        writeBytes(destDir.resolve("jni-config.json"), ProtoUtil.toJson(jniConfig.getEntries()));
        writeBytes(destDir.resolve("resource-config.json"), ProtoUtil.toJson(resourceConfig));
        writeBytes(destDir.resolve("proxy-config.json"), ProtoUtil.toJson(proxyConfig.getEntries()));
        writeBytes(destDir.resolve("serialization-config.json"), ProtoUtil.toJson(serializationConfig));
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
