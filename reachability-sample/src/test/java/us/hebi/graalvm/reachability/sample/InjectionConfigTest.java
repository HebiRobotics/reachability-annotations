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

import static us.hebi.graalvm.reachability.sample.ReachableConfigTest.*;

/**
 * @author Florian Enner
 * @since 11 Jul 2026
 */
class InjectionConfigTest {

    private static String reflectionConfig = readJsonContent("di", "reflect-config.json");

    @Test
    void testConstructorInjection() {
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.InjectionConfig$ConstructorInjection"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.InjectionConfig$ConstructorInjection",
                  "methods": [{
                    "name": "<init>",
                    "parameterTypes": ["java.lang.String"]
                  }]
                }
                """);
    }

    @Test
    void testMethodInjection() {
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.InjectionConfig$MethodInjection"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.InjectionConfig$MethodInjection",
                  "methods": [{
                    "name": "close",
                    "parameterTypes": []
                  }]
                }
                """);
    }

    @Test
    void testFieldInjection() {
        // Fields of the injection target class
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.InjectionConfig$FieldInjection"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.InjectionConfig$FieldInjection",
                  "fields": [{
                    "name": "config"
                  }, {
                    "name": "string"
                  }]
                }
                """);

        // Default constructors for the injected types
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.InjectionConfig$FieldInjection"
                  },
                  "name": "java.lang.String",
                  "methods": [{
                    "name": "<init>",
                    "parameterTypes": []
                  }]
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.InjectionConfig$FieldInjection"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.InjectionConfig",
                  "methods": [{
                    "name": "<init>",
                    "parameterTypes": []
                  }]
                }
                """);
    }

    @Test
    void testInjectionSample() {
        // class config
        assertContains(reflectionConfig, """
                {
                    "condition": {
                        "typeReachable": "us.hebi.graalvm.reachability.sample.InjectionConfig$InjectionSample"
                    },
                    "name": "us.hebi.graalvm.reachability.sample.InjectionConfig$InjectionSample",
                            "methods": [{
                        "name": "<init>",
                                "parameterTypes": ["java.lang.String"]
                    }, {
                        "name": "postConstruct",
                                "parameterTypes": []
                    }, {
                        "name": "preDestroy",
                                "parameterTypes": []
                    }],
                    "fields": [{
                        "name": "injectedField"
                    }]
                }
                """);

        // default constructor
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.InjectionConfig$InjectionSample"
                  },
                  "name": "us.hebi.graalvm.reachability.sample.InjectionConfig$FieldInjection",
                  "methods": [{
                    "name": "<init>",
                    "parameterTypes": []
                  }]
                }
                """);

    }

}
