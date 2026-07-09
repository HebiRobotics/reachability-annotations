package us.hebi.graalvm.config.metadata;

import us.hebi.graalvm.config.schema.v1_0_0.Condition;
import us.hebi.graalvm.config.schema.v1_0_0.ProxyConfig;
import us.hebi.graalvm.config.schema.v1_0_0.ReflectConfig;
import us.hebi.graalvm.config.schema.v1_0_0.ResourceConfig;
import us.hebi.quickbuf.JsonSource;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static com.google.common.base.Preconditions.*;

/**
 * @author Florian Enner
 * @since 09 Jul 2026
 */
public class MarshallerV100 {

    public static ReachabilityMetadata mergeMetadataFrom(Path sourceDir, ReachabilityMetadata metadata) throws IOException {
        checkNotNull(sourceDir, "source path can't be null");

        // Reflection config
        var json = tryReadFile(sourceDir.resolve("reflect-config.json"));
        if (json.isPresent()) {
            for (var proto : json.get().readRepeatedMessage(ReflectConfig.newInstance().getEntries())) {
                var condition = proto.tryGetCondition().map(Condition::getTypeReachable).orElse(null);
                var reflectedType = metadata.getMetadata(condition).addReflectedType(proto.getName());
                if (proto.getAllDeclaredFields()) reflectedType.setAllDeclaredFields(true);
                if (proto.getAllDeclaredMethods()) reflectedType.setAllDeclaredMethods(true);
                if (proto.getAllDeclaredConstructors()) reflectedType.setAllDeclaredConstructors(true);
            }
        }

        // JNI config (separate file in v1.0.0)
        json = tryReadFile(sourceDir.resolve("jni-config.json"));
        if (json.isPresent()) {
            for (var proto : json.get().readRepeatedMessage(ReflectConfig.newInstance().getEntries())) {
                var condition = proto.tryGetCondition().map(Condition::getTypeReachable).orElse(null);
                var reflectedType = metadata.getMetadata(condition).addReflectedType(proto.getName());
                reflectedType.setJniAccessible(true);
                if (proto.getAllDeclaredFields()) reflectedType.setAllDeclaredFields(true);
                if (proto.getAllDeclaredMethods()) reflectedType.setAllDeclaredMethods(true);
                if (proto.getAllDeclaredConstructors()) reflectedType.setAllDeclaredConstructors(true);
            }
        }

        json = tryReadFile(sourceDir.resolve("resource-config.json"));
        if (json.isPresent()) {
            ResourceConfig resourceConfig = json.get().readMessage(ResourceConfig.newInstance());

            // Resources
            for (var proto : resourceConfig.getResources().getIncludes()) {
                var condition = proto.tryGetCondition().map(Condition::getTypeReachable).orElse(null);
                metadata.getMetadata(condition).addResourcePattern(proto.getPattern());
            }

            // Bundles
            for (var proto : resourceConfig.getBundles()) {
                var condition = proto.tryGetCondition().map(Condition::getTypeReachable).orElse(null);
                var bundle = metadata.getMetadata(condition).addBundle(proto.getName());
                for (String locale : proto.getLocales()) {
                    bundle.locales.add(locale);
                }
            }

        }

        // Proxies
        json = tryReadFile(sourceDir.resolve("proxy-config.json"));
        if (json.isPresent()) {
            for (var proto : json.get().readRepeatedMessage(ProxyConfig.newInstance().getEntries())) {
                var condition = proto.tryGetCondition().map(Condition::getTypeReachable).orElse(null);
                metadata.getMetadata(condition).addProxyInterfaces(String.join(ReachabilityMetadata.PROXY_DELIMITER, proto.getInterfaces()));
            }
        }

        return metadata;
    }

    private static Optional<JsonSource> tryReadFile(Path file) throws IOException {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(JsonSource.newInstance(Files.readAllBytes(file)));
    }

    public static void exportMetadata(ReachabilityMetadata source, URI destDir) {

    }

}
