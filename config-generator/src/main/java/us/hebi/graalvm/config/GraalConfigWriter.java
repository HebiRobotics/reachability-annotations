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

import us.hebi.graalvm.config.schema.v1_0_0.Condition;
import us.hebi.graalvm.config.schema.v1_0_0.ProxyConfig;
import us.hebi.graalvm.config.schema.v1_0_0.ReflectConfig;
import us.hebi.graalvm.config.schema.v1_0_0.ResourceConfig;
import us.hebi.graalvm.config.schema.v1_0_0.ResourceConfig.BundleIdentifier;
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

        public void addProxyInterfaces(String... fullyQualifiedNames) {
            proxies.add(String.join(PROXY_DELIMITER, fullyQualifiedNames));
        }

        public void addAllReflectedTypes(Collection<String> fullyQualifiedNames) {
            fullyQualifiedNames.forEach(this::addReflectedType);
        }

        public void addAllResources(Collection<URI> resources) {
            resources.forEach(this::addResourceGlob);
        }

        public void addResourceGlob(URI resource) {
            URI relativePath = rootDir != null ? rootDir.relativize(resource) : resource;
            addResourceGlob(relativePath.getPath());
        }

        public void addResourceGlob(String glob) {
            resources.add(convertGlobToRegex(glob));
        }

        /**
         * v1.0.0 used regex, but the v1.2.0 format uses globs. We want to be forwards
         * compatible with the newer format, so we limit it to blobs from the start.
         */
        public static String convertGlobToRegex(String glob) {
            if (glob == null || glob.isEmpty()) {
                return "";
            }

            // If there are no glob wildcards, wrap the entire path in a literal regex block.
            if (!glob.contains("*") && !glob.contains("?")) {
                return "\\Q" + glob + "\\E";
            }

            StringBuilder regex = new StringBuilder();
            int i = 0;
            int len = glob.length();

            while (i < len) {
                char c = glob.charAt(i++);
                switch (c) {
                    case '*':
                        // Handle double-star recursive wildcard (**) vs single-star (*)
                        if (i < len && glob.charAt(i) == '*') {
                            regex.append(".*");
                            i++; // skip second star
                        } else {
                            regex.append("[^/]*"); // match within package level
                        }
                        break;
                    case '?':
                        regex.append(".");
                        break;
                    case '.':
                    case '(':
                    case ')':
                    case '+':
                    case '|':
                    case '^':
                    case '$':
                    case '@':
                    case '%':
                        // Escape standard regex control meta-characters
                        regex.append('\\').append(c);
                        break;
                    default:
                        regex.append(c);
                        break;
                }
            }
            return regex.toString();
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
        final Set<String> proxies = new TreeSet<>();
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

            for (String fqdn : config.proxies) {

                var rule = proxyConfig.getMutableEntries().next();
                Optional.ofNullable(config.condition)
                        .ifPresent(rule::setCondition);

                if (fqdn.contains(PROXY_DELIMITER)) {
                    for (var part : fqdn.split(PROXY_DELIMITER)) {
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

    private static final String PROXY_DELIMITER = ";";

    interface SinkWriter {
        void writeTo(JsonSink sink) throws IOException;
    }

    private void writeJson(Filer filer, String fileName, SinkWriter content) throws IOException {
        var sink = JsonSink.newPrettyInstance();
        content.writeTo(sink);

        // Match convention of picocli-codegen
        String coordinates = Optional.ofNullable(env)
                .map(ProcessingEnvironment::getOptions)
                .map(opts -> opts.get("project"))
                .orElse("");
        filer.createFile("META-INF/native-image/" + name + "/" + coordinates, fileName, (ContentSource) writer -> {
            writer.write(sink.toString());
        });
    }

    private final String name;
    private URI rootDir;
    private ProcessingEnvironment env;
    private final Map<String, ConditionalConfig> configs = new TreeMap<>();

}
