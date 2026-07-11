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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static us.hebi.graalvm.config.sample.ReachableConfigTest.*;

/**
 * @author Florian Enner
 * @since 11 Jul 2026
 */
class InjectionConfigTest {

    private final static String base = "META-INF/native-image/di-generated/us.hebi.graalvm/config-sample/";
    private static String reflectionConfig = readJsonContent(base + "reflect-config.json");

    @Test
    void testConstructorInjection() {
        assertContains(reflectionConfig, """
                {
                   "condition": {
                     "typeReachable": "us.hebi.graalvm.config.sample.InjectionConfig$ConstructorInjection"
                   },
                   "name": "us.hebi.graalvm.config.sample.InjectionConfig$ConstructorInjection",
                   "allDeclaredConstructors": true
                 }
                """);
    }

    @Test
    void testMethodInjection() {
        assertContains(reflectionConfig, """
                {
                   "condition": {
                     "typeReachable": "us.hebi.graalvm.config.sample.InjectionConfig$MethodInjection"
                   },
                   "name": "us.hebi.graalvm.config.sample.InjectionConfig$MethodInjection",
                   "allDeclaredMethods": true
                 }
                """);
    }


    @Test
    void testFieldInjection() {
        // Fields of the injection target class
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.InjectionConfig$FieldInjection"
                  },
                  "name": "us.hebi.graalvm.config.sample.InjectionConfig$FieldInjection",
                  "allDeclaredFields": true
                }
                """);

        // Constructors of the injected types
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.InjectionConfig$FieldInjection"
                  },
                  "name": "java.lang.String",
                  "allDeclaredConstructors": true
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.InjectionConfig$FieldInjection"
                  },
                  "name": "us.hebi.graalvm.config.sample.InjectionConfig",
                  "allDeclaredConstructors": true
                }
                """);
    }


}
