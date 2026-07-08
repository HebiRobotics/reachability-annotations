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

package us.hebi.ffi.generator.fxml;

import us.hebi.ffi.generator.fxml.schema.Condition;
import us.hebi.ffi.generator.fxml.schema.ProxyConfig;
import us.hebi.ffi.generator.fxml.schema.ReflectConfig;
import us.hebi.ffi.generator.fxml.schema.ResourceConfig;
import us.hebi.ffi.generator.fxml.schema.ResourceConfig.BundleIdentifier;
import us.hebi.quickbuf.JsonSink;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * @author Florian Enner
 * @since 25 Nov 2025
 */
public class GraalConfigWriter {

    public GraalConfigWriter(String configName) {
        this.name = configName;
    }

    public class ConditionalConfig {

        public void addReflectedType(TypeElement typeElement) {
            if (classes.add(getBinaryName(typeElement))) {
                TypeMirror superclass = typeElement.getSuperclass();
                if (superclass.getKind() != TypeKind.NONE) {
                    TypeElement parent = (TypeElement) env.getTypeUtils().asElement(superclass);
                    if (parent != null && !"java.lang.Object".equals(parent.getQualifiedName().toString())) {
                        addReflectedType(parent);
                    }
                }
            }
        }

        public void addReflectedType(String fullyQualifiedName) {
            if (!classes.contains(fullyQualifiedName)) {
                TypeElement typeElement = env.getElementUtils().getTypeElement(fullyQualifiedName);
                if (typeElement != null) {
                    addReflectedType(typeElement);
                } else {
                    classes.add(fullyQualifiedName);
                }
            }
        }

        public void addJniType(TypeElement typeElement) {
            if (jniClasses.add(getBinaryName(typeElement))) {
                TypeMirror superclass = typeElement.getSuperclass();
                if (superclass.getKind() != TypeKind.NONE) {
                    TypeElement parent = (TypeElement) env.getTypeUtils().asElement(superclass);
                    if (parent != null && !"java.lang.Object".equals(parent.getQualifiedName().toString())) {
                        addJniType(parent);
                    }
                }
            }
        }

        public void addJniType(String fullyQualifiedName) {
            if (!jniClasses.contains(fullyQualifiedName)) {
                TypeElement typeElement = env.getElementUtils().getTypeElement(fullyQualifiedName);
                if (typeElement != null) {
                    addJniType(typeElement);
                } else {
                    jniClasses.add(fullyQualifiedName);
                }
            }
        }

        public void addProxyInterface(String fullyQualifiedName) {
            interfaces.add(fullyQualifiedName);
        }

        public void addAllReflectedTypes(Collection<String> fullyQualifiedNames) {
            fullyQualifiedNames.forEach(this::addReflectedType);
        }

        public void addAllResources(Collection<URI> resources) {
            resources.forEach(this::addResource);
        }

        public void addResource(URI resource) {
            URI relativePath = rootDir != null ? rootDir.relativize(resource) : resource;
            addResourcePattern("\\Q" + relativePath.getPath() + "\\E"); // literal path w/o Regex
        }

        public void addResourcePattern(String pattern) {
            resources.add(pattern);
        }

        public BundleIdentifier getBundleIdentifier(String name) {
            return bundles.computeIfAbsent(name, key -> BundleIdentifier.newInstance().setName(key));
        }

        ConditionalConfig(String binaryName) {
            if (binaryName == null || binaryName.isEmpty()) {
                condition = null;
            } else {
                condition = Condition.newInstance().setTypeReachable(binaryName);
                condition.getSerializedSize(); // pre-serialize
            }
        }

        final Condition condition;
        final Set<String> classes = new TreeSet<>();
        final Set<String> jniClasses = new TreeSet<>();
        final Set<String> resources = new TreeSet<>();
        final Set<String> interfaces = new TreeSet<>();
        final Map<String, BundleIdentifier> bundles = new TreeMap<>();

    }

    private static String getBinaryName(TypeElement typeElement) {
        Element enclosing = typeElement.getEnclosingElement();
        if (enclosing instanceof TypeElement) {
            // Nested class - use $ separator
            return getBinaryName((TypeElement) enclosing) + "$" + typeElement.getSimpleName();
        } else {
            // Top-level class - use qualified name
            return typeElement.getQualifiedName().toString();
        }
    }

    public void setRootDirectory(URI uri) {
        this.rootDir = uri;
    }

    public void setProcessingEnvironment(ProcessingEnvironment env) {
        this.env = env;
    }

    public ConditionalConfig getConfigByType(TypeElement typeElement) {
        return getConfigByName(typeElement == null ? null : getBinaryName(typeElement));
    }

    public ConditionalConfig getConfigByName(String condition) {
        if (condition == null || "java.lang.Object".equals(condition)) {
            condition = "";
        }
        return configs.computeIfAbsent(condition, ConditionalConfig::new);
    }

    /**
     * Writes config rules in graalvm-json format
     */
    public void writeConfig(Filer filer) throws IOException {
        ReflectConfig reflectConfig = ReflectConfig.newInstance();
        ReflectConfig jniConfig = ReflectConfig.newInstance();
        ResourceConfig resourceConfig = ResourceConfig.newInstance();
        ProxyConfig proxyConfig = ProxyConfig.newInstance();

        for (var config : configs.values()) {

            for (String fqdn : config.classes) {

                var rule = reflectConfig.getMutableEntries().next()
                        .setName(fqdn)
                        .setAllDeclaredFields(true)
                        .setAllDeclaredMethods(true)
                        .setAllDeclaredConstructors(true);

                Optional.ofNullable(config.condition)
                        .ifPresent(rule::setCondition);
            }

            for (String fqdn : config.jniClasses) {

                var rule = jniConfig.getMutableEntries().next()
                        .setName(fqdn)
                        .setAllDeclaredFields(true)
                        .setAllDeclaredMethods(true)
                        .setAllDeclaredConstructors(true);

                Optional.ofNullable(config.condition)
                        .ifPresent(rule::setCondition);
            }

            for (var pattern : config.resources) {

                var rule = resourceConfig.getMutableResources()
                        .getMutableIncludes()
                        .next()
                        .setPattern(pattern);

                Optional.ofNullable(config.condition)
                        .ifPresent(rule::setCondition);

            }

            for (var bundle : config.bundles.values()) {
                var rule = resourceConfig.getMutableBundles().next()
                        .copyFrom(bundle);
                Optional.ofNullable(config.condition)
                        .ifPresent(rule::setCondition);
            }

            for (String fqdn : config.interfaces) {

                var rule = proxyConfig.getMutableEntries().next();
                Optional.ofNullable(config.condition)
                        .ifPresent(rule::setCondition);

                if (fqdn.contains(";")) {
                    for (var part : fqdn.split(";")) {
                        rule.getMutableInterfaces().add(part.trim());
                    }
                } else {
                    rule.getMutableInterfaces().add(fqdn);
                }

            }

        }
        if (!reflectConfig.isEmpty()) {
            writeJson(filer, "reflect-config.json", sink -> sink.writeRepeatedMessage(reflectConfig.getEntries()));
        }
        if (!jniConfig.isEmpty()) {
            writeJson(filer, "jni-config.json", sink -> sink.writeRepeatedMessage(jniConfig.getEntries()));
        }
        if (!resourceConfig.isEmpty()) {
            writeJson(filer, "resource-config.json", sink -> sink.writeMessage(resourceConfig));
        }
        if (!proxyConfig.isEmpty()) {
            writeJson(filer, "proxy-config.json", sink -> sink.writeRepeatedMessage(proxyConfig.getEntries()));
        }

    }

    interface SinkWriter {
        void writeTo(JsonSink sink) throws IOException;
    }

    private void writeJson(Filer filer, String fileName, SinkWriter content) throws IOException {
        var sink = JsonSink.newPrettyInstance();
        content.writeTo(sink);

        // Match convention of picocli-codegen
        String artifactId = Optional.ofNullable(env)
                .map(ProcessingEnvironment::getOptions)
                .map(opts -> opts.get("project"))
                .orElse("");
        if (!artifactId.isEmpty()) {
            artifactId += "/";
        }
        filer.createFile("META-INF/native-image/" + artifactId + name, fileName, (ContentSource) writer -> {
            writer.write(sink.toString());
        });
    }

    private final String name;
    private URI rootDir;
    private ProcessingEnvironment env;
    private final Map<String, ConditionalConfig> configs = new TreeMap<>();

}
