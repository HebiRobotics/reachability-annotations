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

package us.hebi.graalvm.reachability.sample;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static us.hebi.graalvm.reachability.sample.ReachableConfigTest.*;

/**
 * @author Florian Enner
 * @since 11 Jul 2026
 */
public class FxResourcesTest {

    private static String stepId = "fxresources";
    private static String reflectionConfig = readJsonContent(stepId, "reflect-config.json");
    private static String resourceConfig = readJsonContent(stepId,  "resource-config.json");

    @Test
    void absoluteWildcardResources() throws IOException {
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddAbsoluteWildcardResources"
                  },
                  "pattern": "us/hebi/graalvm/reachability/.*\\\\.css"
                }
                """);
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddAbsoluteWildcardResources"
                  },
                  "pattern": "us/hebi/graalvm/reachability/.*\\\\.fxml"
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddAbsoluteWildcardResources"
                  },
                  "name": "javafx.scene.image.ImageView",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
    }

    @Test
    void relativeWildcardResources() throws IOException {
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddRelativeWildcardResources"
                  },
                  "pattern": "us/hebi/graalvm/reachability/sample/.*\\\\.css"
                }
                """);
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddRelativeWildcardResources"
                  },
                  "pattern": "us/hebi/graalvm/reachability/sample/.*\\\\.fxml"
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddRelativeWildcardResources"
                  },
                  "name": "javafx.scene.control.Button",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
    }

    @Test
    void relativeResources() throws IOException {
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddRelativeResources"
                  },
                  "pattern": "\\\\Qus/hebi/graalvm/reachability/sample/javafx/javafx.css\\\\E"
                }
                """);
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddRelativeResources"
                  },
                  "pattern": "\\\\Qus/hebi/graalvm/reachability/sample/javafx/javafx.fxml\\\\E"
                }
                """);
        assertContains(reflectionConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddRelativeResources"
                  },
                  "name": "javafx.scene.control.Button",
                  "allDeclaredMethods": true,
                  "allDeclaredFields": true,
                  "allDeclaredConstructors": true
                }
                """);
    }

    @Test
    void relativeResourcesNonParsing() throws IOException {
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddRelativeResourcesNonParsing"
                  },
                  "pattern": "\\\\Qus/hebi/graalvm/reachability/sample/javafx/javafx.css\\\\E"
                }
                """);
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddRelativeResourcesNonParsing"
                  },
                  "pattern": "\\\\Qus/hebi/graalvm/reachability/sample/javafx/javafx.fxml\\\\E"
                }
                """);
        Assertions.assertFalse(reflectionConfig.contains("us.hebi.graalvm.reachability.sample.ReachableConfig$AddRelativeResourcesNonParsing"));
    }

    @Test
    void nonExistingResources() throws IOException {
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddNonexistingResources"
                  },
                  "pattern": "\\\\Qus/hebi/graalvm/reachability/sample/hello.css\\\\E"
                }
                """);
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$AddNonexistingResources"
                  },
                  "pattern": "\\\\Qus/hebi/graalvm/reachability/sample/hello.fxml\\\\E"
                }
                """);
        Assertions.assertFalse(reflectionConfig.contains("us.hebi.graalvm.reachability.sample.ReachableConfig$AddNonexistingResources"));
    }

    @Test
    void multipleResourceAnnotations() throws IOException {
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$MultipleResourceAnnotations"
                  },
                  "pattern": "\\\\Qus/hebi/graalvm/reachability/sample/hello.css\\\\E"
                }
                """);
        assertContains(resourceConfig, """
                {
                  "condition": {
                    "typeReachable": "us.hebi.graalvm.reachability.sample.ReachableConfig$MultipleResourceAnnotations"
                  },
                  "pattern": "\\\\Qus/hebi/graalvm/reachability/sample/hello.fxml\\\\E"
                }
                """);
        Assertions.assertFalse(reflectionConfig.contains("us.hebi.graalvm.reachability.sample.ReachableConfig$MultipleResourceAnnotations"));
    }

}
