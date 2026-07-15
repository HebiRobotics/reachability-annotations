/*-
 * #%L
 * reachability-sample
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

package us.hebi.graalvm.reachability.sample;

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
public class ReachableConfigTest {

    private static String stepId = "reachable";
    private static String reflectionConfig = readJsonContent(stepId, "reflect-config.json");
    private static String jniConfig = readJsonContent(stepId, "jni-config.json");
    private static String serializationConfig = readJsonContent(stepId, "serialization-config.json");
    private static String resourceConfig = readJsonContent(stepId, "resource-config.json");
    private static String proxyConfig = readJsonContent(stepId, "proxy-config.json");

    @Test
    void testDefaultEnum() throws IOException {
        // Specified enum
        assertContains(reflectionConfig, """
                {
                    "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$ReflectEnum"
                },
                    "name": "us.hebi.graalvm.reachability.sample.ReachableConfig$ReflectEnum",
                        "allDeclaredMethods": true,
                        "allDeclaredFields": true,
                        "allDeclaredConstructors": true
                }
                """);

        // Enum parent class
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$ReflectEnum"
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
                     "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$NestedParentClass"
                   },
                   "name": "us.hebi.graalvm.reachability.sample.ReachableConfig$NestedParentClass",
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
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$NestedChildClass"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.ReachableConfig$NestedChildClass",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);

        // Automatic parent class
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$NestedChildClass"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.ReachableConfig$NestedParentClass",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
    }

    @Test
    void testReferencedChildClass() throws IOException {
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$ReferencingChildClass"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.ReachableConfig$NestedChildClass",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$ReferencingChildClass"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.ReachableConfig$NestedParentClass",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
    }

    @Test
    void testReferencedChildClassWithoutHierarchy() throws IOException {
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$ReferencingChildClassWithoutParent"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.ReachableConfig$NestedChildClass",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
        assertMaxOccurences(reflectionConfig, "$ReferencingChildClassWithoutParent", 1);
    }

    @Test
    void testJniSpecifiedChildClass() throws IOException {
        assertContains(jniConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$ReferencingChildClass"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.ReachableConfig$NestedChildClass",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
        assertContains(jniConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$ReferencingChildClass"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.ReachableConfig$NestedParentClass",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
    }

    @Test
    void testSerializationSpecifiedChildClass() throws IOException {
        assertContains(serializationConfig, """
                {
                   "condition": {
                     "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$ReferencingChildClass"
                   },
                   "name": "us.hebi.graalvm.reachability.sample.ReachableConfig$NestedChildClass"
                 }
                """);
        assertContains(serializationConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$ReferencingChildClass"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.ReachableConfig$NestedParentClass"
                }
                """);

        // Make sure the required fields are also set
        assertContains(serializationConfig, """
                "lambdaCapturingTypes": []
                """);
        assertContains(serializationConfig, """
                "proxies": []
                """);
    }


    @Test
    void testMultipleClasses() throws IOException {
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$ReferencingMultipleClasses"
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
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$ReferencingMultipleClasses"
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
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$ReferencingMultipleClasses"
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
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$ReferencingMultipleClasses"
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
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$ReferencingMultipleClasses"
                  },
                  "name": "some.random.class$Nested",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
    }

    @Test
    void testNoMemberAccess() throws IOException {
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$NoMemberAccess"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.ReachableConfig$NoMemberAccess"
                }
                """);
    }

    @Test
    void testFullMemberAccess() throws IOException {
        assertContains(reflectionConfig, """
                {
                   "condition": {
                     "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$FullMemberAccess"
                   },
                   "name": "us.hebi.graalvm.reachability.sample.ReachableConfig$FullMemberAccess",
                   "allDeclaredMethods": true,
                   "allDeclaredFields": true,
                   "allDeclaredConstructors": true,
                   "allPublicMethods": true,
                   "allPublicFields": true,
                   "allPublicConstructors": true,
                   "unsafeAllocated": true
                 }
                """);
    }

    @Test
    void testPrivateClassHierarchy() throws IOException {
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$PrivateClassHierarchy"
                  },
                  "name": "sun.net.www.protocol.jar.JarURLConnection",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$PrivateClassHierarchy"
                  },
                  "name": "java.net.JarURLConnection",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$PrivateClassHierarchy"
                  },
                  "name": "java.net.URLConnection",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
    }

    @Test
    void testKeepPrivateClass() throws IOException {
        assertContains(reflectionConfig, """
                {
                  "name": "jdk.internal.vm.annotation.Stable",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "sun.misc.Unsafe"
                  },
                  "name": "sun.misc.Unsafe",
                  "allDeclaredFields": true
                }
                """);
    }

    @Test
    void testKeepPublicClass() throws IOException {
        assertContains(reflectionConfig, """
                {
                  "name": "java.lang.String",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$ReferencingMultipleClasses"
                  },
                  "name": "java.net.InetAddress",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
    }

    @Test
    void testKeepPrivateJni() throws IOException {
        assertContains(jniConfig, """
                {
                   "condition": {
                     "typeReachable": "sun.misc.Unsafe"
                   },
                   "name": "sun.misc.Unsafe",
                   "allDeclaredFields": true
                 }
                """);
    }

    @Test
    void testAbsoluteResources() throws IOException {
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddResources"
                  },
                  "pattern": "assets/[^/]*\\\\.jpg"
                }
                """);
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddResources"
                  },
                  "pattern": "assets/[^/]*\\\\.png"
                }
                """);
    }

    @Test
    void testRelativeResources() throws IOException {
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddResources"
                  },
                  "pattern": "us/hebi/graalvm/reachability/sample/images/[^/]*\\\\.jpg"
                }
                """);
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddResources"
                  },
                  "pattern": "us/hebi/graalvm/reachability/sample/images/[^/]*\\\\.png"
                }
                """);
    }

    @Test
    void testBundles() throws IOException {
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddBundle"
                  },
                  "name": "the.bundle.base"
                }
                """);
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddBundle"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.relative.bundle"
                }
                """);
    }

    @Test
    void testProxies() throws IOException {
        assertContains(proxyConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddProxies"
                  },
                  "interfaces": ["name1", "name2"]
                }
                """);
        assertContains(proxyConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddProxies"
                  },
                  "interfaces": ["name3", "name4"]
                }
                """);
    }

    public static void assertMaxOccurences(String json, String searchString, int maxCount) {
        int count = 0;
        int index = 0;

        // Fast index traversal to avoid generating heavy garbage strings
        while ((index = json.indexOf(searchString, index)) != -1) {
            count++;

            // Fail early if we exceed the limit without wasting time scanning the rest of the file
            if (count > maxCount) {
                fail(String.format("Assertion failed: String '%s' occurred more than %d occurrences.", searchString, maxCount));
            }

            index += searchString.length();
        }
    }

    public static void assertContains(String json, String expected) {
        expected = expected.strip();
        if (json.contains(expected)) {
            return;
        }
        String minimizedJson = json.replaceAll("\\s", "");
        String minimizedExpected = expected.replaceAll("\\s", "");
        if (!minimizedJson.contains(minimizedExpected)) {
            fail("The json source does not contain expected content:\n" + expected);
        }
    }

    public static String readJsonContent(String stepId, String fileName) {
        String base = "META-INF/native-image/reachability-generated/us.hebi.graalvm/reachability-sample/";
        String resourcePath = base + stepId + "/" + fileName;
        URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        assertNotNull(resourceUrl, "The processor failed to generate the metadata file at: " + resourcePath);
        try {
            return Files.readString(Paths.get(resourceUrl.toURI()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
