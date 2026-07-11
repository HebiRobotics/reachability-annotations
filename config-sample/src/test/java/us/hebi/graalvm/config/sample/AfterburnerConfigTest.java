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

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static us.hebi.graalvm.config.sample.ReachableConfigTest.*;

/**
 * @author Florian Enner
 * @since 09 Jul 2026
 */
class AfterburnerConfigTest {

    private final static String base = "META-INF/native-image/afterburner-generated/us.hebi.graalvm/config-sample/";
    private static String reflectionConfig = readJsonContent(base + "reflect-config.json");
    private static String resourceConfig = readJsonContent(base + "resource-config.json");

    @Test
    void testCss() {
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.javafx.JavaFxView"
                  },
                  "pattern": "\\\\Qus/hebi/graalvm/config/sample/javafx/javafx.css\\\\E"
                }
                """);
    }

    @Test
    void testIncludedCss() {
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.javafx.JavaFxView"
                  },
                  "pattern": "\\\\Qus/hebi/graalvm/config/sample/javafx/theme-base.css\\\\E"
                }
                """);
    }

    @Test
    void testPropertyFiles() {
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.javafx.JavaFxView"
                  },
                  "pattern": "us/hebi/graalvm/config/sample/javafx/javafx[^/]*\\\\.properties"
                }
                """);
    }

    @Test
    void testFxmlFile() {
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.javafx.JavaFxView"
                  },
                  "pattern": "\\\\Qus/hebi/graalvm/config/sample/javafx/javafx.fxml\\\\E"
                }
                """);
    }

    @Test
    void testIncludedFxmlFile() {
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.javafx.JavaFxView"
                  },
                  "pattern": "\\\\Qus/hebi/graalvm/config/sample/javafx/included.fxml\\\\E"
                }
                """);
    }

    @Test
    void testViewClass() {
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.javafx.JavaFxView"
                  },
                  "name": "us.hebi.graalvm.config.sample.javafx.JavaFxView",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
    }

    @Test
    void testControllers() {
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.javafx.JavaFxView"
                  },
                  "name": "nonexisting.Controller",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.javafx.JavaFxView"
                  },
                  "name": "us.hebi.graalvm.config.sample.javafx.IncludedFxController",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
    }

    @Test
    void testImports() {
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.javafx.JavaFxView"
                  },
                  "name": "javafx.scene.control.Button",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.javafx.JavaFxView"
                  },
                  "name": "javafx.scene.layout.AnchorPane",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.config.sample.javafx.JavaFxView"
                  },
                  "name": "javafx.scene.image.ImageView",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
    }

}
