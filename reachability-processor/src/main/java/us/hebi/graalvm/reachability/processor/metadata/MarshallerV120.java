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

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import us.hebi.graalvm.reachability.annotations.MemberAccess;
import us.hebi.graalvm.reachability.processor.metadata.ReachabilityMetadata.ResourceEntry;
import us.hebi.graalvm.reachability.processor.metadata.schema.v1_0_0.ProxyConfig;
import us.hebi.graalvm.reachability.processor.metadata.schema.v1_0_0.ProxyEntry;
import us.hebi.graalvm.reachability.processor.metadata.schema.v1_2_0.Condition;
import us.hebi.graalvm.reachability.processor.metadata.schema.v1_2_0.ReflectionEntry;
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
public class MarshallerV120 {

    public static ReachabilityMetadata mergeMetadataFrom(Path sourceDir, ReachabilityMetadata metadata) throws IOException {
        checkNotNull(sourceDir, "source path can't be null");

        var rootProto = parseJsonObject(sourceDir.resolve("reachability-metadata.json"),
                us.hebi.graalvm.reachability.processor.metadata.schema.v1_2_0.ReachabilityMetadata.getFactory());

        for (var proto : rootProto.getReflection()) {
            var condition = proto.tryGetCondition().map(Condition::getTypeReached).orElse(null);
            var reflectedType = metadata.getMetadata(condition).addReflectedType(proto.getType());
            copyEntryFromProto(proto, reflectedType);
        }

        for (var proto : rootProto.getResources()) {
            var condition = proto.tryGetCondition().map(Condition::getTypeReached).orElse(null);

            if (proto.hasGlob()) {
                metadata.getMetadata(condition).addBundle(new ResourceEntry(proto.getModule(), proto.getGlob()));
            } else if (proto.hasBundle()) {
                metadata.getMetadata(condition).addBundle(new ResourceEntry(proto.getModule(), proto.getBundle()));
            }
        }

        // Proxies are not implemented in 1.2.0 yet, so we use the old format for now
        for (var proto : parseJsonList(sourceDir.resolve("proxy-config.json"), ProxyEntry.getFactory())) {
            var condition = proto.getCondition().tryGetTypeReachable().orElse(null);
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

    private static void copyEntryFromProto(@MonotonicNonNull ReflectionEntry proto, ReachabilityMetadata.ReflectionEntry entry) {
        if (proto.getAllDeclaredMethods()) entry.addMemberAccess(MemberAccess.ALL_DECLARED_METHODS);
        if (proto.getAllDeclaredFields()) entry.addMemberAccess(MemberAccess.ALL_DECLARED_FIELDS);
        if (proto.getAllDeclaredConstructors()) entry.addMemberAccess(MemberAccess.ALL_DECLARED_CONSTRUCTORS);
        if (proto.getAllPublicMethods()) entry.addMemberAccess(MemberAccess.ALL_PUBLIC_METHODS);
        if (proto.getAllPublicFields()) entry.addMemberAccess(MemberAccess.ALL_PUBLIC_FIELDS);
        if (proto.getAllPublicConstructors()) entry.addMemberAccess(MemberAccess.ALL_PUBLIC_CONSTRUCTORS);
        if (proto.getUnsafeAllocated()) entry.addMemberAccess(MemberAccess.UNSAFE_ALLOCATED);

        if (proto.getJniAccessible()) entry.setJniAccessible(true);
        if (proto.getSerializable()) entry.setSerializable(true);

        for (var method : proto.getMethods()) {
            entry.addMethod(method.getName(), ProtoUtil.toStringArray(method.getParameterTypes()));
        }
        for (var field : proto.getFields()) {
            entry.addField(field.getName());
        }
    }

    private static void copyEntryToProto(ReachabilityMetadata.ReflectionEntry entry, ReflectionEntry proto) {
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

        if (entry.isJniAccessible()) proto.setJniAccessible(true);
        if (entry.isSerializable()) proto.setSerializable(true);

        for (var method : entry.getMethods()) {
            proto.getMutableMethods().next()
                    .setName(method.getName())
                    .addAllParameterTypes(method.getParameterTypes());
        }
        for (var fieldName : entry.getFields()) {
            proto.getMutableFields().next().setName(fieldName);
        }
    }

    public static void saveMetadataTo(ReachabilityMetadata source, Path destDir) throws IOException {
        var proto = us.hebi.graalvm.reachability.processor.metadata.schema.v1_2_0.ReachabilityMetadata.newInstance();
        var proxyConfig = ProxyConfig.newInstance();

        // Merge all conditional data into a single config file
        for (var metadata : source.getAll()) {
            var condition = Optional.of(metadata.condition.getTypeReachable())
                    .filter(Predicate.not(String::isBlank))
                    .map(name -> Condition.newInstance().setTypeReached(name));

            // reflection
            for (var type : metadata.reflectedTypes.values()) {
                var entry = proto.getMutableReflection().next()
                        .setType(type.name);
                copyEntryToProto(type, entry);
                condition.ifPresent(entry::setCondition);
            }

            // glob resources
            for (var glob : metadata.resourceGlobs.values()) {
                var entry = proto.getMutableResources().next()
                        .setGlob(glob.getGlobOrName());
                ProtoUtil.copyNonEmptyString(glob.getModule(), entry::setModule);
                condition.ifPresent(entry::setCondition);
            }

            // bundles
            for (var bundle : metadata.bundles.values()) {
                var entry = proto.getMutableResources().next()
                        .setBundle(bundle.getGlobOrName());
                ProtoUtil.copyNonEmptyString(bundle.getModule(), entry::setModule);
                condition.ifPresent(entry::setCondition);
            }

            // proxy-config stored using the old format for now (TOOD: implement in 1.2.0)
            for (var interfaceNames : metadata.proxyInterfaceNames) {
                var entry = proxyConfig.getMutableEntries().next();
                for (var fqdn : interfaceNames) {
                    entry.getMutableInterfaces().add(fqdn.trim());
                }
                condition.ifPresent(cond -> entry.getMutableCondition().setTypeReachable(cond.getTypeReached()));
            }

        }

        // Save combined metadata to corresponding files
        writeBytes(destDir.resolve("reachability-metadata.json"), ProtoUtil.toJson(proto));
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
