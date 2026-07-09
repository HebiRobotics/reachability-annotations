/*-
 * #%L
 * config-sample
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

package us.hebi.graalvm.config.sample;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Florian Enner
 * @since 09 Jul 2026
 */
class ReflectionConfigTest {

    private String reflectionConfig;

    @BeforeEach
    void loadReflectionConfig() throws Exception {
        String resourcePath = "META-INF/native-image/reachable-generated/us.hebi.graalvm/config-sample/reflect-config.json";
        URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        assertNotNull(resourceUrl, "The processor failed to generate the metadata file at: " + resourcePath);
        reflectionConfig = Files.readString(Paths.get(resourceUrl.toURI()), StandardCharsets.UTF_8);
    }

    @Test
    void testDefaultEnum() throws IOException {
        // Specified enum
        assertContains(reflectionConfig, """
                {
                    "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$ReflectEnum"
                },
                    "name": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$ReflectEnum",
                        "allDeclaredMethods": true,
                        "allDeclaredFields": true,
                        "allDeclaredConstructors": true
                }
                """);

        // Enum parent class
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$ReflectEnum"
                  },
                  "name": "java.lang.Enum",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
    }

    @Test
    void testDefaultReflection() throws IOException {
        assertContains(reflectionConfig, """
                {
                   "condition": {
                     "typeReachable": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$NestedParentClass"
                   },
                   "name": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$NestedParentClass",
                   "allDeclaredMethods": true,
                   "allDeclaredFields": true,
                   "allDeclaredConstructors": true
                 }
                """);
    }

    @Test
    void testDefaultChildClass() throws IOException {
        // Specified child
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$NestedChildClass"
                  },
                  "name": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$NestedChildClass",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);

        // Automatic parent class
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$NestedChildClass"
                  },
                  "name": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$NestedParentClass",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
    }

    @Test
    void testSpecifiedChildClass() throws IOException {
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$ReferencingChildClass"
                  },
                  "name": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$NestedChildClass",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$ReferencingChildClass"
                  },
                  "name": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$NestedParentClass",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
    }

    @Test
    void testMultipleClasses() throws IOException {
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$ReferencingMultipleClasses"
                  },
                  "name": "java.net.InetAddress",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$ReferencingMultipleClasses"
                  },
                  "name": "java.net.InetSocketAddress",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);

        // Looked up parent class
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$ReferencingMultipleClasses"
                  },
                  "name": "java.net.SocketAddress",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);

        // Named list
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$ReferencingMultipleClasses"
                  },
                  "name": "other.random.class",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.ReflectionConfigOptions$ReferencingMultipleClasses"
                  },
                  "name": "some.random.class$Nested",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
    }

    private void assertContains(String json, String expected) {
        expected = expected.strip();
        if (json.contains(expected)) {
            return;
        }
        String minimizedJson = json.replaceAll("\\s", "");
        String minimizedExpected = expected.replaceAll("\\s", "");
        if (!minimizedExpected.contains(minimizedExpected)) {
            fail("The json source does not contain expected content:\n" + expected);
        }
    }


}
